package za.co.neroland.nerospace.pipe;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.platform.EnergyLookup;
import za.co.neroland.nerospace.platform.FluidLookup;
import za.co.neroland.nerospace.platform.GasLookup;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Universal Pipe block — a connection-aware transmitter that joins to adjacent pipes and to any block
 * exposing an energy/fluid/gas storage (via the cross-loader lookup seams) or a vanilla item
 * {@link Container}. Six boolean connection properties drive the multipart tube model (core + arms) and
 * the voxel shape. It ticks its {@link UniversalPipeBlockEntity} relay; sneak-empty-hand pops upgrades.
 *
 * <p>Cross-loader port: connections are recomputed from the block entity's server tick (see
 * {@link UniversalPipeBlockEntity}) rather than via {@code neighborChanged}/{@code updateShape} — those
 * 26.x override signatures (with {@code Orientation} / {@code ScheduledTickAccess}) are version-fragile,
 * whereas a throttled tick refresh uses only stable APIs and connects within a tick. {@code canConnect}
 * uses the {@code EnergyLookup}/{@code FluidLookup}/{@code GasLookup} seams + {@code Container} adjacency
 * instead of the root's NeoForge {@code Capabilities}.</p>
 */
public class UniversalPipeBlock extends BaseEntityBlock {

    public static final MapCodec<UniversalPipeBlock> CODEC = simpleCodec(UniversalPipeBlock::new);

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    /** Connection property per direction, ordered by {@link Direction#get3DDataValue()}. */
    public static final BooleanProperty [] CONNECTIONS = {DOWN, UP, NORTH, SOUTH, WEST, EAST};

    /** Voxel shapes indexed by the 6-bit connection mask (bit = direction 3D data value). */
    private static final VoxelShape [] SHAPES = buildShapes();

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public UniversalPipeBlock(Properties properties) {
        super(NerospaceCommon.requireNonNull(properties));
        BlockState base = NerospaceCommon.requireNonNull(this.stateDefinition.any());
        for (BooleanProperty prop : CONNECTIONS) {
            base = NerospaceCommon.requireNonNull(base.setValue(prop, false));
        }
        registerDefaultState(NerospaceCommon.requireNonNull(base));
    }

    private static VoxelShape [] buildShapes() {
        VoxelShape core = Block.box(4, 4, 4, 12, 12, 12);
        VoxelShape [] arms = {
                Block.box(4, 0, 4, 12, 4, 12),   // down
                Block.box(4, 12, 4, 12, 16, 12), // up
                Block.box(4, 4, 0, 12, 12, 4),   // north
                Block.box(4, 4, 12, 12, 12, 16), // south
                Block.box(0, 4, 4, 4, 12, 12),   // west
                Block.box(12, 4, 4, 16, 12, 12), // east
        };
        VoxelShape [] shapes = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (int d = 0; d < 6; d++) {
                if ((mask & (1 << d)) != 0) {
                    shape = Shapes.or(shape, arms[d]);
                }
            }
            shapes[mask] = shape;
        }
        return shapes;
    }

    private static int connectionMask(BlockState state) {
        int mask = 0;
        for (int d = 0; d < 6; d++) {
            if (state.getValue(CONNECTIONS[d])) {
                mask |= 1 << d;
            }
        }
        return mask;
    }

    @Override
    protected MapCodec<UniversalPipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPES[connectionMask(state)];
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // --- Connections -----------------------------------------------------------

    /** True when the neighbour on {@code dir} is another pipe, a vanilla item container, or exposes an
     *  energy/fluid/gas storage through the cross-loader lookup seams. */
    public static boolean canConnect(Level level, BlockPos pos, Direction dir) {
        BlockPos np = pos.relative(dir);
        BlockEntity be = level.getBlockEntity(np);
        if (be instanceof UniversalPipeBlockEntity) {
            return true;
        }
        if (be instanceof Container) {
            return true;
        }
        Direction opposite = dir.getOpposite();
        return EnergyLookup.INSTANCE.find(level, np, opposite) != null
                || FluidLookup.INSTANCE.find(level, np, opposite) != null
                || GasLookup.INSTANCE.find(level, np, opposite) != null;
    }

    /** Recompute all six connection properties of {@code state} against the world. */
    public static BlockState withConnections(BlockState state, Level level,
            BlockPos pos) {
        for (Direction dir : Direction.values()) {
            state = NerospaceCommon.requireNonNull(
                    state.setValue(CONNECTIONS[dir.get3DDataValue()], canConnect(level, pos, dir)));
        }
        return state;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    // --- Block entity -----------------------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe) {
            if (player.isShiftKeyDown()) {
                pipe.uninstallUpgrades(); // sneak + empty hand: pop installed upgrades back out
                return InteractionResult.SUCCESS;
            }
            // Read out what is flowing through this segment (energy / fluid / gas / items).
            serverPlayer.sendSystemMessage(Component.literal("§eEnergy: " + pipe.getEnergy().getAmount() + " FE"));
            if (pipe.getFluidTank().getFluid() != Fluids.EMPTY) {
                serverPlayer.sendSystemMessage(Component.literal("§bFluid: " + pipe.getFluidTank().getAmount() + " mB"));
            }
            if (!pipe.getGas().getGas().isEmpty()) {
                serverPlayer.sendSystemMessage(Component.literal("§aGas: " + pipe.getGas().getAmount() + " mB"));
            }
            int items = 0;
            for (int i = 0; i < pipe.getContainerSize(); i++) {
                items += pipe.getItem(i).getCount();
            }
            if (items > 0) {
                serverPlayer.sendSystemMessage(Component.literal("§fItems in transit: " + items));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UniversalPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.UNIVERSAL_PIPE.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }
}
