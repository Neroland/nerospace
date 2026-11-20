package za.co.neroland.nerospace.world.planet.europa.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.VentWarmthRules;

/**
 * Hydrothermal Vent: a sea-floor vent. With water or brine directly above it is {@code ACTIVE}: it glows (7), counts
 * as a heat source, warms the vent basalt around it and is the base a Smoker Chimney grows on. A dry vent is inert.
 * The state follows the cell above on placement and on every neighbour change (one block read). Worldgen must place
 * it already active, since generation sends no neighbour update. Register with
 * {@code lightLevel(HydrothermalVentBlock::light)}.
 */
public class HydrothermalVentBlock extends Block {

    public static final MapCodec<HydrothermalVentBlock> CODEC = BlockCodecs.simple(HydrothermalVentBlock::new);

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    @SuppressWarnings("this-escape")
    public HydrothermalVentBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    protected MapCodec<? extends HydrothermalVentBlock> codec() {
        return CODEC;
    }

    /** Light for a state, for the registered properties. */
    public static int light(BlockState state) {
        return VentWarmthRules.light(state.getValue(ACTIVE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ACTIVE, activeAt(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        refresh(state, level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
        refresh(state, level, pos);
    }

    private static boolean activeAt(Level level, BlockPos pos) {
        return VentWarmthRules.active(EuropaHeat.isLiquid(level.getBlockState(pos.above())));
    }

    private static void refresh(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        boolean active = activeAt(level, pos);
        if (active != state.getValue(ACTIVE)) {
            level.setBlock(pos, state.setValue(ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE) && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, pos.getX() + 0.25 + random.nextDouble() * 0.5,
                    pos.getY() + 1.05, pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0.0, 0.08, 0.0);
        }
    }
}
