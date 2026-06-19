package za.co.neroland.nerospace.registry;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/**
 * Item registrations shared by both loaders, plus the creative-tab grouping the
 * loader entry points consume. Tools/armor use vanilla {@code Item.Properties}
 * delegates ({@code pickaxe(...)}, {@code humanoidArmor(...)}) — confirmed
 * present on the vanilla classpath, so they compile on both loaders.
 */
public final class ModItems {

    public static final RegistrationProvider<Item> ITEMS =
            RegistrationProvider.get(Registries.ITEM, NerospaceCommon.MOD_ID);

    // --- Block items --------------------------------------------------------
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
    public static final RegistryEntry<BlockItem> STATION_FLOOR_ITEM = blockItem("station_floor", ModBlocks.STATION_FLOOR);
    public static final RegistryEntry<BlockItem> STATION_WALL_ITEM = blockItem("station_wall", ModBlocks.STATION_WALL);
    public static final RegistryEntry<BlockItem> ALIEN_BRICKS_ITEM = blockItem("alien_bricks", ModBlocks.ALIEN_BRICKS);
    public static final RegistryEntry<BlockItem> CRACKED_ALIEN_BRICKS_ITEM = blockItem("cracked_alien_bricks", ModBlocks.CRACKED_ALIEN_BRICKS);
    public static final RegistryEntry<BlockItem> ALIEN_TILE_ITEM = blockItem("alien_tile", ModBlocks.ALIEN_TILE);
    public static final RegistryEntry<BlockItem> ALIEN_PILLAR_ITEM = blockItem("alien_pillar", ModBlocks.ALIEN_PILLAR);
    public static final RegistryEntry<BlockItem> ALIEN_LAMP_ITEM = blockItem("alien_lamp", ModBlocks.ALIEN_LAMP);
    public static final RegistryEntry<BlockItem> ALIEN_CRYSTAL_BLOCK_ITEM = blockItem("alien_crystal_block", ModBlocks.ALIEN_CRYSTAL_BLOCK);
    public static final RegistryEntry<BlockItem> METEOR_ROCK_ITEM = blockItem("meteor_rock", ModBlocks.METEOR_ROCK);
    public static final RegistryEntry<BlockItem> ITEM_STORE_ITEM = blockItem("item_store", ModBlocks.ITEM_STORE);

    // --- Materials ----------------------------------------------------------
    public static final RegistryEntry<Item> RAW_NEROSIUM = item("raw_nerosium");
    public static final RegistryEntry<Item> NEROSIUM_INGOT = item("nerosium_ingot");
    public static final RegistryEntry<Item> RAW_NEROSTEEL = item("raw_nerosteel");
    public static final RegistryEntry<Item> NEROSTEEL_INGOT = item("nerosteel_ingot");
    public static final RegistryEntry<Item> XERTZ_QUARTZ = item("xertz_quartz");
    public static final RegistryEntry<Item> CINDRITE = item("cindrite");
    public static final RegistryEntry<Item> GLACITE = item("glacite");
    public static final RegistryEntry<Item> NEROSIUM_DUST = item("nerosium_dust");
    public static final RegistryEntry<Item> ALIEN_FRAGMENT = item("alien_fragment");
    public static final RegistryEntry<Item> ALIEN_TECH_SCRAP = item("alien_tech_scrap");
    public static final RegistryEntry<Item> ALIEN_CORE = item("alien_core");
    public static final RegistryEntry<Item> ROCKET_FUEL_CANISTER = item("rocket_fuel_canister");
    public static final RegistryEntry<Item> FRAME_CASING = item("frame_casing");
    public static final RegistryEntry<Item> GRAV_STRIDERS = item("grav_striders");
    public static final RegistryEntry<Item> DRIFT_FLEECE = item("drift_fleece");

