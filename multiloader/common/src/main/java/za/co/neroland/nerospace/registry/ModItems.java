package za.co.neroland.nerospace.registry;

import java.util.ArrayList;
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
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.item.DestinationCompassItem;
import za.co.neroland.nerospace.item.GreenxertzNavigatorItem;
import za.co.neroland.nerospace.item.NerospaceSpawnEggItem;
import za.co.neroland.nerospace.module.ModuleType;
import za.co.neroland.nerospace.module.UpgradeModuleItem;
import za.co.neroland.nerospace.rocket.RocketItem;
import za.co.neroland.nerospace.rocket.RocketTier;
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
    public static final RegistryEntry<BlockItem> VILLAGE_CORE_ITEM = blockItem("village_core", ModBlocks.VILLAGE_CORE);
    public static final RegistryEntry<BlockItem> METEOR_ROCK_ITEM = blockItem("meteor_rock", ModBlocks.METEOR_ROCK);
    public static final RegistryEntry<BlockItem> ITEM_STORE_ITEM = blockItem("item_store", ModBlocks.ITEM_STORE);
    public static final RegistryEntry<BlockItem> BATTERY_ITEM = blockItem("battery", ModBlocks.BATTERY);
    public static final RegistryEntry<BlockItem> FLUID_TANK_ITEM = blockItem("fluid_tank", ModBlocks.FLUID_TANK);
    public static final RegistryEntry<BlockItem> COMBUSTION_GENERATOR_ITEM = blockItem("combustion_generator", ModBlocks.COMBUSTION_GENERATOR);
    public static final RegistryEntry<BlockItem> NEROSIUM_GRINDER_ITEM = blockItem("nerosium_grinder", ModBlocks.NEROSIUM_GRINDER);
    public static final RegistryEntry<BlockItem> PASSIVE_GENERATOR_ITEM = blockItem("passive_generator", ModBlocks.PASSIVE_GENERATOR);
    public static final RegistryEntry<BlockItem> UNIVERSAL_PIPE_ITEM = blockItem("universal_pipe", ModBlocks.UNIVERSAL_PIPE);
    public static final RegistryEntry<BlockItem> TRASH_CAN_ITEM = blockItem("trash_can", ModBlocks.TRASH_CAN);
    public static final RegistryEntry<BlockItem> CREATIVE_BATTERY_ITEM = blockItem("creative_battery", ModBlocks.CREATIVE_BATTERY);
    public static final RegistryEntry<BlockItem> CREATIVE_FLUID_TANK_ITEM = blockItem("creative_fluid_tank", ModBlocks.CREATIVE_FLUID_TANK);
    public static final RegistryEntry<BlockItem> CREATIVE_GAS_TANK_ITEM = blockItem("creative_gas_tank", ModBlocks.CREATIVE_GAS_TANK);
    public static final RegistryEntry<BlockItem> CREATIVE_ITEM_STORE_ITEM = blockItem("creative_item_store", ModBlocks.CREATIVE_ITEM_STORE);
    public static final RegistryEntry<BlockItem> GAS_TANK_ITEM = blockItem("gas_tank", ModBlocks.GAS_TANK);
    public static final RegistryEntry<BlockItem> OXYGEN_GENERATOR_ITEM = blockItem("oxygen_generator", ModBlocks.OXYGEN_GENERATOR);
    public static final RegistryEntry<BlockItem> SOLAR_PANEL_ITEM = blockItem("solar_panel", ModBlocks.SOLAR_PANEL);
    public static final RegistryEntry<BlockItem> ROCKET_LAUNCH_PAD_ITEM = blockItem("rocket_launch_pad", ModBlocks.ROCKET_LAUNCH_PAD);
    public static final RegistryEntry<BlockItem> LAUNCH_GANTRY_ITEM = blockItem("launch_gantry", ModBlocks.LAUNCH_GANTRY);
    public static final RegistryEntry<BlockItem> FUEL_TANK_ITEM = blockItem("fuel_tank", ModBlocks.FUEL_TANK);
    public static final RegistryEntry<BlockItem> FUEL_REFINERY_ITEM = blockItem("fuel_refinery", ModBlocks.FUEL_REFINERY);
    public static final RegistryEntry<BlockItem> QUARRY_CONTROLLER_ITEM = blockItem("quarry_controller", ModBlocks.QUARRY_CONTROLLER);
    public static final RegistryEntry<BlockItem> QUARRY_LANDMARK_ITEM = blockItem("quarry_landmark", ModBlocks.QUARRY_LANDMARK);

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
    /** A real bucket of the {@code rocket_fuel} fluid; places the liquid block / fills tanks. */
    public static final RegistryEntry<BucketItem> ROCKET_FUEL_BUCKET = ITEMS.register("rocket_fuel_bucket",
            key -> new BucketItem((Fluid) ModFluids.ROCKET_FUEL.get(), new Item.Properties().stacksTo(1).setId(key)));
    public static final RegistryEntry<Item> FRAME_CASING = item("frame_casing");
    public static final RegistryEntry<Item> GRAV_STRIDERS = item("grav_striders");
    public static final RegistryEntry<Item> DRIFT_FLEECE = item("drift_fleece");
    /** Trade-only Artificer gear; ported as a plain item (its custom gear behaviour is deferred). */
    public static final RegistryEntry<Item> XERTZ_RESONATOR = item("xertz_resonator");

    // --- Machine upgrade modules (the quarry is the first consumer) ----------
    public static final RegistryEntry<Item> SPEED_MODULE = module("speed_module", ModuleType.SPEED);
    public static final RegistryEntry<Item> EFFICIENCY_MODULE = module("efficiency_module", ModuleType.EFFICIENCY);
    public static final RegistryEntry<Item> FORTUNE_MODULE = module("fortune_module", ModuleType.FORTUNE);
    public static final RegistryEntry<Item> SILK_TOUCH_MODULE = module("silk_touch_module", ModuleType.SILK_TOUCH);

    // --- Rockets (one item per tier; deploys a RocketEntity onto a launch pad) ----
    public static final RegistryEntry<RocketItem> ROCKET_TIER_1 = ITEMS.register("rocket_tier_1",
            key -> new RocketItem(new Item.Properties().stacksTo(1).setId(key), RocketTier.TIER_1));
    public static final RegistryEntry<RocketItem> ROCKET_TIER_2 = ITEMS.register("rocket_tier_2",
            key -> new RocketItem(new Item.Properties().stacksTo(1).setId(key), RocketTier.TIER_2));
    public static final RegistryEntry<RocketItem> ROCKET_TIER_3 = ITEMS.register("rocket_tier_3",
            key -> new RocketItem(new Item.Properties().stacksTo(1).setId(key), RocketTier.TIER_3));
    public static final RegistryEntry<RocketItem> ROCKET_TIER_4 = ITEMS.register("rocket_tier_4",
            key -> new RocketItem(new Item.Properties().stacksTo(1).setId(key), RocketTier.TIER_4));

    // --- Creative travel devices --------------------------------------------
    public static final RegistryEntry<Item> GREENXERTZ_NAVIGATOR = ITEMS.register("greenxertz_navigator",
            key -> new GreenxertzNavigatorItem(new Item.Properties().stacksTo(1).setId(key)));
    public static final RegistryEntry<Item> STATION_COMPASS = ITEMS.register("station_compass",
            key -> new DestinationCompassItem(new Item.Properties().stacksTo(1).setId(key), ModDimensions.STATION_LEVEL));
    public static final RegistryEntry<Item> GREENXERTZ_COMPASS = ITEMS.register("greenxertz_compass",
            key -> new DestinationCompassItem(new Item.Properties().stacksTo(1).setId(key), ModDimensions.GREENXERTZ_LEVEL));
    public static final RegistryEntry<Item> CINDARA_COMPASS = ITEMS.register("cindara_compass",
            key -> new DestinationCompassItem(new Item.Properties().stacksTo(1).setId(key), ModDimensions.CINDARA_LEVEL));
    public static final RegistryEntry<Item> GLACIRA_COMPASS = ITEMS.register("glacira_compass",
            key -> new DestinationCompassItem(new Item.Properties().stacksTo(1).setId(key), ModDimensions.GLACIRA_LEVEL));

    // --- Spawn eggs (lazy entity-type supplier; ruin warden is summon-only) ----
    public static final RegistryEntry<Item> XERTZ_STALKER_SPAWN_EGG = spawnEgg("xertz_stalker_spawn_egg", ModEntities.XERTZ_STALKER);
    public static final RegistryEntry<Item> QUARTZ_CRAWLER_SPAWN_EGG = spawnEgg("quartz_crawler_spawn_egg", ModEntities.QUARTZ_CRAWLER);
    public static final RegistryEntry<Item> GREENLING_SPAWN_EGG = spawnEgg("greenling_spawn_egg", ModEntities.GREENLING);
    public static final RegistryEntry<Item> ALIEN_VILLAGER_SPAWN_EGG = spawnEgg("alien_villager_spawn_egg", ModEntities.ALIEN_VILLAGER);
    public static final RegistryEntry<Item> CINDER_STALKER_SPAWN_EGG = spawnEgg("cinder_stalker_spawn_egg", ModEntities.CINDER_STALKER);
    public static final RegistryEntry<Item> FROST_STRIDER_SPAWN_EGG = spawnEgg("frost_strider_spawn_egg", ModEntities.FROST_STRIDER);
    public static final RegistryEntry<Item> MEADOW_LOPER_SPAWN_EGG = spawnEgg("meadow_loper_spawn_egg", ModEntities.MEADOW_LOPER);
    public static final RegistryEntry<Item> EMBER_STRUTTER_SPAWN_EGG = spawnEgg("ember_strutter_spawn_egg", ModEntities.EMBER_STRUTTER);
    public static final RegistryEntry<Item> WOOLLY_DRIFT_SPAWN_EGG = spawnEgg("woolly_drift_spawn_egg", ModEntities.WOOLLY_DRIFT);

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

    private static RegistryEntry<Item> spawnEgg(String name, RegistryEntry<? extends EntityType<? extends Mob>> type) {
        return ITEMS.register(name, key -> new NerospaceSpawnEggItem(new Item.Properties().setId(key), type));
    }

    private static RegistryEntry<Item> module(String name, ModuleType type) {
        return ITEMS.register(name, key -> new UpgradeModuleItem(new Item.Properties().setId(key), type));
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
                        ALIEN_PILLAR_ITEM.get(), ALIEN_LAMP_ITEM.get(), ALIEN_CRYSTAL_BLOCK_ITEM.get(),
                        VILLAGE_CORE_ITEM.get()),
                CreativeModeTabs.INGREDIENTS,
                List.<ItemLike>of(RAW_NEROSIUM.get(), NEROSIUM_INGOT.get(),
                        RAW_NEROSTEEL.get(), NEROSTEEL_INGOT.get(),
                        XERTZ_QUARTZ.get(), CINDRITE.get(), GLACITE.get(),
                        NEROSIUM_DUST.get(), ALIEN_FRAGMENT.get(), ALIEN_TECH_SCRAP.get(), ALIEN_CORE.get(),
                        ROCKET_FUEL_CANISTER.get(), FRAME_CASING.get(), GRAV_STRIDERS.get(), DRIFT_FLEECE.get()),
                CreativeModeTabs.TOOLS_AND_UTILITIES,
                List.<ItemLike>of(NEROSIUM_PICKAXE.get(), ROCKET_FUEL_BUCKET.get(), XERTZ_RESONATOR.get(),
                        ROCKET_TIER_1.get(), ROCKET_TIER_2.get(), ROCKET_TIER_3.get(), ROCKET_TIER_4.get(),
                        GREENXERTZ_NAVIGATOR.get(), STATION_COMPASS.get(), GREENXERTZ_COMPASS.get(),
                        CINDARA_COMPASS.get(), GLACIRA_COMPASS.get()),
                CreativeModeTabs.SPAWN_EGGS,
                List.<ItemLike>of(XERTZ_STALKER_SPAWN_EGG.get(), QUARTZ_CRAWLER_SPAWN_EGG.get(),
                        GREENLING_SPAWN_EGG.get(), ALIEN_VILLAGER_SPAWN_EGG.get(), CINDER_STALKER_SPAWN_EGG.get(),
                        FROST_STRIDER_SPAWN_EGG.get(), MEADOW_LOPER_SPAWN_EGG.get(), EMBER_STRUTTER_SPAWN_EGG.get(),
                        WOOLLY_DRIFT_SPAWN_EGG.get()),
                CreativeModeTabs.COMBAT,
                List.<ItemLike>of(
                        OXYGEN_SUIT_HELMET.get(), OXYGEN_SUIT_CHESTPLATE.get(), OXYGEN_SUIT_LEGGINGS.get(), OXYGEN_SUIT_BOOTS.get(),
                        OXYGEN_SUIT_T2_HELMET.get(), OXYGEN_SUIT_T2_CHESTPLATE.get(), OXYGEN_SUIT_T2_LEGGINGS.get(), OXYGEN_SUIT_T2_BOOTS.get(),
                        OXYGEN_SUIT_HEAT_HELMET.get(), OXYGEN_SUIT_HEAT_CHESTPLATE.get(), OXYGEN_SUIT_HEAT_LEGGINGS.get(), OXYGEN_SUIT_HEAT_BOOTS.get(),
                        OXYGEN_SUIT_COLD_HELMET.get(), OXYGEN_SUIT_COLD_CHESTPLATE.get(), OXYGEN_SUIT_COLD_LEGGINGS.get(), OXYGEN_SUIT_COLD_BOOTS.get()),
                CreativeModeTabs.FUNCTIONAL_BLOCKS,
                List.<ItemLike>of(ITEM_STORE_ITEM.get(), BATTERY_ITEM.get(), FLUID_TANK_ITEM.get(), COMBUSTION_GENERATOR_ITEM.get(), NEROSIUM_GRINDER_ITEM.get(), PASSIVE_GENERATOR_ITEM.get(), UNIVERSAL_PIPE_ITEM.get(), TRASH_CAN_ITEM.get(), CREATIVE_BATTERY_ITEM.get(), GAS_TANK_ITEM.get(), OXYGEN_GENERATOR_ITEM.get(), SOLAR_PANEL_ITEM.get(), ROCKET_LAUNCH_PAD_ITEM.get(), LAUNCH_GANTRY_ITEM.get(), FUEL_TANK_ITEM.get(), FUEL_REFINERY_ITEM.get(), QUARRY_CONTROLLER_ITEM.get(), QUARRY_LANDMARK_ITEM.get(),
                        SPEED_MODULE.get(), EFFICIENCY_MODULE.get(), FORTUNE_MODULE.get(), SILK_TOUCH_MODULE.get(),
                        CREATIVE_FLUID_TANK_ITEM.get(), CREATIVE_GAS_TANK_ITEM.get(), CREATIVE_ITEM_STORE_ITEM.get()));
    }

    /**
     * All mod items in a defined order, for the dedicated {@link ModCreativeTab}. Flattens
     * {@link #creativeTabItems()} (which still groups by vanilla theme) in a stable tab order.
     */
    public static List<ItemLike> creativeContents() {
        Map<ResourceKey<CreativeModeTab>, List<ItemLike>> groups = creativeTabItems();
        List<ItemLike> all = new ArrayList<>();
        for (ResourceKey<CreativeModeTab> tab : List.of(
                CreativeModeTabs.NATURAL_BLOCKS, CreativeModeTabs.BUILDING_BLOCKS,
                CreativeModeTabs.INGREDIENTS, CreativeModeTabs.TOOLS_AND_UTILITIES,
                CreativeModeTabs.COMBAT, CreativeModeTabs.FUNCTIONAL_BLOCKS,
                CreativeModeTabs.SPAWN_EGGS)) {
            List<ItemLike> group = groups.get(tab);
            if (group != null) {
                all.addAll(group);
            }
        }
        return all;
    }

    private ModItems() {
    }

    public static void init() {
    }
}
