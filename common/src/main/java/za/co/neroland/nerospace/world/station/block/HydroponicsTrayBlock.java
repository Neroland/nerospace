package za.co.neroland.nerospace.world.station.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * Hydroponics Tray: farmland-equivalent (in {@code minecraft:supports_crops} and
 * {@code minecraft:grows_crops}) that is always fully moist (the farmland {@code moisture} property, fixed at 7) and
 * needs no water. A crop on it grows only with a Hull Window within three blocks above the tray and inside an oxygen
 * field ({@code HydroRules}); the gate and the INDUSTRIAL class skip are applied by {@code CropHydroponicsMixin}.
 */
public class HydroponicsTrayBlock extends Block {

    public static final MapCodec<HydroponicsTrayBlock> CODEC = BlockCodecs.simple(HydroponicsTrayBlock::new);

    @SuppressWarnings("this-escape")
    public HydroponicsTrayBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FarmlandBlock.MOISTURE, 7));
    }

    protected MapCodec<? extends HydroponicsTrayBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FarmlandBlock.MOISTURE);
    }
}
