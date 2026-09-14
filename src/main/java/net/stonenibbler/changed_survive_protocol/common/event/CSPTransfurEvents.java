package net.stonenibbler.changed_survive_protocol.common.event;

import net.ltxprogrammer.changed.entity.TransfurCause;
import net.ltxprogrammer.changed.entity.TransfurContext;
import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.entity.ai.AssimilationBehavior;
import net.ltxprogrammer.changed.entity.ai.ImmediateTransfurDecision;
import net.ltxprogrammer.changed.entity.ai.LatexAssimilationDecision;
import net.ltxprogrammer.changed.entity.ai.NonLatexAssimilationDecision;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.ltxprogrammer.changed.process.TransfurEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.stonenibbler.changed_survive_protocol.ChangedSurviveProtocol;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.data.CSPCapabilities;
import net.stonenibbler.changed_survive_protocol.common.data.CSPPlayerData;
import net.stonenibbler.changed_survive_protocol.common.network.CSPNetwork;
import net.stonenibbler.changed_survive_protocol.common.util.CSPTransfurState;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class CSPTransfurEvents {
    private static final Set<UUID> ALLOWED_IMMEDIATE_TRANSFURS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> COMMAND_UNTRANSFUR_RESETS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final ConcurrentHashMap<UUID, Long> BLOCKED_PROGRESS_COMPLETIONS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, PendingTotemForm> PENDING_TOTEM_FORMS = new ConcurrentHashMap<>();
    private static final ThreadLocal<ProgressExposureContext> ACTIVE_PROGRESS_CONTEXT = new ThreadLocal<>();
    private static final ResourceLocation CHANGED_ADDON_TRANSFUR_TOTEM = ResourceLocation.fromNamespaceAndPath("changed_addon", "transfur_totem");
    private static final ResourceLocation LEGACY_PURO_TOTEM_FORM = ResourceLocation.fromNamespaceAndPath("changed_addon", "form_puro_kind/female");
    private static final ResourceLocation CURRENT_PURO_TOTEM_FORM = ResourceLocation.fromNamespaceAndPath("changed_addon", "form_latex_puro_kind/female");
    private static final String TRANSFUR_TOTEM_FORM_TAG = "form";

    private CSPTransfurEvents() {
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PENDING_TOTEM_FORMS.remove(player.getUUID());
        ItemStack stack = event.getItemStack();
        if (player.isShiftKeyDown()
                || ProcessTransfur.isPlayerTransfurred(player)
                || player.getCooldowns().isOnCooldown(stack.getItem())
                || !CHANGED_ADDON_TRANSFUR_TOTEM.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) {
            return;
        }

        var tag = stack.getTag();
        ResourceLocation formId = tag == null ? null : ResourceLocation.tryParse(tag.getString(TRANSFUR_TOTEM_FORM_TAG));
        if (formId == null) {
            return;
        }
        if (LEGACY_PURO_TOTEM_FORM.equals(formId)) {
            formId = CURRENT_PURO_TOTEM_FORM;
        }
        if (ChangedRegistry.TRANSFUR_VARIANT.get().getValue(formId) == null) {
            return;
        }

        PENDING_TOTEM_FORMS.put(player.getUUID(), new PendingTotemForm(formId, player.level().getGameTime()));
    }

    public static void onEntityVariantAssigned(ProcessTransfur.EntityVariantAssigned event) {
        if (!(event.livingEntity instanceof ServerPlayer player)) {
            return;
        }

        if (hasCommandUntransfurReset(player)) {
            tryCompleteCommandUntransfurReset(player, "variant assignment after command untransfur");
        } else if (restoreSettledVariantAssignment(player, event)) {
            return;
        }

        if (event.variant != null) {
            if (consumeBlockedProgressCompletion(player)) {
                event.variant = null;
            }
            return;
        }

        if (player.isDeadOrDying()) {
            return;
        }

        var currentVariant = ProcessTransfur.getPlayerTransfurVariant(player);
        if (currentVariant != null && currentVariant.isTemporaryFromSuit()) {
            return;
        }

        resetAfterUntransfur(player);
    }

    public static void onImmediateTransfurDecision(TransfurEvents.ImmediateTransfurDecisionEvent event) {
        if (!(event.entity instanceof ServerPlayer player)
                || event.getOriginalDecision().initialKeepConscious()
                || isImmediateTransfurAllowed(player)
                || !canRedirectTransfur(player)) {
            return;
        }

        addExposure(player, event.getTransfurVariant().getFormId().toString(), CSPConfig.COMMON.immediateHazardCoverage.get());
        event.setCanceled(true);
    }

    public static void onKeepConscious(ProcessTransfur.KeepConsciousEvent event) {
        if (event.player instanceof ServerPlayer player && hasBlockedProgressCompletion(player)) {
            event.shouldKeepConscious = true;
        }
    }

    public static void onUntransfurPlayer(TransfurEvents.UntransfurPlayerEvent event) {
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (event instanceof TransfurEvents.UntransfurPlayerByCommandEvent) {
            COMMAND_UNTRANSFUR_RESETS.add(player.getUUID());
            tryCompleteCommandUntransfurReset(player, "command untransfur");
            return;
        }

        try {
            CSPCapabilities.get(player).ifPresent(data -> {
                if (!data.hasSettledStrain()) {
                    return;
                }

                TransfurVariant<?> nextVariant = resolveVariant(data.getSettledStrainId());
                ensureSettledState(player, data);
                event.setNextVariant(nextVariant);
                event.setCanceled(true);
                syncSafely(player, data, "settled untransfur protection");
            });
        } catch (RuntimeException exception) {
            ChangedSurviveProtocol.LOGGER.warn("Failed to handle CSP untransfur protection for {}; leaving event unchanged",
                    player.getScoreboardName(), exception);
        }
    }

    public static void onChangedVariant(ProcessTransfur.EntityVariantAssigned.ChangedVariant event) {
        if (!(event.livingEntity instanceof ServerPlayer player)) {
            return;
        }

        try {
            CSPCapabilities.get(player).ifPresent(data -> {
                if (event.newVariant != null) {
                    data.setActiveStrainId(event.newVariant.getFormId().toString()); // Totems will just override everything, lol.
                    if (consumePendingTotemForm(player, event.newVariant)) {
                        data.setTotemFormId(event.newVariant.getFormId().toString());
                    } else if (data.hasTotemForm() && !data.getTotemFormId().equals(event.newVariant.getFormId().toString())) {
                        data.clearTotemForm();
                    }
                }

                if (event.context != null && event.context.cause() == TransfurCause.STASIS_CHAMBER) {
                    data.setStabilizedLatex(true);
                    data.setUnstableLatex(false);
                    data.setUnstableLatexTicks(0);
                    data.setLucidityDrainMultiplier(1.0D);
                    data.setLucidity(100.0D);
                    data.setCoverage(0.0D);
                    data.setInfected(false);
                    data.setLucidityActive(false);
                } else {
                    activateLatexNeeds(player, data);
                }
                syncSafely(player, data, "variant change");
            });
        } catch (RuntimeException exception) {
            ChangedSurviveProtocol.LOGGER.warn("Failed to update CSP data for {} after variant change",
                    player.getScoreboardName(), exception);
        }
    }

    public static void forceUncontrolledTransfur(ServerPlayer player, CSPPlayerData data) {
        var currentVariant = ProcessTransfur.getPlayerTransfurVariant(player);
        if (currentVariant != null) {
            if (!currentVariant.isTemporaryFromSuit()) {
                activateLatexNeeds(player, data);
                return;
            }
            ProcessTransfur.removePlayerTransfurVariant(player);
        }

        TransfurVariant<?> variant = resolveVariant(data.getActiveStrainId());
        ALLOWED_IMMEDIATE_TRANSFURS.add(player.getUUID());
        try {
            ProcessTransfur.transfur(player, ImmediateTransfurDecision.safe(variant, TransfurCause.DEFAULT, changedEntity -> {
                activateLatexNeeds(player, data);
                data.setCoverage(0.0D);
            }));
        } finally {
            ALLOWED_IMMEDIATE_TRANSFURS.remove(player.getUUID());
        }
    }

    public static boolean restoreSettledForm(ServerPlayer player, CSPPlayerData data) {
        if (hasCommandUntransfurReset(player)) {
            tryCompleteCommandUntransfurReset(player, "settled form restore after command untransfur");
            return false;
        }

        try {
            if (!data.hasSettledStrain() || ProcessTransfur.isPlayerTransfurred(player)) {
                return false;
            }

            TransfurVariant<?> variant = resolveVariant(data.getSettledStrainId());
            ALLOWED_IMMEDIATE_TRANSFURS.add(player.getUUID());
            try {
                ProcessTransfur.setPlayerTransfurVariant(player, variant, TransfurContext.hazard(TransfurCause.DEFAULT), 1.0F);
                ensureSettledState(player, data);
                syncSafely(player, data, "settled form restore");
                return true;
            } finally {
                ALLOWED_IMMEDIATE_TRANSFURS.remove(player.getUUID());
            }
        } catch (RuntimeException exception) {
            ChangedSurviveProtocol.LOGGER.warn("Failed to restore settled CSP form for {}; leaving player unchanged",
                    player.getScoreboardName(), exception);
            return false;
        }
    }

    public static boolean isImmediateTransfurAllowed(ServerPlayer player) {
        return ALLOWED_IMMEDIATE_TRANSFURS.contains(player.getUUID());
    }

    public static void onPlayerLoggedOut(UUID playerId) {
        ALLOWED_IMMEDIATE_TRANSFURS.remove(playerId);
        COMMAND_UNTRANSFUR_RESETS.remove(playerId);
        BLOCKED_PROGRESS_COMPLETIONS.remove(playerId);
        PENDING_TOTEM_FORMS.remove(playerId);
    }

    public static void expirePendingTotemForm(ServerPlayer player) {
        PendingTotemForm pending = PENDING_TOTEM_FORMS.get(player.getUUID());
        if (pending != null && pending.gameTime != player.level().getGameTime()) {
            PENDING_TOTEM_FORMS.remove(player.getUUID(), pending);
        }
    }

    public static boolean tryHandleDirectProgress(ServerPlayer player, float progress) {
        if (!canRedirectTransfur(player)) {
            return false;
        }

        float oldProgress = ProcessTransfur.getPlayerTransfurProgress(player);
        float delta = progress - oldProgress;
        if (delta <= 0.0F) {
            return false;
        }

        ProgressExposureContext context = ACTIVE_PROGRESS_CONTEXT.get();
        if (progress >= ProcessTransfur.getEntityTransfurTolerance(player)) {
            BLOCKED_PROGRESS_COMPLETIONS.put(player.getUUID(), player.level().getGameTime());
        }

        addExposure(player, context == null ? "" : context.strainId, coverageFromProgressDelta(player, delta));
        clearChangedProgress(player);
        return true;
    }

    public static AssimilationBehavior wrapLatexProgressBehavior(ServerPlayer player, LatexAssimilationDecision<?> decision, AssimilationBehavior behavior) {
        return wrapProgressBehavior(player, behavior, new ProgressExposureContext(decision.transfurVariant().getFormId().toString()));
    }

    public static AssimilationBehavior wrapNonLatexProgressBehavior(ServerPlayer player, NonLatexAssimilationDecision<?> decision, AssimilationBehavior behavior) {
        return wrapProgressBehavior(player, behavior, new ProgressExposureContext(decision.transfurVariant().getFormId().toString()));
    }

    public static AssimilationBehavior suitLatexExposureBehavior(ServerPlayer player, LatexAssimilationDecision<?> decision) {
        return exposureOnlyBehavior(player, decision.transfurVariant().getFormId().toString(), decision.transfurProgress());
    }

    public static AssimilationBehavior suitNonLatexExposureBehavior(ServerPlayer player, NonLatexAssimilationDecision<?> decision) {
        return exposureOnlyBehavior(player, decision.transfurVariant().getFormId().toString(), decision.transfurProgress());
    }

    private static AssimilationBehavior exposureOnlyBehavior(ServerPlayer player, String strainId, float progress) {
        return new AssimilationBehavior() {
            @Override
            public void stepAssimilate() {
                double remaining = Math.max(0.0D, ProcessTransfur.getEntityTransfurTolerance(player) - ProcessTransfur.getPlayerTransfurProgress(player));
                float appliedProgress = (float)Math.max(0.0D, Math.min(remaining, progress));
                addExposure(player, strainId, coverageFromProgressDelta(player, appliedProgress));
                clearChangedProgress(player);
            }

            @Override
            public boolean willAssimilate() {
                return false;
            }

            @Override
            public AssimilationBehavior appendTransfurListener(Consumer<IAbstractChangedEntity> transfurLogic) {
                return this;
            }
        };
    }

    public static boolean hasTemporarySuit(ServerPlayer player) {
        var variant = ProcessTransfur.getPlayerTransfurVariant(player);
        return variant != null && variant.isTemporaryFromSuit() && canRedirectTransfur(player);
    }

    private static AssimilationBehavior wrapProgressBehavior(ServerPlayer player, AssimilationBehavior behavior, ProgressExposureContext context) {
        return new AssimilationBehavior() {
            @Override
            public void stepAssimilate() {
                if (!canRedirectTransfur(player)) {
                    behavior.stepAssimilate();
                    return;
                }

                if (behavior.willAssimilate()) {
                    double remaining = Math.max(0.0D, ProcessTransfur.getEntityTransfurTolerance(player) - ProcessTransfur.getPlayerTransfurProgress(player));
                    addExposure(player, context.strainId, coverageFromProgressDelta(player, (float)remaining));
                    clearChangedProgress(player);
                    return;
                }

                ACTIVE_PROGRESS_CONTEXT.set(context);
                try {
                    behavior.stepAssimilate();
                } finally {
                    ACTIVE_PROGRESS_CONTEXT.remove();
                }
            }

            @Override
            public boolean willAssimilate() {
                return behavior.willAssimilate();
            }

            @Override
            public AssimilationBehavior appendTransfurListener(Consumer<IAbstractChangedEntity> transfurLogic) {
                return wrapProgressBehavior(player, behavior.appendTransfurListener(transfurLogic), context);
            }
        };
    }

    private static boolean canRedirectTransfur(ServerPlayer player) {
        return player.connection != null
                && !player.level().isClientSide
                && CSPTransfurState.isSurvivalProtocolActive(player)
                && !CSPTransfurState.hasNonSuitTransfur(player);
    }

    private static void clearChangedProgress(ServerPlayer player) {
        if (ProcessTransfur.getPlayerTransfurProgress(player) > 0.0F) {
            ProcessTransfur.setPlayerTransfurProgress(player, 0.0F);
        }
    }

    private static void addExposure(ServerPlayer player, String strainId, double amount) {
        CSPCapabilities.get(player).ifPresent(data -> {
            if (data.isInfected()) {
                data.addInfection(amount * CSPConfig.COMMON.infectionFromExtraCoverageMultiplier.get(), strainId);
                data.setCoverage(0.0D);
            } else {
                double totalCoverage = data.getCoverage() + amount;
                double threshold = CSPConfig.COMMON.coverageInfectionThreshold.get();
                if (totalCoverage >= threshold) {
                    double overflow = totalCoverage - threshold;
                    data.setCoverage(0.0D);
                    data.setInfectionPercent(Math.max(1.0D, CSPConfig.COMMON.infectionStartPercent.get()));
                    if (overflow > 0.0D) {
                        data.addInfection(overflow * CSPConfig.COMMON.infectionFromExtraCoverageMultiplier.get(), strainId);
                    }
                } else {
                    data.addCoverage(amount, strainId);
                }
            }

            syncSafely(player, data, "transfur exposure");
        });
    }

    private static boolean restoreSettledVariantAssignment(ServerPlayer player, ProcessTransfur.EntityVariantAssigned event) {
        final boolean[] restored = {false};
        try {
            CSPCapabilities.get(player).ifPresent(data -> {
                if (!data.hasSettledStrain()) {
                    return;
                }
                event.variant = resolveVariant(data.getSettledStrainId());
                ensureSettledState(player, data);
                syncSafely(player, data, "settled variant assignment");
                restored[0] = true;
            });
        } catch (RuntimeException exception) {
            ChangedSurviveProtocol.LOGGER.warn("Failed to restore settled CSP variant for {}; leaving assignment unchanged",
                    player.getScoreboardName(), exception);
        }
        return restored[0];
    }

    private static boolean hasCommandUntransfurReset(ServerPlayer player) {
        return COMMAND_UNTRANSFUR_RESETS.contains(player.getUUID());
    }

    private static void tryCompleteCommandUntransfurReset(ServerPlayer player, String reason) {
        try {
            final boolean[] reset = {false};
            CSPCapabilities.get(player).ifPresent(data -> {
                data.reset();
                syncSafely(player, data, reason);
                reset[0] = true;
            });

            if (reset[0]) {
                COMMAND_UNTRANSFUR_RESETS.remove(player.getUUID());
            }
        } catch (RuntimeException exception) {
            ChangedSurviveProtocol.LOGGER.warn("Failed to clear CSP data for {} during {}; command untransfur will continue",
                    player.getScoreboardName(), reason, exception);
        }
    }

    private static void resetAfterUntransfur(ServerPlayer player) {
        try {
            CSPCapabilities.get(player).ifPresent(data -> {
                data.setLucidityActive(false);
                data.setUnstableLatex(false);
                data.setStabilizedLatex(false);
                data.setUnstableLatexTicks(0);
                data.setLucidityDrainMultiplier(1.0D);
                data.setLucidity(100.0D);
                data.setCoverage(0.0D);
                data.setInfected(false);
                data.clearTotemForm();
                syncSafely(player, data, "untransfur cleanup");
            });
        } catch (RuntimeException exception) {
            ChangedSurviveProtocol.LOGGER.warn("Failed to clean CSP data for {} after untransfur",
                    player.getScoreboardName(), exception);
        }
    }

    private static void syncSafely(ServerPlayer player, CSPPlayerData data, String reason) {
        try {
            CSPNetwork.sync(player, data);
        } catch (RuntimeException exception) {
            ChangedSurviveProtocol.LOGGER.warn("Failed to sync CSP data for {} during {}",
                    player.getScoreboardName(), reason, exception);
        }
    }

    private static void ensureSettledState(ServerPlayer player, CSPPlayerData data) {
        data.setActiveStrainId(data.getSettledStrainId());
        data.setCoverage(0.0D);
        data.setInfected(false);
        data.setSuppressantTicks(0);
        data.setStabilizedLatex(true);
        data.setLucidityActive(false);
        data.setUnstableLatex(false);
        data.setUnstableLatexTicks(0);
        data.setLucidityDrainMultiplier(1.0D);
        data.setLucidity(Math.max(data.getLucidity(), 90.0D));
    }

    private static double coverageFromProgressDelta(ServerPlayer player, float delta) {
        double tolerance = Math.max(1.0D, ProcessTransfur.getEntityTransfurTolerance(player));
        return delta / tolerance * CSPConfig.COMMON.coverageInfectionThreshold.get();
    }

    static TransfurVariant<?> resolveVariant(String strainId) {
        if (!strainId.isBlank()) {
            ResourceLocation id = ResourceLocation.tryParse(strainId);
            if (id != null) {
                TransfurVariant<?> variant = ChangedRegistry.TRANSFUR_VARIANT.get().getValue(id);
                if (variant != null) {
                    return variant;
                }
            }
        }
        return ChangedTransfurVariants.FALLBACK_VARIANT.get();
    }

    private static void activateLatexNeeds(ServerPlayer player, CSPPlayerData data) {
        data.setInfected(false);
        data.setCoverage(0.0D);
        boolean lucidityActive = CSPTransfurState.usesLucidity(player, data);
        data.setLucidityActive(lucidityActive);
        data.setLucidity(Math.max(data.getLucidity(), 100.0D));
        data.setUnstableLatex(lucidityActive);
    }

    private static boolean consumePendingTotemForm(ServerPlayer player, TransfurVariant<?> variant) {
        PendingTotemForm pending = PENDING_TOTEM_FORMS.remove(player.getUUID());
        return pending != null
                && pending.gameTime == player.level().getGameTime()
                && pending.formId.equals(variant.getFormId());
    }

    private static boolean hasBlockedProgressCompletion(ServerPlayer player) {
        Long gameTime = BLOCKED_PROGRESS_COMPLETIONS.get(player.getUUID());
        if (gameTime == null) {
            return false;
        }
        if (gameTime != player.level().getGameTime()) {
            BLOCKED_PROGRESS_COMPLETIONS.remove(player.getUUID(), gameTime);
            return false;
        }
        return true;
    }

    private static boolean consumeBlockedProgressCompletion(ServerPlayer player) {
        if (!hasBlockedProgressCompletion(player)) {
            return false;
        }
        BLOCKED_PROGRESS_COMPLETIONS.remove(player.getUUID());
        return true;
    }

    private record ProgressExposureContext(String strainId) {
    }

    private record PendingTotemForm(ResourceLocation formId, long gameTime) {
    }
}
