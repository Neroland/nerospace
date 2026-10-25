package za.co.neroland.nerospace.block;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.Caps;
import za.co.neroland.nerospace.block.rules.CrystalStageRules;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.world.planet.WeatherEffects;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * In-place staged growth (Frost Crystal Cluster, 002 §1.4): when the event ends, every loaded sky-exposed cluster
 * below stage 3 gains one stage (≤ 64 per chunk per event, config may lower). Never spreads. Register with
 * {@code lightLevel(s -> CrystalStageRules.light(s.getValue(STAGE)))} and
 * {@code CrystalStageBlock.register(WeatherEvent.DIAMOND_DUST, ModBlocks.FROST_CRYSTAL_CLUSTER::get)}.
 */
public class CrystalStageBlock extends Block {

    public static final MapCodec<CrystalStageBlock> CODEC = BlockCodecs.simple(CrystalStageBlock::new);

    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, CrystalStageRules.MAX_STAGE);

    @SuppressWarnings("this-escape")
    public CrystalStageBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    protected MapCodec<? extends CrystalStageBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    /** Queues the event-end growth (drained over ticks); returns the number of chunks queued. */
    public int growAll(ServerLevel level) {
        return WeatherScan.forEachLoadedBlock(level, s -> s.is(this) && s.getValue(STAGE) < CrystalStageRules.MAX_STAGE,
                Caps.lower(CrystalStageRules.PER_CHUNK_CAP, NerospaceConfig.frostCrystalCap()), (pos, s) -> {
                    int next = CrystalStageRules.nextStage(s.getValue(STAGE), level.canSeeSky(pos.above()), true);
                    return next != s.getValue(STAGE)
                            && level.setBlock(pos.immutable(), s.setValue(STAGE, next), Block.UPDATE_CLIENTS);
                });
    }

    public static void register(WeatherEvent event, Supplier<? extends CrystalStageBlock> block) {
        WeatherEffects.Registry.register(event, new WeatherEffects() {
            @Override
            public void onEnd(ServerLevel level, WeatherEvent ended) {
                block.get().growAll(level);
            }
        });
    }
}
