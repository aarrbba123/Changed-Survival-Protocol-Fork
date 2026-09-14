package net.stonenibbler.changed_survive_protocol.common.infestation;

import net.ltxprogrammer.changed.block.DoubleBlockPlace;
import net.ltxprogrammer.changed.block.WhiteLatexPillar;
import net.ltxprogrammer.changed.init.ChangedBlocks;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.ltxprogrammer.changed.world.LatexCoverState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.stonenibbler.changed_survive_protocol.common.gamerule.CSPGameRules;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

final class LatexHeartDecorations {
    private static final int MIN_DECORATION_CLAIMS = 80;
    private static final int MAX_DECORATIONS = 128;
    private static final int DECORATION_SPACING_SQR = 25;
    private static final int MAX_DECORATION_PLACEMENTS_PER_GROWTH = 3;
    private static final List<Supplier<? extends Block>> DARK_SMALL_CRYSTALS = List.<Supplier<? extends Block>>of(
            ChangedBlocks.LATEX_CRYSTAL,
            ChangedBlocks.DARK_DRAGON_CRYSTAL,
            ChangedBlocks.BEIFENG_CRYSTAL_SMALL,
            ChangedBlocks.WOLF_CRYSTAL_SMALL
    );
    private static final List<Supplier<? extends Block>> DARK_TALL_CRYSTALS = List.<Supplier<? extends Block>>of(
            ChangedBlocks.DARK_LATEX_CRYSTAL_LARGE,
            ChangedBlocks.BEIFENG_CRYSTAL,
            ChangedBlocks.WOLF_CRYSTAL
    );

    private LatexHeartDecorations() {
    }

    static void maybeDecorate(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, int claimCount) {
        if (!level.getGameRules().getBoolean(CSPGameRules.DO_LATEX_HEART_INFESTATIONS)
                || !level.getGameRules().getBoolean(CSPGameRules.LATEX_HEART_SPECIAL_BLOCKS)
                || claimCount < MIN_DECORATION_CLAIMS) {
            return;
        }

        int decorationCap = decorationCap(claimCount);
        cleanupTrackedDecorations(level, data, heart);
        int missingDecorations = decorationCap - data.decorationCount(heart.id());
        if (decorationCap <= 0 || missingDecorations <= 0) {
            return;
        }

        if (missingDecorations < Math.max(2, decorationCap / 8) && level.random.nextInt(3) != 0) {
            return;
        }

        int placements = Math.min(MAX_DECORATION_PLACEMENTS_PER_GROWTH, Math.max(1, missingDecorations / 16));
        int placed = 0;
        for (int i = 0; i < placements * 16 && placed < placements; i++) {
            BlockPos pos = LatexCoverRules.randomOwnedCover(level, data, heart);
            if (pos == null || pos.distSqr(heart.pos()) < 9.0D || !hasDecorationSpacing(data, heart, pos)) {
                continue;
            }

            if (placeDecoration(level, data, heart, pos)) {
                placed++;
            }
        }
    }

    private static int decorationCap(int claimCount) {
        if (claimCount < MIN_DECORATION_CLAIMS) {
            return 0;
        }
        return Math.min(MAX_DECORATIONS, Math.max(2, claimCount / 45));
    }

    private static boolean hasDecorationSpacing(LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, BlockPos pos) {
        for (BlockPos decoration : data.decorationPositions(heart.id())) {
            if (decoration.distSqr(pos) < DECORATION_SPACING_SQR) {
                return false;
            }
        }
        return true;
    }

    private static boolean placeDecoration(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, BlockPos pos) {
        if (!canPlaceDecorationAt(level, pos)) {
            return false;
        }

        int roll = level.random.nextInt(10);
        if (roll < 5 && placeLatexBlock(level, data, heart, pos)) {
            return true;
        }
        if (placeSpecial(level, data, heart, pos)) {
            return true;
        }
        return placeLatexBlock(level, data, heart, pos);
    }

