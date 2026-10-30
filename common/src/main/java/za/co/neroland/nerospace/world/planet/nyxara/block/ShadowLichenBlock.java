package za.co.neroland.nerospace.world.planet.nyxara.block;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreadeableBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.ChunkSpreadCounter;
import za.co.neroland.nerospace.block.rules.LichenRules;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.world.planet.PlanetWeather;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * Shadow Lichen (Nyxara): a glow-lichen-style multiface that spreads only in the dark and dies in flares.
 * On a random tick it spreads 1-in-10 to an adjacent face while the light at its cell is 3 or less and its chunk holds
 * fewer than 32 lichen blocks (config may lower, {@code blocks.shadowLichenCap}); during a Nyxara flare each random
 * tick withers one face 1-in-2 (the last face removes the block) and drops nothing. A torch stops it. Shears harvest
 * Night Fibre (loot table). Placed by worldgen and by players like glow lichen.
 */
public class ShadowLichenBlock extends MultifaceSpreadeableBlock {

    public static final MapCodec<ShadowLichenBlock> CODEC = BlockCodecs.simple(ShadowLichenBlock::new);

    private final MultifaceSpreader spreader = new MultifaceSpreader(this);

    public ShadowLichenBlock(Properties properties) {
        super(properties);
    }

    public MapCodec<ShadowLichenBlock> codec() {
        return CODEC;
    }

    @Override
    public MultifaceSpreader getSpreader() {
        return this.spreader;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean flare = PlanetWeather.event(level) == WeatherEvent.FLARE;
        if (flare) {
            if (LichenRules.withers(true, random.nextInt(LichenRules.WITHER_ONE_IN))) {
                wither(state, level, pos, random);
            }
            return;
        }
        int light = level.getMaxLocalRawBrightness(pos);
        if (light > LichenRules.MAX_LIGHT) {
            return;
        }
        int count = ChunkSpreadCounter.count(level, pos, this);
        if (LichenRules.canSpread(light, count, NerospaceConfig.shadowLichenCap(), false,
                random.nextInt(LichenRules.ONE_IN))) {
            this.spreader.spreadFromRandomFaceTowardRandomDirection(state, level, pos, random)
                    .ifPresent(spread -> ChunkSpreadCounter.increment(level, spread.pos(), this));
        }
    }

    /** Removes one random face; the last face removes the block. Drops nothing. */
    private void wither(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        List<Direction> faces = new ArrayList<>();
        for (Direction d : Direction.values()) {
            if (MultifaceBlock.hasFace(state, d)) {
                faces.add(d);
            }
        }
        if (faces.size() <= 1) {
            level.removeBlock(pos, false);
            return;
        }
        Direction gone = faces.get(random.nextInt(faces.size()));
        level.setBlock(pos, state.setValue(MultifaceBlock.getFaceProperty(gone), Boolean.FALSE), Block.UPDATE_ALL);
    }
}
