package net.stonenibbler.changed_survive_protocol.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.data.CSPCapabilities;
import net.stonenibbler.changed_survive_protocol.common.network.CSPNetwork;
import net.stonenibbler.changed_survive_protocol.common.util.CSPTransfurState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StabilizationDoseItem extends StrainTaggedItem {
    public StabilizationDoseItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.changed_survive_protocol.permanent_warning").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!CSPTransfurState.hasNonSuitTransfur(player)) {
            player.displayClientMessage(Component.translatable("message.changed_survive_protocol.stabilization.not_latex"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!CSPStrainItems.matchesCurrentLatex(player, stack)) {
            player.displayClientMessage(Component.translatable("message.changed_survive_protocol.stabilization.wrong_strain"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!CSPConfig.COMMON.lucidityMechanicsEnabled.get()) {
            player.displayClientMessage(Component.translatable("message.changed_survive_protocol.stabilization.disabled"), true);
            return InteractionResultHolder.fail(stack);
        }

        final boolean[] used = {false};
        CSPCapabilities.get(serverPlayer).ifPresent(data -> {
            if (data.hasSettledStrain()) {
                player.displayClientMessage(Component.translatable("message.changed_survive_protocol.stabilization.already_settled"), true);
                return;
            }
            if (data.getLucidity() < CSPConfig.COMMON.stabilizationRequiredLucidity.get()) {
                player.displayClientMessage(Component.translatable("message.changed_survive_protocol.stabilization.low_lucidity"), true);
                return;
            }
            String strainId = CSPStrainItems.strainId(stack);
            data.setSettledStrainId(strainId);
            data.setActiveStrainId(strainId);
            data.setStabilizedLatex(true);
            data.setLucidityActive(false);
            data.setUnstableLatex(false);
            data.setUnstableLatexTicks(0);
            data.setLucidityDrainMultiplier(1.0D);
            data.setLucidity(Math.max(data.getLucidity(), 90.0D));
            CSPNetwork.sync(serverPlayer, data);
            player.displayClientMessage(Component.translatable("message.changed_survive_protocol.stabilization.settled"), true);
            used[0] = true;
        });

        if (!used[0]) {
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