    // --- Tool + armor materials --------------------------------------------
    public static final ToolMaterial NEROSIUM_TOOL_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL, 350, 7.0F, 2.5F, 15, cTag("ingots/nerosium"));

    public static final ResourceKey<EquipmentAsset> OXYGEN_SUIT_ASSET = equipAsset("oxygen_suit");
    public static final ResourceKey<EquipmentAsset> OXYGEN_SUIT_T2_ASSET = equipAsset("oxygen_suit_t2");
    public static final ResourceKey<EquipmentAsset> OXYGEN_SUIT_HEAT_ASSET = equipAsset("oxygen_suit_heat");
    public static final ResourceKey<EquipmentAsset> OXYGEN_SUIT_COLD_ASSET = equipAsset("oxygen_suit_cold");

    private static final Map<ArmorType, Integer> T1_DEFENSE =
            Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 7, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3);
    private static final Map<ArmorType, Integer> T2_DEFENSE =
            Map.of(ArmorType.HELMET, 4, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 4);

    public static final ArmorMaterial OXYGEN_SUIT_MATERIAL = new ArmorMaterial(
            28, T1_DEFENSE, 12, SoundEvents.ARMOR_EQUIP_IRON, 1.5F, 0.0F, cTag("ingots/nerosteel"), OXYGEN_SUIT_ASSET);
    public static final ArmorMaterial OXYGEN_SUIT_T2_MATERIAL = new ArmorMaterial(
            36, T2_DEFENSE, 14, SoundEvents.ARMOR_EQUIP_NETHERITE, 2.0F, 0.0F, cTag("gems/cindrite"), OXYGEN_SUIT_T2_ASSET);
    public static final ArmorMaterial OXYGEN_SUIT_HEAT_MATERIAL = new ArmorMaterial(
            36, T2_DEFENSE, 14, SoundEvents.ARMOR_EQUIP_NETHERITE, 2.0F, 0.0F, cTag("gems/cindrite"), OXYGEN_SUIT_HEAT_ASSET);
    public static final ArmorMaterial OXYGEN_SUIT_COLD_MATERIAL = new ArmorMaterial(
            36, T2_DEFENSE, 14, SoundEvents.ARMOR_EQUIP_NETHERITE, 2.0F, 0.0F, cTag("gems/glacite"), OXYGEN_SUIT_COLD_ASSET);

    // --- Tools + armor items -----------------------------------------------
    public static final RegistryEntry<Item> NEROSIUM_PICKAXE =
            item("nerosium_pickaxe", p -> p.pickaxe(NEROSIUM_TOOL_MATERIAL, 1.0F, -2.8F));

    public static final RegistryEntry<Item> OXYGEN_SUIT_HELMET = armor("oxygen_suit_helmet", OXYGEN_SUIT_MATERIAL, ArmorType.HELMET);
    public static final RegistryEntry<Item> OXYGEN_SUIT_CHESTPLATE = armor("oxygen_suit_chestplate", OXYGEN_SUIT_MATERIAL, ArmorType.CHESTPLATE);
    public static final RegistryEntry<Item> OXYGEN_SUIT_LEGGINGS = armor("oxygen_suit_leggings", OXYGEN_SUIT_MATERIAL, ArmorType.LEGGINGS);
    public static final RegistryEntry<Item> OXYGEN_SUIT_BOOTS = armor("oxygen_suit_boots", OXYGEN_SUIT_MATERIAL, ArmorType.BOOTS);
    public static final RegistryEntry<Item> OXYGEN_SUIT_T2_HELMET = armor("oxygen_suit_t2_helmet", OXYGEN_SUIT_T2_MATERIAL, ArmorType.HELMET);
    public static final RegistryEntry<Item> OXYGEN_SUIT_T2_CHESTPLATE = armor("oxygen_suit_t2_chestplate", OXYGEN_SUIT_T2_MATERIAL, ArmorType.CHESTPLATE);
    public static final RegistryEntry<Item> OXYGEN_SUIT_T2_LEGGINGS = armor("oxygen_suit_t2_leggings", OXYGEN_SUIT_T2_MATERIAL, ArmorType.LEGGINGS);
    public static final RegistryEntry<Item> OXYGEN_SUIT_T2_BOOTS = armor("oxygen_suit_t2_boots", OXYGEN_SUIT_T2_MATERIAL, ArmorType.BOOTS);
    public static final RegistryEntry<Item> OXYGEN_SUIT_HEAT_HELMET = armor("oxygen_suit_heat_helmet", OXYGEN_SUIT_HEAT_MATERIAL, ArmorType.HELMET);
    public static final RegistryEntry<Item> OXYGEN_SUIT_HEAT_CHESTPLATE = armor("oxygen_suit_heat_chestplate", OXYGEN_SUIT_HEAT_MATERIAL, ArmorType.CHESTPLATE);
    public static final RegistryEntry<Item> OXYGEN_SUIT_HEAT_LEGGINGS = armor("oxygen_suit_heat_leggings", OXYGEN_SUIT_HEAT_MATERIAL, ArmorType.LEGGINGS);
    public static final RegistryEntry<Item> OXYGEN_SUIT_HEAT_BOOTS = armor("oxygen_suit_heat_boots", OXYGEN_SUIT_HEAT_MATERIAL, ArmorType.BOOTS);
    public static final RegistryEntry<Item> OXYGEN_SUIT_COLD_HELMET = armor("oxygen_suit_cold_helmet", OXYGEN_SUIT_COLD_MATERIAL, ArmorType.HELMET);
    public static final RegistryEntry<Item> OXYGEN_SUIT_COLD_CHESTPLATE = armor("oxygen_suit_cold_chestplate", OXYGEN_SUIT_COLD_MATERIAL, ArmorType.CHESTPLATE);
    public static final RegistryEntry<Item> OXYGEN_SUIT_COLD_LEGGINGS = armor("oxygen_suit_cold_leggings", OXYGEN_SUIT_COLD_MATERIAL, ArmorType.LEGGINGS);
    public static final RegistryEntry<Item> OXYGEN_SUIT_COLD_BOOTS = armor("oxygen_suit_cold_boots", OXYGEN_SUIT_COLD_MATERIAL, ArmorType.BOOTS);

    // --- helpers ------------------------------------------------------------
    private static RegistryEntry<Item> item(String name) {
        return item(name, p -> p);
    }

    private static RegistryEntry<Item> item(String name, UnaryOperator<Item.Properties> cfg) {
        return ITEMS.register(name, key -> new Item(cfg.apply(new Item.Properties().setId(key))));
    }

    private static RegistryEntry<Item> armor(String name, ArmorMaterial material, ArmorType type) {
        return item(name, p -> p.humanoidArmor(material, type));
    }

    private static RegistryEntry<BlockItem> blockItem(String name, RegistryEntry<? extends Block> block) {
        return ITEMS.register(name, key -> new BlockItem(block.get(), new Item.Properties().setId(key)));
    }

    private static TagKey<Item> cTag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }

    private static ResourceKey<EquipmentAsset> equipAsset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, name));
    }

    /** Items grouped by the vanilla creative tab they should appear in. */
    public static Map<ResourceKey<CreativeModeTab>, List<ItemLike>> creativeTabItems() {
        return Map.of(
                CreativeModeTabs.NATURAL_BLOCKS,
                List.<ItemLike>of(NEROSIUM_ORE_ITEM.get(), DEEPSLATE_NEROSIUM_ORE_ITEM.get(),
                        NEROSTEEL_ORE_ITEM.get(), XERTZ_QUARTZ_ORE_ITEM.get(),
                        CINDRITE_ORE_ITEM.get(), GLACITE_ORE_ITEM.get(), METEOR_ROCK_ITEM.get()),
                CreativeModeTabs.BUILDING_BLOCKS,
                List.<ItemLike>of(NEROSIUM_BLOCK_ITEM.get(), RAW_NEROSIUM_BLOCK_ITEM.get(),
                        NEROSTEEL_BLOCK_ITEM.get(), CINDRITE_BLOCK_ITEM.get(), GLACITE_BLOCK_ITEM.get(),
                        STATION_FLOOR_ITEM.get(), STATION_WALL_ITEM.get(),
                        ALIEN_BRICKS_ITEM.get(), CRACKED_ALIEN_BRICKS_ITEM.get(), ALIEN_TILE_ITEM.get(),
                        ALIEN_PILLAR_ITEM.get(), ALIEN_LAMP_ITEM.get(), ALIEN_CRYSTAL_BLOCK_ITEM.get()),
                CreativeModeTabs.INGREDIENTS,
                List.<ItemLike>of(RAW_NEROSIUM.get(), NEROSIUM_INGOT.get(),
                        RAW_NEROSTEEL.get(), NEROSTEEL_INGOT.get(),
                        XERTZ_QUARTZ.get(), CINDRITE.get(), GLACITE.get(),
                        NEROSIUM_DUST.get(), ALIEN_FRAGMENT.get(), ALIEN_TECH_SCRAP.get(), ALIEN_CORE.get(),
                        ROCKET_FUEL_CANISTER.get(), FRAME_CASING.get(), GRAV_STRIDERS.get(), DRIFT_FLEECE.get()),
                CreativeModeTabs.TOOLS_AND_UTILITIES,
                List.<ItemLike>of(NEROSIUM_PICKAXE.get()),
                CreativeModeTabs.COMBAT,
                List.<ItemLike>of(
                        OXYGEN_SUIT_HELMET.get(), OXYGEN_SUIT_CHESTPLATE.get(), OXYGEN_SUIT_LEGGINGS.get(), OXYGEN_SUIT_BOOTS.get(),
                        OXYGEN_SUIT_T2_HELMET.get(), OXYGEN_SUIT_T2_CHESTPLATE.get(), OXYGEN_SUIT_T2_LEGGINGS.get(), OXYGEN_SUIT_T2_BOOTS.get(),
                        OXYGEN_SUIT_HEAT_HELMET.get(), OXYGEN_SUIT_HEAT_CHESTPLATE.get(), OXYGEN_SUIT_HEAT_LEGGINGS.get(), OXYGEN_SUIT_HEAT_BOOTS.get(),
                        OXYGEN_SUIT_COLD_HELMET.get(), OXYGEN_SUIT_COLD_CHESTPLATE.get(), OXYGEN_SUIT_COLD_LEGGINGS.get(), OXYGEN_SUIT_COLD_BOOTS.get()),
                CreativeModeTabs.FUNCTIONAL_BLOCKS,
                List.<ItemLike>of(ITEM_STORE_ITEM.get()));
    }

    private ModItems() {
    }

    public static void init() {
    }
}
