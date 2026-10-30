package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.PillarRules;

/**
 * Self-extending pillar (Basalt Column): a player placement schedules a tick every 20 ticks that adds the next
 * {@code SEGMENT} above while the cell is air and unobstructed, to height 4 (≤ 3 scheduled ticks per placement).
 * Worldgen places full columns directly. Growth is free but not harvestable: only {@code SEGMENT=0} drops the item
 * (the loot table), grown segments 1–3 drop nothing unless mined with Silk Touch, so one item never becomes four.
 */
public class PillarGrowthBlock extends Block {

    public static final MapCodec<PillarGrowthBlock> CODEC = BlockCodecs.simple(PillarGrowthBlock::new);

    public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 0, PillarRules.MAX_HEIGHT - 1);

    @SuppressWarnings("this-escape")
    public PillarGrowthBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(SEGMENT, 0));
    }

    protected MapCodec<? extends PillarGrowthBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SEGMENT);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer != null) {
            level.scheduleTick(pos, this, PillarRules.DELAY_TICKS);
        }
    }

    /** Column height counted downward through this block, including {@code top}. */
    private int heightAt(ServerLevel level, BlockPos top) {
        int h = 0;
        BlockPos.MutableBlockPos p = top.mutable();
        while (h < PillarRules.MAX_HEIGHT && level.getBlockState(p).is(this)) {
            h++;
            p.move(0, -1, 0);
        }
        return h;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        BlockState next = defaultBlockState();
        boolean free = level.getBlockState(above).isAir() && level.isUnobstructed(next, above, CollisionContext.empty());
        int segment = PillarRules.nextSegment(heightAt(level, pos), PillarRules.MAX_HEIGHT, free);
        if (segment < 0) {
            return;
        }
        level.setBlockAndUpdate(above, next.setValue(SEGMENT, Math.min(segment, PillarRules.MAX_HEIGHT - 1)));
        level.scheduleTick(above, this, PillarRules.DELAY_TICKS);
    }
}
