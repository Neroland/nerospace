package za.co.neroland.nerospace.world;

//? if <26.3 {
import com.mojang.serialization.Codec;
//?}

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
//? if <26.3 {
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
//?}

import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Ancient Ruin — a derelict, half-buried alien hall of cracked alien brick with collapsed walls and a
 * dead crystal core, holding a loot vault of rare alien goods. Spaced + capped by {@link StructureSpacing}.
 */
//? if >=26.3 {
/*public class RuinFeature implements Feature {
*///?} else {
public class RuinFeature extends Feature<NoneFeatureConfiguration> {
//?}

    //? if >=26.3 {
    /*// Minecraft 26.3+: features are data-driven values; this configuration-free type has one instance.
    public static final RuinFeature INSTANCE = new RuinFeature();
    public static final com.mojang.serialization.MapCodec<RuinFeature> CODEC = com.mojang.serialization.MapCodec.unit(INSTANCE);

    @Override
    public com.mojang.serialization.MapCodec<? extends Feature> codec() {
        return CODEC;
    }
    *///?}
    //? if <26.3 {
    public RuinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }
    //?}

    //? if >=26.3 {
    /*@Override
    public boolean place(WorldGenLevel level, net.minecraft.world.level.chunk.ChunkGenerator generator, RandomSource rand,
            BlockPos o) {
        return generate(level, rand, o);
    }
    *///?} else {
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        return generate(ctx.level(), ctx.random(), ctx.origin());
    }
    //?}

    /** Version-neutral body shared by both feature APIs. */
    private boolean generate(WorldGenLevel level, RandomSource rand, BlockPos o) {
        if (!StructureSpacing.shouldPlace(o, StructureSpacing.Roi.RUIN)) {
            return false;
        }
        int baseY = o.getY() - 2; // sunken
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        AlienBuild.tower(level, o.getX(), baseY, o.getZ(), 6, 6, true, rand, m);

        BlockState core = ModBlocks.VILLAGE_CORE.get().defaultBlockState();
        m.set(o.getX(), baseY, o.getZ());
        level.setBlock(m, core, 2);

        BlockPos chestPos = new BlockPos(o.getX() + 3, baseY, o.getZ() + 3);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setItem(4, new ItemStack(ModItems.ALIEN_CORE.get(), 1));
            chest.setItem(6, new ItemStack(ModItems.ALIEN_TECH_SCRAP.get(), 2 + rand.nextInt(4)));
            chest.setItem(10, new ItemStack(ModItems.ALIEN_FRAGMENT.get(), 3 + rand.nextInt(5)));
            chest.setItem(13, new ItemStack(ModItems.NEROSIUM_INGOT.get(), 2 + rand.nextInt(4)));
            chest.setItem(22, new ItemStack(Items.EMERALD, 4 + rand.nextInt(8)));
        }
        return true;
    }
}
