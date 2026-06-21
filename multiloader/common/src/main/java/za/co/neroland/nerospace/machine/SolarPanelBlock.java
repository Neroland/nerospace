package za.co.neroland.nerospace.machine;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * A solar panel that pools with adjacent same-tier panels into a {@link SolarArray}. Each tier is its
 * own registered block with its own output/buffer; energy is exposed on every side (output ports), so
 * any face feeds a pipe or machine from the shared array pool. GUI-less; emits a comparator signal
 * proportional to its stored energy.
 *
 * <p>Cross-loader port: tier-aware (the standalone single-tier block is generalised). The N×N
 * multiblock footprint + the tilting sun-tracking deck renderer are a deferred enhancement — every
 * panel is a 1×1 block here.</p>
 */
public class SolarPanelBlock extends BaseEntityBlock {

    public static final MapCodec<SolarPanelBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    SolarTier.CODEC.fieldOf("tier").forGetter(SolarPanelBlock::tier),
                    propertiesCodec()
            ).apply(instance, SolarPanelBlock::new));

    private final SolarTier tier;

    public SolarPanelBlock(SolarTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public SolarTier tier() {
        return this.tier;
    }

    @Override
    protected MapCodec<SolarPanelBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarPanelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.SOLAR_PANEL.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof SolarPanelBlockEntity panel ? panel.comparatorSignal() : 0;
    }
}
