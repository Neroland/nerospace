package za.co.neroland.nerospace.world;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
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
 * Ancient Ruin (ALIEN_VILLAGERS_DESIGN.md §5.2, Phase 7) — a derelict, partially-buried alien
 * megastructure: a sunken nerosteel hall with broken walls, a glowing core, and a loot vault of rare
 * alien goods. The "explore for a while" content; rarer than the hamlets.
 *
 * <p>Robust slice: a single bounded ruin built procedurally (no NBT). The bigger multi-level dungeons,
 * the lore/relic sites and the dedicated boss entity remain the next iteration of this phase.
 */
public class RuinFeature extends Feature<NoneFeatureConfiguration> {

    private static final int RADIUS = 6;  // 13x13 footprint
    private static final int HEIGHT = 5;
    private static final int SINK = 2;     // buried depth

    public RuinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource rand = ctx.random();
        BlockState wall = ModBlocks.NEROSTEEL_BLOCK.get().defaultBlockState();
        BlockState core = ModBlocks.VILLAGE_CORE.get().defaultBlockState();
        BlockState light = Blocks.GLOWSTONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        int baseY = origin.getY() - SINK;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                m.set(x, baseY - 1, z);
                level.setBlock(m, wall, 2); // floor
                boolean perimeter = Math.abs(dx) == RADIUS || Math.abs(dz) == RADIUS;
                for (int dy = 0; dy < HEIGHT; dy++) {
                    m.set(x, baseY + dy, z);
                    if (perimeter && rand.nextFloat() > 0.25F) {
                        level.setBlock(m, wall, 2); // ruined wall — ~25% gaps
                    } else {
                        level.setBlock(m, air, 2);
                    }
                }
            }
        }
        // Central glowing core.
        m.set(origin.getX(), baseY, origin.getZ());
        level.setBlock(m, core, 2);
        m.set(origin.getX(), baseY + HEIGHT, origin.getZ());
        level.setBlock(m, light, 2);

        // Loot vault: a chest of rare alien goods, offset from the core.
        BlockPos chestPos = new BlockPos(origin.getX() + 3, baseY, origin.getZ() + 3);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setItem(4, new ItemStack(ModItems.ALIEN_CORE.get(), 1));
            chest.setItem(6, new ItemStack(ModItems.ALIEN_TECH_SCRAP.get(), 2 + rand.nextInt(4)));
            chest.setItem(10, new ItemStack(ModItems.ALIEN_FRAGMENT.get(), 3 + rand.nextInt(5)));
            chest.setItem(13, new ItemStack(ModItems.NEROSIUM_INGOT.get(), 2 + rand.nextInt(4)));
            chest.setItem(22, new ItemStack(net.minecraft.world.item.Items.EMERALD, 4 + rand.nextInt(8)));
        }
        return true;
    }

}
