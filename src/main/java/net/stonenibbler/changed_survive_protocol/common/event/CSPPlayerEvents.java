package net.stonenibbler.changed_survive_protocol.common.event;

import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.stonenibbler.changed_survive_protocol.common.collapse.FeralBodySpawner;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.data.CSPCapabilities;
import net.stonenibbler.changed_survive_protocol.common.data.CSPPlayerData;
import net.stonenibbler.changed_survive_protocol.common.latex.LatexStrandManager;
import net.stonenibbler.changed_survive_protocol.common.network.CSPNetwork;
import net.stonenibbler.changed_survive_protocol.common.util.CSPTransfurState;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

public final class CSPPlayerEvents {
    private static final Map<UUID, CSPPlayerData> DEATH_SNAPSHOTS = new HashMap<>();

    private CSPPlayerEvents() {
    }

    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        CSPCapabilities.attachPlayerData(event);
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        try {
            CSPCapabilities.get(event.getEntity()).ifPresent(newData -> {
                CSPPlayerData deathSnapshot = event.isWasDeath() ? DEATH_SNAPSHOTS.remove(event.getOriginal().getUUID()) : null;
                if (deathSnapshot != null) {
                    newData.copyFrom(deathSnapshot);
                    return;
                }

                CSPCapabilities.get(event.getOriginal()).ifPresent(newData::copyFrom);
            });
        } finally {
            event.getOriginal().invalidateCaps();
        }
    }

    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CSPCapabilities.get(player).ifPresent(data -> {
            CSPPlayerData snapshot = new CSPPlayerData();
            snapshot.copyFrom(data);
            DEATH_SNAPSHOTS.put(player.getUUID(), snapshot);
        });
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        DEATH_SNAPSHOTS.remove(event.getEntity().getUUID());
        sync(event.getEntity());
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        DEATH_SNAPSHOTS.remove(event.getEntity().getUUID());
        CSPTransfurEvents.onPlayerLoggedOut(event.getEntity().getUUID());
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event.getEntity());
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getEntity());
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        CSPTransfurEvents.expirePendingTotemForm(serverPlayer);
        CSPCapabilities.get(serverPlayer).ifPresent(data -> {
            boolean dirty = tick(serverPlayer, data);
            if (dirty || serverPlayer.tickCount % 100 == 0) {
                CSPNetwork.sync(serverPlayer, data);
            }
        });
    }

    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CSPCapabilities.get(serverPlayer).ifPresent(data -> CSPNetwork.sync(serverPlayer, data));
        }
    }

    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewTarget() instanceof Player player) || !ProcessTransfur.isPlayerLatex(player)) {
            return;
        }

        if (LatexStrandManager.isSociallyFriendly(event.getEntity(), player)) {
            event.setNewTarget(null);
        }
    }

    public static void onFoodFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !CSPTransfurState.usesLucidity(player)) {
            return;
        }
        if (event.getItem().getFoodProperties(player) == null) {
            return;
        }

        var food = event.getItem().getFoodProperties(player);
        double recovered = food.getNutrition() * CSPConfig.COMMON.lucidityRecoveryPerFoodNutrition.get()
                + food.getSaturationModifier() * CSPConfig.COMMON.lucidityRecoveryPerFoodSaturation.get();
        if (recovered <= 0.0D) {
            return;
        }

        CSPCapabilities.get(player).ifPresent(data -> {
            if (!CSPTransfurState.usesLucidity(player, data)) {
                return;
            }
            data.addLucidity(recovered);
            CSPNetwork.sync(player, data);
        });
    }

    private static boolean tick(ServerPlayer player, CSPPlayerData data) {
        boolean dirty = false;

        if (!CSPTransfurState.isSurvivalProtocolActive(player)) {
            if (data.isLucidityActive()) {
                data.setLucidityActive(false);
                return true;
            }
            return false;
        }

        boolean suppressedThisTick = data.getSuppressantTicks() > 0;
        data.tickSuppressant();

        boolean anyTransfurred = ProcessTransfur.isPlayerTransfurred(player);
        boolean transfurred = CSPTransfurState.hasNonSuitTransfur(player);
        boolean lucidityEnabled = CSPTransfurState.usesLucidity(player, data);
        if (!anyTransfurred && data.hasSettledStrain()) {
            dirty |= CSPTransfurEvents.restoreSettledForm(player, data);
            transfurred = CSPTransfurState.hasNonSuitTransfur(player);
            lucidityEnabled = CSPTransfurState.usesLucidity(player, data);
        }

        if (data.isLucidityActive() != lucidityEnabled) {
            data.setLucidityActive(lucidityEnabled);
            dirty = true;
        }

        if (transfurred) {
            if (data.getCoverage() > 0.0D || data.isInfected()) {
                data.setCoverage(0.0D);
                data.setInfected(false);
                dirty = true;
            }
            if (lucidityEnabled) {
                dirty |= tickLatexNeeds(player, data);
            }
        }

        if (!transfurred) {
            if (!data.isInfected() && data.getCoverage() >= CSPConfig.COMMON.coverageInfectionThreshold.get()) {
                startInfection(data);
                dirty = true;
            }

            if (!data.isInfected() && isBeingWashed(player) && data.getCoverage() > 0.0D) {
                double washAmount = player.isInWater() ? CSPConfig.COMMON.waterCoverageWashPerTick.get() : CSPConfig.COMMON.rainCoverageWashPerTick.get();
                data.setCoverage(data.getCoverage() - washAmount);
                dirty = true;
            } else if (!data.isInfected() && data.getCoverage() > 0.0D && player.tickCount % CSPConfig.COMMON.passiveCoverageDecayIntervalTicks.get() == 0) {
                data.setCoverage(data.getCoverage() - CSPConfig.COMMON.passiveCoverageDecayAmount.get());
                dirty = true;
            } else if (!data.isInfected() && data.getCoverage() <= 0.0D) {
                data.cleanupInfectionMap();
            }

            if (data.isInfected()) {
                if (data.getCoverage() > 0.0D) {
                    data.setCoverage(0.0D);
                    dirty = true;
                }

                if (data.getInfectionPercent() < 100.0D && !suppressedThisTick && player.tickCount % CSPConfig.COMMON.infectionGrowthIntervalTicks.get() == 0) {
                    data.addInfection(CSPConfig.COMMON.infectionGrowthPerInterval.get(), "");
                    dirty = true;
                }

                if (data.getInfectionPercent() >= 100.0D) {
                    CSPTransfurEvents.forceUncontrolledTransfur(player, data);
                    dirty = true;
                }
            }
        }

        if (lucidityEnabled && data.getLucidity() <= 0.0D) {
            FeralBodySpawner.forceCollapse(player, data);
            dirty = true;
        }

        return dirty;
    }

    private static boolean tickLatexNeeds(ServerPlayer player, CSPPlayerData data) {
        if (data.isStabilizedLatex()) {
            return false;
        }

        boolean dirty = false;

        if (!data.isUnstableLatex()) {
            data.setUnstableLatex(true);
            dirty = true;
        }
        data.addUnstableLatexTick();

        if (player.tickCount % CSPConfig.COMMON.latexNeedIntervalTicks.get() == 0) {
            double multiplierRange = CSPConfig.COMMON.maxUnstableLucidityMultiplier.get() - 1.0D;
            double multiplierProgress = Math.min(1.0D, data.getUnstableLatexTicks() / (double)CSPConfig.COMMON.unstableTicksForMaxMultiplier.get());
            data.setLucidityDrainMultiplier(1.0D + multiplierRange * multiplierProgress);
            double lucidityDrain = CSPConfig.COMMON.unstableLucidityDrain.get() * data.getLucidityDrainMultiplier();
            dirty |= CSPLucidityEvents.tickLatexEnvironment(player, data, lucidityDrain);
        }

        return dirty;
    }

    private static void startInfection(CSPPlayerData data) {
        data.setCoverage(0.0D);
        data.setInfectionPercent(Math.max(1.0D, CSPConfig.COMMON.infectionStartPercent.get()));
    }

    private static boolean isBeingWashed(Player player) {
        return player.isInWater() || player.isInWaterRainOrBubble();
    }
}
