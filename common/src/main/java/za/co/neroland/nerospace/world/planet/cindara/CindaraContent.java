package za.co.neroland.nerospace.world.planet.cindara;

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

import za.co.neroland.nerospace.block.IgnitingCrustBlock;
import za.co.neroland.nerospace.block.QuenchableHazardBlock;
import za.co.neroland.nerospace.block.SoftStoneBlock;
import za.co.neroland.nerospace.block.WeatherLayerBlock;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;
import za.co.neroland.nerospace.world.planet.cindara.block.MagmaGlassBlock;

/**
 * Every new Cindara block and item, registered through the shared providers. The 1.6.0 Cindara ids (Cindrite Ore,
 * Cindrite Block, Cindrite) stay in {@link ModBlocks}/{@link ModItems}; they only appear here in the tab order.
 * Behaviour comes from the shared bases: Ashstone mines instantly with any pickaxe ({@link SoftStoneBlock}),
 * Ember-Cracked Ashstone burns bare boots and quenches to Ashstone next to water ({@link QuenchableHazardBlock}),
 * Sulfur Crust sets its flammable neighbours alight when fire touches it and burns to Ashstone
 * ({@link IgnitingCrustBlock}), Ash Layer piles up during Ash Fall ({@link WeatherLayerBlock}, hooked by
 * {@link CindaraWeather}), Magma Glass shrugs off lava ({@link MagmaGlassBlock}) and Basalt Spire is never struck by an
 * ember strike ({@code #nerospace:lightning_safe}, {@link EmberStrike}). Class loading registers everything;
 * {@link #init()} only forces the load.
 */
public final class CindaraContent {

    // --- Blocks (creative-tab order) ---------------------------------------------------------------------------
    public static final RegistryEntry<SoftStoneBlock> ASHSTONE = ModBlocks.BLOCKS.register("ashstone",
            key -> new SoftStoneBlock(props(key).mapColor(MapColor.TERRACOTTA_BLACK).strength(1.2F, 3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE)));

    public static final RegistryEntry<QuenchableHazardBlock> EMBER_CRACKED_ASHSTONE = ModBlocks.BLOCKS.register(
            "ember_cracked_ashstone",
            key -> new QuenchableHazardBlock(props(key).mapColor(MapColor.TERRACOTTA_BLACK).strength(1.5F, 3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).lightLevel(s -> 3),
                    () -> ASHSTONE.get().defaultBlockState()));

    public static final RegistryEntry<Block> BASALT_SPIRE = ModBlocks.BLOCKS.register("basalt_spire",
            key -> new Block(props(key).mapColor(MapColor.COLOR_BLACK).strength(3.0F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));

    public static final RegistryEntry<MagmaGlassBlock> MAGMA_GLASS = ModBlocks.BLOCKS.register("magma_glass",
            key -> new MagmaGlassBlock(props(key).mapColor(MapColor.COLOR_ORANGE).strength(0.4F, 1200.0F)
                    .sound(SoundType.GLASS).lightLevel(s -> 7).noOcclusion()
                    .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)));

    public static final RegistryEntry<WeatherLayerBlock> ASH_LAYER = ModBlocks.BLOCKS.register("ash_layer",
            key -> new WeatherLayerBlock(props(key).mapColor(MapColor.TERRACOTTA_GRAY).replaceable().forceSolidOff()
                    .randomTicks().strength(0.1F).requiresCorrectToolForDrops().sound(SoundType.SNOW)));

    public static final RegistryEntry<IgnitingCrustBlock> SULFUR_CRUST = ModBlocks.BLOCKS.register("sulfur_crust",
            key -> new IgnitingCrustBlock(props(key).mapColor(MapColor.COLOR_YELLOW).strength(0.6F, 0.6F)
                    .sound(SoundType.SAND).ignitedByLava(), () -> ASHSTONE.get().defaultBlockState()));

    // --- Block items ---------------------------------------------------------------------------------------
    public static final RegistryEntry<BlockItem> ASHSTONE_ITEM = blockItem("ashstone", ASHSTONE);
    public static final RegistryEntry<BlockItem> EMBER_CRACKED_ASHSTONE_ITEM =
            blockItem("ember_cracked_ashstone", EMBER_CRACKED_ASHSTONE);
    public static final RegistryEntry<BlockItem> BASALT_SPIRE_ITEM = blockItem("basalt_spire", BASALT_SPIRE);
    public static final RegistryEntry<BlockItem> MAGMA_GLASS_ITEM = blockItem("magma_glass", MAGMA_GLASS);
    public static final RegistryEntry<BlockItem> ASH_LAYER_ITEM = blockItem("ash_layer", ASH_LAYER);
    public static final RegistryEntry<BlockItem> SULFUR_CRUST_ITEM = blockItem("sulfur_crust", SULFUR_CRUST);

    // --- Items ---------------------------------------------------------------------------------------------
    /** Dropped by Ashstone and Ash Layer; crafts back into both. */
    public static final RegistryEntry<Item> ASH_DUST = item("ash_dust");
    /** Explosive / battery ingredient from Sulfur Crust. */
    public static final RegistryEntry<Item> SULFUR = item("sulfur");

    private CindaraContent() {
    }

    /** Forces class loading (and so registration). Called once from the shared registry init. */
    public static void init() {
    }

    /**
     * The Cindara run of the planets tab, in tab order: ground, stone, features, ores (the existing Cindrite Ore
     * and Block right after the new stone group), then items.
     */
    public static List<Supplier<? extends ItemLike>> creativeContents() {
        return List.of(ASHSTONE_ITEM, EMBER_CRACKED_ASHSTONE_ITEM, BASALT_SPIRE_ITEM, MAGMA_GLASS_ITEM, ASH_LAYER_ITEM,
                SULFUR_CRUST_ITEM, ModItems.CINDRITE_ORE_ITEM, ModItems.CINDRITE_BLOCK_ITEM, ASH_DUST, SULFUR,
                ModItems.CINDRITE);
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
