package net.stonenibbler.changed_survive_protocol.common.item;

import net.ltxprogrammer.changed.init.ChangedDamageSources;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.data.CSPCapabilities;
import net.stonenibbler.changed_survive_protocol.common.network.CSPNetwork;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPItems;
import net.stonenibbler.changed_survive_protocol.common.util.CSPInventoryUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SamplingScalpelItem extends Item {
    public SamplingScalpelItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.changed_survive_protocol.scalpel_warning").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (ProcessTransfur.isPlayerTransfurred(player)) {
            player.displayClientMessage(Component.translatable("message.changed_survive_protocol.scalpel.use_syringe"), true);
            return InteractionResultHolder.fail(stack);
        }

        final boolean[] used = {false};
        CSPCapabilities.get(serverPlayer).ifPresent(data -> {
            if (!data.isInfected() || data.getActiveStrainId().isBlank()) {
                player.displayClientMessage(Component.translatable("message.changed_survive_protocol.scalpel.no_strain"), true);
                return;
            }
            String strainId = data.getActiveStrainId();
            float configuredDamage = CSPConfig.COMMON.sampleExtractionDamage.get().floatValue();
            if (configuredDamage > 0.0F) {
                float minHealth = CSPConfig.COMMON.sampleExtractionMinHealth.get().floatValue();
                if (player.getHealth() <= minHealth) {
                    player.displayClientMessage(Component.translatable("message.changed_survive_protocol.scalpel.too_weak"), true);
                    return;
                }
                float damage = Math.min(configuredDamage, player.getHealth() - minHealth);
                if (!player.hurt(ChangedDamageSources.BLOODLOSS.source(player.level().registryAccess()), damage)) {
                    return;
                }
            }
            double remainingInfection = data.getInfectionPercent() - CSPConfig.COMMON.sampleExtractionInfectionRemoval.get();
            if (remainingInfection <= 0.0D) {
                data.clearInfection();
            } else {
                data.setInfectionPercent(remainingInfection);
            }
            CSPNetwork.sync(serverPlayer, data);
            CSPInventoryUtil.giveOrDrop(player, CSPStrainItems.withStrain(new ItemStack(CSPItems.RAW_LATEX_SAMPLE.get()), strainId));
            used[0] = true;
        });

        if (!used[0]) {
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
