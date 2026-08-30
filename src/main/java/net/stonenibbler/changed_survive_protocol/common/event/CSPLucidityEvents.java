package net.stonenibbler.changed_survive_protocol.common.event;

import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.entity.latex.LatexType;
import net.ltxprogrammer.changed.init.ChangedTags;
import net.ltxprogrammer.changed.process.TransfurEvents;
import net.ltxprogrammer.changed.world.LatexCoverState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.data.CSPCapabilities;
import net.stonenibbler.changed_survive_protocol.common.data.CSPPlayerData;
import net.stonenibbler.changed_survive_protocol.common.item.CSPStrainItems;
import net.stonenibbler.changed_survive_protocol.common.latex.LatexStrandManager;
import net.stonenibbler.changed_survive_protocol.common.network.CSPNetwork;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPItems;
import net.stonenibbler.changed_survive_protocol.common.util.CSPTransfurState;

public final class CSPLucidityEvents {
    private static final int SMALL_LATEX_COUNT = 3;
    private static final int MEDIUM_LATEX_COUNT = 8;
    private static final int LARGE_LATEX_COUNT = 16;
    private static final int NEST_LATEX_COUNT = 12;

    private static final int SMALL_TRANSFURRED_COUNT = 2;
    private static final int MEDIUM_TRANSFURRED_COUNT = 6;
    private static final int LARGE_TRANSFURRED_COUNT = 12; // large crowds

    private CSPLucidityEvents() {
    }

