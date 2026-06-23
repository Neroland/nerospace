package za.co.neroland.nerospace.world;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Ancient Ruin (ALIEN_VILLAGERS_DESIGN.md §5.2 / §7 — redesigned) — a derelict, half-buried alien
 * hall built from cracked alien brick with collapsed walls and a dead crystal core, holding a loot
 * vault of rare alien goods (and, with the boss available, a dangerous prize). Spaced + capped by
 * {@link StructureSpacing}.
 */
public class RuinFeature extends Feature<NoneFeatureConfiguration> {

    public RuinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        BlockPos o = ctx.origin();
        if (!StructureSpacing.shouldPlace(o, StructureSpacing.Roi.RUIN)) {
            return false;
        }
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        int baseY = o.getY() - 2; // sunken
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        // A large weathered hall.
        AlienBuild.tower(level, o.getX(), baseY, o.getZ(), 6, 6, true, rand, m);

        // Dead/awakening crystal core at the centre.
        BlockState core = ModBlocks.VILLAGE_CORE.get().defaultBlockState();
        m.set(o.getX(), baseY, o.getZ());
        level.setBlock(m, core, 2);

        // Loot vault: a chest of rare alien goods, offset from the core.
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
