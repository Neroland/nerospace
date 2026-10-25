package za.co.neroland.nerospace.world.station.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * Telescope: the Observatory's instrument. The only block that reveals a locked planet's card early; its
 * reveal is computed by the Station Core from the sightline column above it ({@code ObservatoryRules}).
 * {@code DORMANT} is true while the station is not RESEARCH class (the module stays standing; nothing is revealed).
 */
public class TelescopeBlock extends Block {

    public static final MapCodec<TelescopeBlock> CODEC = BlockCodecs.simple(TelescopeBlock::new);

    public static final BooleanProperty DORMANT = BooleanProperty.create("dormant");

    @SuppressWarnings("this-escape")
    public TelescopeBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(DORMANT, Boolean.FALSE));
    }

    protected MapCodec<? extends TelescopeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DORMANT);
    }
}