    static boolean canPlaceDecorationAt(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        AABB box = new AABB(pos);
        return level.getEntities((Entity)null, box, entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator() && entity.getBoundingBox().intersects(box)).isEmpty();
    }

    private static void cleanupTrackedDecorations(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart) {
        for (BlockPos pos : data.decorationsFor(heart.id())) {
            if (level.isLoaded(pos) && !data.shouldRemoveDecorationBlock(pos, level.getBlockState(pos))) {
                data.removeDecoration(pos);
            }
        }
    }

    private static boolean placeLatexBlock(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, BlockPos pos) {
        if (!LatexCoverRules.isOwnedCover(level, data, heart, pos) || !canPlaceDecorationAt(level, pos)) {
            return false;
        }
        level.setBlockAndUpdate(pos, LatexInfestationBlocks.sourceBlock(heart.kind()).defaultBlockState());
        data.addDecoration(pos, heart.id(), level.getBlockState(pos).getBlock());
        return true;
    }

    private static boolean placeSpecial(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, BlockPos pos) {
        if (!LatexCoverRules.isOwnedCover(level, data, heart, pos) || !canPlaceDecorationAt(level, pos)) {
            return false;
        }
        List<BlockSnapshot> before = snapshotDecorationArea(level, pos);
        Block block = heart.kind() == LatexHeartBlock.Kind.DARK ? chooseDarkCrystal(level) : ChangedBlocks.WHITE_LATEX_PILLAR.get();
        if (needsHeadroom(block) && (!level.isLoaded(pos.above()) || !level.getBlockState(pos.above()).isAir() || !canPlaceDecorationAt(level, pos.above()))) {
            return false;
        }
        LatexCoverState.setAtAndUpdate(level, pos, LatexInfestationBlocks.floorCover(heart.kind()));
        if (!block.defaultBlockState().canSurvive(level, pos)) {
            LatexCoverState.setAtAndUpdate(level, pos, ChangedLatexTypes.NONE.get().defaultCoverState());
            return false;
        }
        if (block == ChangedBlocks.WHITE_LATEX_PILLAR.get()) {
            ((WhiteLatexPillar)block).placeAt(level, block.defaultBlockState(), pos, 3);
        } else if (block instanceof DoubleBlockPlace doubleBlock) {
            doubleBlock.placeAt(level, block.defaultBlockState(), pos, 3);
        } else {
            level.setBlockAndUpdate(pos, block.defaultBlockState());
        }
        trackPlacedDecorationBlocks(level, data, heart, before);
        return true;
    }

    private static Block chooseDarkCrystal(ServerLevel level) {
        List<Supplier<? extends Block>> crystals = level.random.nextInt(4) == 0 ? DARK_TALL_CRYSTALS : DARK_SMALL_CRYSTALS;
        return crystals.get(level.random.nextInt(crystals.size())).get();
    }

    private static boolean needsHeadroom(Block block) {
        return block == ChangedBlocks.WHITE_LATEX_PILLAR.get() || block instanceof DoubleBlockPlace;
    }

    private static List<BlockSnapshot> snapshotDecorationArea(ServerLevel level, BlockPos origin) {
        List<BlockSnapshot> blocks = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, -1, -1), origin.offset(1, 2, 1))) {
            BlockPos immutable = pos.immutable();
            if (!level.isLoaded(immutable)) {
                continue;
            }
            blocks.add(new BlockSnapshot(immutable, level.getBlockState(immutable).getBlock()));
        }
        return blocks;
    }

    private static void trackPlacedDecorationBlocks(ServerLevel level, LatexInfestationSavedData data, LatexInfestationSavedData.HeartRecord heart, List<BlockSnapshot> before) {
        for (BlockSnapshot snapshot : before) {
            if (!level.isLoaded(snapshot.pos())) {
                continue;
            }
            BlockState state = level.getBlockState(snapshot.pos());
            if (!state.isAir() && state.getBlock() != snapshot.block()) {
                data.addDecoration(snapshot.pos(), heart.id(), state.getBlock());
            }
        }
    }

    private record BlockSnapshot(BlockPos pos, Block block) {
    }
}
