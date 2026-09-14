package net.stonenibbler.changed_survive_protocol.common.item;

import net.ltxprogrammer.changed.process.ProcessTransfur;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StrandCureDoseItem extends StrainTaggedItem {
    public StrandCureDoseItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.changed_survive_protocol.infection_only").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (ProcessTransfur.isPlayerTransfurred(player)) {
            player.displayClientMessage(Component.translatable("message.changed_survive_protocol.cure.no_effect_latex"), true);
            return InteractionResultHolder.fail(stack);
        }

        String doseStrain = CSPStrainItems.strainId(stack);
        if (doseStrain.isBlank()) {
            player.displayClientMessage(Component.translatable("message.changed_survive_protocol.cure.unidentified"), true);
            return InteractionResultHolder.fail(stack);
        }

        final boolean[] used = {false};
        CSPCapabilities.get(serverPlayer).ifPresent(data -> {
            if (!data.isInfected()) {
                player.displayClientMessage(Component.translatable("message.changed_survive_protocol.cure.no_infection"), true);
                return;
            }
            if (!doseStrain.equals(data.getActiveStrainId())) {
                player.displayClientMessage(Component.translatable("message.changed_survive_protocol.cure.wrong_strain"), true);
                return;
            }
            data.setSuppressantTicks(Math.max(data.getSuppressantTicks(), CSPConfig.COMMON.cureDoseSuppressantTicks.get()));
            data.setInfectionPercent(data.getInfectionPercent() - CSPConfig.COMMON.cureDoseInfectionRemoval.get());
            if (!data.isInfected()) {
                data.setActiveStrainId("");
                data.setSuppressantTicks(0);
            }
            CSPNetwork.sync(serverPlayer, data);
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
