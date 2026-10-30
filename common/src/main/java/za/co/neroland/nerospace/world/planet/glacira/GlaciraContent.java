package za.co.neroland.nerospace.world.planet.glacira;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import za.co.neroland.nerospace.block.ClockLitBlock;
import za.co.neroland.nerospace.block.CrystalStageBlock;
import za.co.neroland.nerospace.block.MethaneIceBlock;
import za.co.neroland.nerospace.block.rules.AuroraRules;
import za.co.neroland.nerospace.block.rules.CryoIceRules;
import za.co.neroland.nerospace.block.rules.CrystalStageRules;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;
import za.co.neroland.nerospace.world.planet.glacira.block.AuroraStoneBlock;
import za.co.neroland.nerospace.world.planet.glacira.block.FrostCrystalClusterBlock;
import za.co.neroland.nerospace.world.planet.glacira.block.FrozenGeyserBlock;

/**
 * Every new Glacira block and item, registered through the shared providers. The 1.x Glacira ids (Glacite Ore,
 * Glacite Block, Glacite) stay where they are in {@code ModBlocks}/{@code ModItems}; this class only lists them in the
 * planets tab. Blocks reuse the shared behaviour bases: Packed Cryo-Ice is blue-ice-fast and never melts (no random
 * tick), Frost Crystal Cluster grows on diamond dust ({@link CrystalStageBlock}), Methane Ice explodes near a flame
 * ({@link MethaneIceBlock}), Permafrost resists hydration (tag {@code nerospace:hydration_immune}), Aurora Stone lights
 * at night ({@link ClockLitBlock}), the Frozen Geyser launches whoever stands on it. Class loading registers
 * everything; {@link #init()} only forces the load.
 */
public final class GlaciraContent {

    // --- Blocks (creative-tab order) ---------------------------------------------------------------------------
    public static final RegistryEntry<Block> PERMAFROST = ModBlocks.BLOCKS.register("permafrost",
            key -> new Block(props(key).mapColor(MapColor.COLOR_LIGHT_BLUE).strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));

    /** Friction 0.989 (blue ice); no random tick, so light never melts it. */
    public static final RegistryEntry<Block> PACKED_CRYO_ICE = ModBlocks.BLOCKS.register("packed_cryo_ice",
            key -> new Block(props(key).mapColor(MapColor.ICE).strength(2.8F, 2.8F)
                    .friction(CryoIceRules.friction()).sound(SoundType.GLASS)));

    public static final RegistryEntry<AuroraStoneBlock> AURORA_STONE = ModBlocks.BLOCKS.register("aurora_stone",
            key -> new AuroraStoneBlock(props(key).mapColor(MapColor.COLOR_CYAN).strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).randomTicks()
                    .lightLevel(s -> s.getValue(ClockLitBlock.LIT) ? AuroraRules.LIGHT : 0)));

    public static final RegistryEntry<FrozenGeyserBlock> FROZEN_GEYSER = ModBlocks.BLOCKS.register("frozen_geyser",
            key -> new FrozenGeyserBlock(props(key).mapColor(MapColor.COLOR_BLUE).strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).randomTicks()));

    public static final RegistryEntry<FrostCrystalClusterBlock> FROST_CRYSTAL_CLUSTER = ModBlocks.BLOCKS.register(
            "frost_crystal_cluster",
            key -> new FrostCrystalClusterBlock(props(key).mapColor(MapColor.ICE).strength(1.5F, 1.5F)
                    .sound(SoundType.AMETHYST_CLUSTER).noOcclusion()
                    .lightLevel(s -> CrystalStageRules.light(s.getValue(CrystalStageBlock.STAGE)))));

    public static final RegistryEntry<MethaneIceBlock> METHANE_ICE = ModBlocks.BLOCKS.register("methane_ice",
            key -> new MethaneIceBlock(props(key).mapColor(MapColor.COLOR_PURPLE).strength(3.0F, 1.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.GLASS)));

    // --- Block items ---------------------------------------------------------------------------------------
    public static final RegistryEntry<BlockItem> PERMAFROST_ITEM = blockItem("permafrost", PERMAFROST);
    public static final RegistryEntry<BlockItem> PACKED_CRYO_ICE_ITEM = blockItem("packed_cryo_ice", PACKED_CRYO_ICE);
    public static final RegistryEntry<BlockItem> AURORA_STONE_ITEM = blockItem("aurora_stone", AURORA_STONE);
    public static final RegistryEntry<BlockItem> FROZEN_GEYSER_ITEM = blockItem("frozen_geyser", FROZEN_GEYSER);
    public static final RegistryEntry<BlockItem> FROST_CRYSTAL_CLUSTER_ITEM = blockItem("frost_crystal_cluster",
            FROST_CRYSTAL_CLUSTER);
    public static final RegistryEntry<BlockItem> METHANE_ICE_ITEM = blockItem("methane_ice", METHANE_ICE);

    // --- Items ---------------------------------------------------------------------------------------------
    public static final RegistryEntry<Item> FROST_SHARD = item("frost_shard");
    /** Methane, the combustion fuel. */
    public static final RegistryEntry<Item> METHANE_CRYSTAL = item("methane_crystal");

    private GlaciraContent() {
    }

    /** Forces class loading (and so registration). Called once from the shared registry init. */
    public static void init() {
    }

    /**
     * The Glacira run of the planets tab, in order: Permafrost, Packed Cryo-Ice, Aurora Stone, Frozen Geyser,
     * Frost Crystal Cluster, Methane Ice, the existing Glacite Ore and Glacite Block, then Frost Shard, Methane
     * Crystal and the existing Glacite.
     */
    public static List<Supplier<? extends ItemLike>> creativeContents() {
        List<Supplier<? extends ItemLike>> out = new ArrayList<>();
        out.add(PERMAFROST_ITEM);
        out.add(PACKED_CRYO_ICE_ITEM);
        out.add(AURORA_STONE_ITEM);
        out.add(FROZEN_GEYSER_ITEM);
        out.add(FROST_CRYSTAL_CLUSTER_ITEM);
        out.add(METHANE_ICE_ITEM);
        out.add(ModItems.GLACITE_ORE_ITEM);
        out.add(ModItems.GLACITE_BLOCK_ITEM);
        out.add(FROST_SHARD);
        out.add(METHANE_CRYSTAL);
        out.add(ModItems.GLACITE);
        return Collections.unmodifiableList(out);
    }

    // --- helpers -------------------------------------------------------------------------------------------

    private static BlockBehaviour.Properties props(ResourceKey<Block> key) {
        return BlockBehaviour.Properties.of().setId(key);
    }

    private static RegistryEntry<BlockItem> blockItem(String name, RegistryEntry<? extends Block> block) {
        // 26.x block items resolve their name from item.nerospace.<id>; the lang fragment mirrors every block key.
        return ModItems.ITEMS.register(name, key -> new BlockItem(block.get(), new Item.Properties().setId(key)));
    }

    private static RegistryEntry<Item> item(String name) {
        return ModItems.ITEMS.register(name, key -> new Item(new Item.Properties().setId(key)));
    }
}
