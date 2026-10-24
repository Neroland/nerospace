package za.co.neroland.nerospace.block;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.WarmthRules;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.world.planet.WeatherEffects;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.WeatherMachine;

/**
 * Weather-gated state (Dawnband Stone, 002 §1.6): {@code FLARING} is set on every loaded block when the event starts
 * and cleared when it ends (one {@link WeatherScan} per transition, cap 256 per chunk, config may lower). Register
 * with {@code lightLevel(s -> WarmthRules.light(s.getValue(FLARING)))} and hook it with
 * {@code WeatherFlipBlock.register(WeatherEvent.FLARE, ModBlocks.DAWNBAND_STONE::get)}. The hazard system asks
 * {@link WarmthRules#shielded} for the COLD shield.
 */
public class WeatherFlipBlock extends Block {

    public static final MapCodec<WeatherFlipBlock> CODEC = BlockCodecs.simple(WeatherFlipBlock::new);

    public static final BooleanProperty FLARING = BooleanProperty.create("flaring");

    @SuppressWarnings("this-escape")
    public WeatherFlipBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FLARING, Boolean.FALSE));
    }

    protected MapCodec<? extends WeatherFlipBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLARING);
    }

    /** Flip every loaded block of this kind to {@code on}; returns the number changed. */
    public int flipAll(ServerLevel level, boolean on) {
        return WeatherScan.forEachLoadedBlock(level, s -> s.is(this) && s.getValue(FLARING) != on,
                NerospaceConfig.dawnbandCap(),
                (pos, s) -> level.setBlock(pos.immutable(), s.setValue(FLARING, on), Block.UPDATE_CLIENTS));
    }

    public static void register(WeatherEvent event, Supplier<? extends WeatherFlipBlock> block) {
        WeatherEffects.Registry.register(event, new WeatherEffects() {
            @Override
            public void onStart(ServerLevel level, WeatherMachine weather) {
                block.get().flipAll(level, true);
            }

            @Override
            public void onEnd(ServerLevel level, WeatherEvent ended) {
                block.get().flipAll(level, false);
            }
        });
    }
}
