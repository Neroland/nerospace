package za.co.neroland.nerospace.registry;

import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/**
 * Item registrations shared by both loaders, plus the creative-tab grouping the
 * loader entry points consume (tab placement defined once, in common).
 */
public final class ModItems {

    public static final RegistrationProvider<Item> ITEMS =
            RegistrationProvider.get(Registries.ITEM, NerospaceCommon.MOD_ID);

    // Ore / material block items
    public static final RegistryEntry<BlockItem> NEROSIUM_ORE_ITEM = blockItem("nerosium_ore", ModBlocks.NEROSIUM_ORE);
    public static final RegistryEntry<BlockItem> DEEPSLATE_NEROSIUM_ORE_ITEM = blockItem("deepslate_nerosium_ore", ModBlocks.DEEPSLATE_NEROSIUM_ORE);
    public static final RegistryEntry<BlockItem> NEROSIUM_BLOCK_ITEM = blockItem("nerosium_block", ModBlocks.NEROSIUM_BLOCK);
    public static final RegistryEntry<BlockItem> RAW_NEROSIUM_BLOCK_ITEM = blockItem("raw_nerosium_block", ModBlocks.RAW_NEROSIUM_BLOCK);
    public static final RegistryEntry<BlockItem> NEROSTEEL_ORE_ITEM = blockItem("nerosteel_ore", ModBlocks.NEROSTEEL_ORE);
    public static final RegistryEntry<BlockItem> XERTZ_QUARTZ_ORE_ITEM = blockItem("xertz_quartz_ore", ModBlocks.XERTZ_QUARTZ_ORE);
    public static final RegistryEntry<BlockItem> NEROSTEEL_BLOCK_ITEM = blockItem("nerosteel_block", ModBlocks.NEROSTEEL_BLOCK);
    public static final RegistryEntry<BlockItem> CINDRITE_ORE_ITEM = blockItem("cindrite_ore", ModBlocks.CINDRITE_ORE);
    public static final RegistryEntry<BlockItem> CINDRITE_BLOCK_ITEM = blockItem("cindrite_block", ModBlocks.CINDRITE_BLOCK);
    public static final RegistryEntry<BlockItem> GLACITE_ORE_ITEM = blockItem("glacite_ore", ModBlocks.GLACITE_ORE);
    public static final RegistryEntry<BlockItem> GLACITE_BLOCK_ITEM = blockItem("glacite_block", ModBlocks.GLACITE_BLOCK);

    // Station / alien decorative / meteor block items
    public static final RegistryEntry<BlockItem> STATION_FLOOR_ITEM = blockItem("station_floor", ModBlocks.STATION_FLOOR);
    public static final RegistryEntry<BlockItem> STATION_WALL_ITEM = blockItem("station_wall", ModBlocks.STATION_WALL);
    public static final RegistryEntry<BlockItem> ALIEN_BRICKS_ITEM = blockItem("alien_bricks", ModBlocks.ALIEN_BRICKS);
    public static final RegistryEntry<BlockItem> CRACKED_ALIEN_BRICKS_ITEM = blockItem("cracked_alien_bricks", ModBlocks.CRACKED_ALIEN_BRICKS);
    public static final RegistryEntry<BlockItem> ALIEN_TILE_ITEM = blockItem("alien_tile", ModBlocks.ALIEN_TILE);
    public static final RegistryEntry<BlockItem> ALIEN_PILLAR_ITEM = blockItem("alien_pillar", ModBlocks.ALIEN_PILLAR);
    public static final RegistryEntry<BlockItem> ALIEN_LAMP_ITEM = blockItem("alien_lamp", ModBlocks.ALIEN_LAMP);
    public static final RegistryEntry<BlockItem> ALIEN_CRYSTAL_BLOCK_ITEM = blockItem("alien_crystal_block", ModBlocks.ALIEN_CRYSTAL_BLOCK);
    public static final RegistryEntry<BlockItem> METEOR_ROCK_ITEM = blockItem("meteor_rock", ModBlocks.METEOR_ROCK);

    // Materials
    public static final RegistryEntry<Item> RAW_NEROSIUM = item("raw_nerosium");
    public static final RegistryEntry<Item> NEROSIUM_INGOT = item("nerosium_ingot");
    public static final RegistryEntry<Item> RAW_NEROSTEEL = item("raw_nerosteel");
    public static final RegistryEntry<Item> NEROSTEEL_INGOT = item("nerosteel_ingot");
    public static final RegistryEntry<Item> XERTZ_QUARTZ = item("xertz_quartz");
    public static final RegistryEntry<Item> CINDRITE = item("cindrite");
    public static final RegistryEntry<Item> GLACITE = item("glacite");

    private static RegistryEntry<Item> item(String name) {
        return ITEMS.register(name, key -> new Item(new Item.Properties().setId(key)));
    }

    private static RegistryEntry<BlockItem> blockItem(String name, RegistryEntry<? extends Block> block) {
        return ITEMS.register(name, key -> new BlockItem(block.get(), new Item.Properties().setId(key)));
    }

    /** Items grouped by the vanilla creative tab they should appear in. */
    public static Map<ResourceKey<CreativeModeTab>, List<ItemLike>> creativeTabItems() {
        return Map.of(
                CreativeModeTabs.NATURAL_BLOCKS,
                List.<ItemLike>of(
                        NEROSIUM_ORE_ITEM.get(), DEEPSLATE_NEROSIUM_ORE_ITEM.get(),
                        NEROSTEEL_ORE_ITEM.get(), XERTZ_QUARTZ_ORE_ITEM.get(),
                        CINDRITE_ORE_ITEM.get(), GLACITE_ORE_ITEM.get(), METEOR_ROCK_ITEM.get()),
                CreativeModeTabs.BUILDING_BLOCKS,
                List.<ItemLike>of(
                        NEROSIUM_BLOCK_ITEM.get(), RAW_NEROSIUM_BLOCK_ITEM.get(),
                        NEROSTEEL_BLOCK_ITEM.get(), CINDRITE_BLOCK_ITEM.get(), GLACITE_BLOCK_ITEM.get(),
                        STATION_FLOOR_ITEM.get(), STATION_WALL_ITEM.get(),
                        ALIEN_BRICKS_ITEM.get(), CRACKED_ALIEN_BRICKS_ITEM.get(), ALIEN_TILE_ITEM.get(),
                        ALIEN_PILLAR_ITEM.get(), ALIEN_LAMP_ITEM.get(), ALIEN_CRYSTAL_BLOCK_ITEM.get()),
                CreativeModeTabs.INGREDIENTS,
                List.<ItemLike>of(
                        RAW_NEROSIUM.get(), NEROSIUM_INGOT.get(),
                        RAW_NEROSTEEL.get(), NEROSTEEL_INGOT.get(),
                        XERTZ_QUARTZ.get(), CINDRITE.get(), GLACITE.get()));
    }

    private ModItems() {
    }

    public static void init() {
    }
}
