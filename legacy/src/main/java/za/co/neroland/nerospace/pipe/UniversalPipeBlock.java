package za.co.neroland.nerospace.pipe;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.gas.GasCapability;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * The Universal Pipe: a connection-aware transmitter that joins a {@link PipeNetwork} with its
 * neighbours and moves all four resource layers across it. Six boolean blockstate properties drive
 * the translucent tube model (core + arms) and the voxel shape; they connect to adjacent pipes and to
 * any block exposing an energy/fluid/item/gas capability. Network membership is rebuilt lazily, so
 * placing or breaking a pipe (merging/splitting networks) needs no explicit hooks.
 *
 * <p>Right-click (empty hand) prints what the segment is carrying; the Configurator sets per-face,
 * per-layer input/output modes.</p>
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
    public static final BooleanProperty[] CONNECTIONS = {DOWN, UP, NORTH, SOUTH, WEST, EAST};

    /** Voxel shapes indexed by the 6-bit connection mask (bit = direction 3D data value). */
    private static final VoxelShape[] SHAPES = buildShapes();

    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public UniversalPipeBlock(Properties properties) {
        super(properties);
        BlockState base = this.stateDefinition.any();
        for (BooleanProperty prop : CONNECTIONS) {
            base = base.setValue(prop, false);
        }
        registerDefaultState(base);
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape core = Block.box(4, 4, 4, 12, 12, 12);
        VoxelShape[] arms = {
                Block.box(4, 0, 4, 12, 4, 12),   // down
                Block.box(4, 12, 4, 12, 16, 12), // up
                Block.box(4, 4, 0, 12, 12, 4),   // north
                Block.box(4, 4, 12, 12, 12, 16), // south
                Block.box(0, 4, 4, 4, 12, 12),   // west
                Block.box(12, 4, 4, 16, 12, 12), // east
        };
        VoxelShape[] shapes = new VoxelShape[64];
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[connectionMask(state)];
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // --- Connections -----------------------------------------------------------

    /** True when the neighbour on {@code dir} is another pipe or exposes any transferable capability. */
    public static boolean canConnect(Level level, BlockPos pos, Direction dir) {
        BlockPos np = pos.relative(dir);
        if (level.getBlockEntity(np) instanceof UniversalPipeBlockEntity) {
            return true;
        }
        Direction opposite = dir.getOpposite();
        return Capabilities.Energy.BLOCK.getCapability(level, np, null, null, opposite) != null
                || Capabilities.Fluid.BLOCK.getCapability(level, np, null, null, opposite) != null
                || Capabilities.Item.BLOCK.getCapability(level, np, null, null, opposite) != null
                || GasCapability.BLOCK.getCapability(level, np, null, null, opposite) != null;
    }

    private static BlockState computeConnections(BlockState state, Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTIONS[dir.get3DDataValue()], canConnect(level, pos, dir));
        }
        return state;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computeConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    /** Immediate pipe-to-pipe connection updates (capability neighbours go through neighborChanged). */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction dir, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (neighborState.getBlock() instanceof UniversalPipeBlock) {
            return state.setValue(CONNECTIONS[dir.get3DDataValue()], true);
        }
        return state;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (!level.isClientSide()) {
            BlockState updated = computeConnections(state, level, pos);
            if (updated != state) {
                level.setBlockAndUpdate(pos, updated);
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            BlockState updated = computeConnections(state, level, pos);
            if (updated != state) {
                level.setBlockAndUpdate(pos, updated);
            }
        }
    }

    // --- Block entity -----------------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UniversalPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.UNIVERSAL_PIPE.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe) {
            // Sneak + empty hand: pop installed upgrades back out.
            if (player.isShiftKeyDown()) {
                int popped = pipe.uninstallUpgrades();
                serverPlayer.sendSystemMessage(Component.translatable(
                        popped > 0 ? "block.nerospace.universal_pipe.upgrades_removed"
                                : "block.nerospace.universal_pipe.no_upgrades"));
                return InteractionResult.SUCCESS;
            }
            serverPlayer.sendSystemMessage(Component.translatable(
                    "block.nerospace.universal_pipe.energy", pipe.getEnergyHandler().getAmountAsInt()));
            if (!pipe.fluid().resource().isEmpty()) {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "block.nerospace.universal_pipe.fluid", pipe.fluid().amount(),
                        pipe.fluid().resource().getFluid().getFluidType().getDescription()));
            }
            if (!pipe.gas().resource().isEmpty()) {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "block.nerospace.universal_pipe.gas", pipe.gas().amount(),
                        pipe.gas().resource().label()));
            }
            if (!pipe.items().isEmpty()) {
                int count = 0;
                for (var item : pipe.items()) {
                    count += item.amount();
                }
                serverPlayer.sendSystemMessage(Component.translatable(
                        "block.nerospace.universal_pipe.items", count));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
