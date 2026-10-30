package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.RootRules;
import za.co.neroland.nerospace.block.rules.SporeContactRules;
import za.co.neroland.nerospace.block.rules.SporeSpreadRules;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.registry.ModTags;
import za.co.neroland.nerospace.world.planet.PlanetWeather;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * Carpet that spreads over a rootable substrate with a per-chunk cap (Spore Moss, 002 §1.5): 1-in-8 per random tick
 * onto an adjacent empty {@code nerospace:xertz_rootable} top, ×3 during a spore bloom, cap 48 per chunk (config may
 * lower). Touching it (entity box in the moss cell) without a sealed helmet gives that entity Nausea I 5 s (point
 * effect). Stage 3 registers it with {@code randomTicks()} and its light level.
 */
public class CappedSpreadBlock extends Block {

    public static final MapCodec<CappedSpreadBlock> CODEC = BlockCodecs.simple(CappedSpreadBlock::new);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public CappedSpreadBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends CappedSpreadBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return RootRules.canRoot(level.getBlockState(pos.below()).is(ModTags.Blocks.XERTZ_ROOTABLE));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Tame inside the terraformed xertz garden (D7): moss seeded by stage 3 stays where it was planted.
        if (level.getBiome(pos).is(za.co.neroland.nerospace.world.planet.greenxertz.GreenxertzTerraform.TABLE.living())) {
            return;
        }
        Direction dir = Direction.from2DDataValue(random.nextInt(4));
        BlockPos target = pos.relative(dir);
        boolean onSoil = level.getBlockState(target).isAir()
                && level.getBlockState(target.below()).is(ModTags.Blocks.XERTZ_ROOTABLE);
        if (!onSoil) {
            return;
        }
        boolean bloom = PlanetWeather.event(level) == WeatherEvent.SPORE_BLOOM;
        int count = ChunkSpreadCounter.count(level, target, this);
        if (SporeSpreadRules.canSpread(true, count, NerospaceConfig.sporeMossCap(), bloom,
                random.nextInt(SporeSpreadRules.ONE_IN))) {
            level.setBlockAndUpdate(target, defaultBlockState());
            ChunkSpreadCounter.increment(level, target, this);
        }
    }

    /**
     * Point nausea on contact: {@code stepOn} would hit the soil under a 1/16 carpet, so the gate runs when an entity's
     * box overlaps the moss cell. Throttled by the effect itself (not re-applied while it runs).
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effects, boolean isPrecise) {
        if (!level.isClientSide() && entity instanceof LivingEntity living
                && SporeContactRules.nausea(BlockSuits.sealedHelmet(entity))
                && !living.hasEffect(MobEffects.NAUSEA)) {
            living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, SporeContactRules.MOSS_NAUSEA_TICKS, 0));
        }
        super.entityInside(state, level, pos, entity, effects, isPrecise);
    }
}
