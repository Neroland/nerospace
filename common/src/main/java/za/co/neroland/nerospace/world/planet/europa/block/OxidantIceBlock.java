package za.co.neroland.nerospace.world.planet.europa.block;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.EuropaDropRules;
import za.co.neroland.nerospace.block.rules.RadiolysisRules;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.PlanetWeather;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.RadiationRules;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * Oxidant Ice: surface ice that radiation loads with oxidants. Under open sky and a radiation level of 2 or more it
 * gains one {@code CHARGE} on a random tick, up to 3; mined it gives one crystal plus one per charge. Silk Touch lifts
 * the block, which places at charge 0. Worldgen places it fully charged.
 *
 * <p>Cost: only ice below charge 3 takes random ticks, and one tick is a sky check, a biome read, a weather-state
 * read and at most one state write.</p>
 */
public class OxidantIceBlock extends Block {

    public static final MapCodec<OxidantIceBlock> CODEC = BlockCodecs.simple(OxidantIceBlock::new);

    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, RadiolysisRules.MAX_CHARGE);

    @SuppressWarnings("this-escape")
    public OxidantIceBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(CHARGE, 0));
    }

    protected MapCodec<? extends OxidantIceBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return RadiolysisRules.ticks(state.getValue(CHARGE));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int charge = state.getValue(CHARGE);
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null || !RadiolysisRules.ticks(charge)) {
            return;
        }
        String biome = level.getBiome(pos).unwrapKey().map(k -> k.identifier().toString()).orElse("");
        int radiation = RadiolysisRules.level(RadiationRules.base(planet.traits().radiation(), biome),
                PlanetWeather.event(level) == WeatherEvent.FLARE);
        if (RadiolysisRules.gains(level.canSeeSky(pos.above()), radiation, charge)) {
            level.setBlock(pos, state.setValue(CHARGE, charge + 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> base = super.getDrops(state, params);
        boolean silk = EuropaDrops.silk(params, state, base);
        BlockPos pos = EuropaDrops.origin(params);
        if (silk || pos == null || base.isEmpty()) {
            return base;
        }
        return EuropaDrops.apply(EuropaDropRules.Kind.OXIDANT_ICE, base, false, false, state.getValue(CHARGE));
    }
}