    public static boolean tickLatexEnvironment(ServerPlayer player, CSPPlayerData data, double lucidityDrain) {
        if (!CSPTransfurState.usesLucidity(player, data) || player.tickCount % CSPConfig.COMMON.latexNeedIntervalTicks.get() != 0) {
            return false;
        }

        boolean dirty = false;
        boolean nearFriendlyLatex = false;
        boolean nearTransfurredCrowd = false;
        double recovery = 0.0D;
        LatexStrandManager.Strand strand = LatexStrandManager.resolve(player).orElse(null);
        if (strand != null) {
            int count = countFriendlyLatex(player.level(), player.blockPosition(), strand.latexType(), 2, 1, 2);
            nearFriendlyLatex = count >= SMALL_LATEX_COUNT;
            boolean aquaticUnderwater = strand.family() == LatexStrandManager.Family.AQUATIC && player.isEyeInFluid(FluidTags.WATER);
            if (nearFriendlyLatex) {
                recovery += count >= LARGE_LATEX_COUNT
                        ? CSPConfig.COMMON.lucidityRecoveryNearLatexLarge.get()
                        : count >= MEDIUM_LATEX_COUNT
                        ? CSPConfig.COMMON.lucidityRecoveryNearLatexMedium.get()
                        : CSPConfig.COMMON.lucidityRecoveryNearLatexSmall.get();
            }

            boolean landAbovewater = strand.family() != LatexStrandManager.Family.AQUATIC && !player.isEyeInFluid(FluidTags.WATER);
            int population = 0;
            // Check if underwater and aquatic, or above water and not aquatic
            if (aquaticUnderwater) {
                recovery += CSPConfig.COMMON.lucidityRecoveryAquaticUnderwater.get();

                population = countAquaticTransfurs(player.level(), player.blockPosition(), player, !isOrganic(strand));
            } else if (landAbovewater) {
                population = countLandTransfurs(player.level(), player.blockPosition(), player, !isOrganic(strand));
            }

            nearTransfurredCrowd = population >= SMALL_TRANSFURRED_COUNT;
            if (nearTransfurredCrowd) {
                recovery += population >= LARGE_TRANSFURRED_COUNT
                ? CSPConfig.COMMON.lucidityRecoveryNearTransfurredLarge.get()
                : population >= MEDIUM_TRANSFURRED_COUNT
                ? CSPConfig.COMMON.lucidityRecoveryNearTransfurredMedium.get()
                : CSPConfig.COMMON.lucidityRecoveryNearTransfurredSmall.get();
            }
        }

        double delta = recovery - Math.max(0.0D, lucidityDrain);
        if (delta != 0.0D) {
            data.addLucidity(delta);
            dirty = true;
        }

        if ((nearTransfurredCrowd || nearFriendlyLatex) && data.getLucidity() >= 70.0D && player.tickCount % 1200 == 0) {
            dirty |= attuneCarriedCulturedStrands(player, CSPConfig.COMMON.culturedStrandPassiveAttunement.get());
        }

        return dirty;
    }

    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !CSPTransfurState.usesLucidity(player) || !player.isSleepingLongEnough()) {
            return;
        }
        BlockPos bedPos = player.getSleepingPos().orElse(null);
        if (bedPos == null) {
            return;
        }
        LatexStrandManager.Strand strand = LatexStrandManager.resolve(player).orElse(null);
        if (strand == null || !isLatexNest(player.level(), bedPos, strand.latexType())) {
            return;
        }
        CSPCapabilities.get(player).ifPresent(data -> {
            if (!CSPTransfurState.usesLucidity(player, data)) {
                return;
            }
            data.addLucidity(CSPConfig.COMMON.lucidityRecoveryFromLatexNestSleep.get());
            attuneCarriedCulturedStrands(player, CSPConfig.COMMON.culturedStrandNestAttunement.get());
            CSPNetwork.sync(player, data);
        });
    }

    public static void onAssimilatedEntity(TransfurEvents.AssimilatedEntityEvent event) {
        rewardAssimilation(event.entity);
    }

    public static void onAbsorbedEntity(TransfurEvents.AbsorbedEntityEvent event) {
        rewardAssimilation(event.entity);
    }

    private static void rewardAssimilation(IAbstractChangedEntity source) {
        if (source.getEntity() instanceof Player player) {
            rewardAssimilation(player);
        }
    }

    public static void rewardAssimilation(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !CSPTransfurState.usesLucidity(player)) {
            return;
        }
        CSPCapabilities.get(serverPlayer).ifPresent(data -> {
            if (!CSPTransfurState.usesLucidity(serverPlayer, data)) {
                return;
            }
            data.addLucidity(CSPConfig.COMMON.lucidityRecoveryFromAssimilation.get());
            attuneCarriedCulturedStrands(serverPlayer, CSPConfig.COMMON.culturedStrandAssimilationAttunement.get());
            CSPNetwork.sync(serverPlayer, data);
        });
    }

    public static boolean attuneCarriedCulturedStrands(Player player, double amount) {
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(CSPItems.CULTURED_LATEX_STRAND.get()) || !CSPStrainItems.matchesCurrentLatex(player, stack)) {
                continue;
            }
            double attunement = CSPStrainItems.attunement(stack) + amount;
            if (attunement >= 100.0D) {
                ItemStack dose = CSPStrainItems.withStrain(new ItemStack(CSPItems.STABILIZATION_DOSE.get()), CSPStrainItems.strainId(stack));
                player.getInventory().setItem(slot, dose);
            } else {
                CSPStrainItems.withAttunement(stack, attunement);
            }
            changed = true;
        }
        return changed;
    }

    private static boolean isLatexNest(Level level, BlockPos bedPos, LatexType playerType) {
        BlockPos center = bedPos;
        BlockState state = level.getBlockState(bedPos);
        if (state.getBlock() instanceof BedBlock && state.hasProperty(BedBlock.PART)) {
            Direction facing = state.getValue(BedBlock.FACING);
            center = state.getValue(BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD ? bedPos.relative(facing.getOpposite()) : bedPos;
        }
        return countFriendlyLatex(level, center, playerType, 2, 1, 2) >= NEST_LATEX_COUNT;
    }

    private static boolean isOrganic(LatexStrandManager.Strand strand) {
        return strand.family() == LatexStrandManager.Family.INDEPENDENT || strand.family() == LatexStrandManager.Family.UNKNOWN;
    }

    private static int countLandTransfurs(Level level, BlockPos center, ServerPlayer player, boolean isLatex) {
        int radius = Math.max(1, CSPConfig.COMMON.transfurredCheckRadius.get());
        AABB area = new AABB(center).inflate(radius);
        return level.getEntitiesOfClass(
            Mob.class, 
            area, 
            mob -> mob.isAlive() 
            && isValidLandTransfur(mob)
            && (isLatex ? LatexStrandManager.isSociallyFriendly(mob, player) : true) // Still perform the check when latex
        ).size();
    }

    // Unlike aquatic-based organics (which is in the AQUATIC family), land-based organics will probably be in the INDEPENDENT family (which has a more stricter same-strain check).
    // We don't want that, as organics (both land & aquatic) are already hard to find in-game, let alone ones with the same strain as the player.
    private static boolean isValidLandTransfur(Mob mob) {
        LatexStrandManager.Strand strand = LatexStrandManager.resolve(mob).orElse(null);
        if (strand == null || strand.family() == LatexStrandManager.Family.AQUATIC) {
            return false;
        }
        return true;
    }

    private static int countAquaticTransfurs(Level level, BlockPos center, ServerPlayer player, boolean isLatex) {
        int radius = Math.max(1, CSPConfig.COMMON.transfurredCheckRadius.get());
        AABB area = new AABB(center).inflate(radius);
        return level.getEntitiesOfClass(
            Mob.class,
            area, 
            mob -> mob.isAlive() 
            && isValidAquaticTransfur(mob, player)
            && (isLatex ? LatexStrandManager.isSociallyFriendly(mob, player) : true)
            ).size();
    }

    private static boolean isValidAquaticTransfur(Mob mob, ServerPlayer player) {
        LatexStrandManager.Strand mobStrand = LatexStrandManager.resolve(mob).orElse(null);
        LatexStrandManager.Strand playerStrand = LatexStrandManager.resolve(player).orElse(null);

        if (mobStrand == null || playerStrand == null) {
            return false;
        }

        return LatexStrandManager.isSociallyFriendly(mobStrand, playerStrand);
    }

    private static int countFriendlyLatex(Level level, BlockPos center, LatexType playerType, int xzRange, int downRange, int upRange) {
        int count = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -xzRange; dx <= xzRange; dx++) {
            for (int dy = -downRange; dy <= upRange; dy++) {
                for (int dz = -xzRange; dz <= xzRange; dz++) {
                    mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (isFriendlyLatex(level, mutable, playerType)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static boolean isFriendlyLatex(Level level, BlockPos pos, LatexType playerType) {
        LatexCoverState coverState = LatexCoverState.getAt(level, pos);
        if (!coverState.isAir() && LatexStrandManager.samePhysicalLatex(playerType, coverState.getType())) {
            return true;
        }
        Block block = level.getBlockState(pos).getBlock();
        LatexType blockType = LatexStrandManager.latexTypeForBlock(block);
        return blockType != null && LatexStrandManager.samePhysicalLatex(playerType, blockType);
    }
}
