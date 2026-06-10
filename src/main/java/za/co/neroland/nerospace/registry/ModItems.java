package za.co.neroland.nerospace.registry;

import java.util.Map;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.item.DestinationCompassItem;
import za.co.neroland.nerospace.item.GreenxertzNavigatorItem;
import za.co.neroland.nerospace.item.NerospaceSpawnEggItem;
import za.co.neroland.nerospace.rocket.RocketItem;
import za.co.neroland.nerospace.rocket.RocketTier;

/**
 * Central item registry for Nerospace (Phase 1 — materials slice).
 *
 * <p>Static fields initialise in source order, so {@link #NEROSIUM_TOOL_MATERIAL} is declared
 * before any tool that consumes it. Tools in 26.1 are plain {@link Item}s configured through
 * {@code Item.Properties} delegates (e.g. {@code props.pickaxe(...)}).</p>
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nerospace.MODID);

    /**
     * Nerosium sits between iron and diamond: iron-tier mining (reuses
     * {@code minecraft:incorrect_for_iron_tool}), higher durability and a small attack bonus.
     * Repaired with items tagged {@code c:ingots/nerosium}.
     */
    public static final ToolMaterial NEROSIUM_TOOL_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL,
            350,
            7.0F,
            2.5F,
            15,
            ModTags.Items.INGOTS_NEROSIUM);

    // --- Materials ----------------------------------------------------------

    public static final DeferredItem<Item> RAW_NEROSIUM = ITEMS.registerSimpleItem("raw_nerosium");
    public static final DeferredItem<Item> NEROSIUM_INGOT = ITEMS.registerSimpleItem("nerosium_ingot");
    // Phase 2: grinder output.
    public static final DeferredItem<Item> NEROSIUM_DUST = ITEMS.registerSimpleItem("nerosium_dust");

    // Phase 3: Greenxertz materials.
    public static final DeferredItem<Item> RAW_NEROSTEEL = ITEMS.registerSimpleItem("raw_nerosteel");
    public static final DeferredItem<Item> NEROSTEEL_INGOT = ITEMS.registerSimpleItem("nerosteel_ingot");
    /** Xertz quartz gem — dropped directly when mining xertz quartz ore (nether-quartz analogue). */
    public static final DeferredItem<Item> XERTZ_QUARTZ = ITEMS.registerSimpleItem("xertz_quartz");

    // Phase 7: Cindara (volcanic moon) materials.
    /** Cindrite gem — dropped by cindrite ore on Cindara; gates the Tier-3 rocket. */
    public static final DeferredItem<Item> CINDRITE = ITEMS.registerSimpleItem("cindrite");

    // Glacira (frozen moon) materials (NEW_DESTINATION_DESIGN.md).
    /** Glacite gem — dropped by glacite ore on Glacira; feeds future suit variants/terraforming. */
    public static final DeferredItem<Item> GLACITE = ITEMS.registerSimpleItem("glacite");

    // --- Tools --------------------------------------------------------------

    public static final DeferredItem<Item> NEROSIUM_PICKAXE = ITEMS.registerItem(
            "nerosium_pickaxe",
            props -> new Item(props.pickaxe(NEROSIUM_TOOL_MATERIAL, 1.0F, -2.8F)));

    // --- Space suit (Phase 8d) ----------------------------------------------

    /** The worn-armour asset key ({@code assets/nerospace/equipment/oxygen_suit.json}). */
    public static final ResourceKey<EquipmentAsset> OXYGEN_SUIT_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Nerospace.MODID, "oxygen_suit"));

    /**
     * Oxygen Suit material — diamond-class protection, repaired with nerosteel. Wearing the full set
     * acts as personal life support off-world (see {@code GreenxertzAtmosphere}).
     */
    public static final ArmorMaterial OXYGEN_SUIT_MATERIAL = new ArmorMaterial(
            28,
            Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 7, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3),
            12,
            SoundEvents.ARMOR_EQUIP_IRON,
            1.5F,
            0.0F,
            ModTags.Items.INGOTS_NEROSTEEL,
            OXYGEN_SUIT_ASSET);

    public static final DeferredItem<Item> OXYGEN_SUIT_HELMET = ITEMS.registerItem(
            "oxygen_suit_helmet",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_MATERIAL, ArmorType.HELMET)));
    public static final DeferredItem<Item> OXYGEN_SUIT_CHESTPLATE = ITEMS.registerItem(
            "oxygen_suit_chestplate",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_MATERIAL, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> OXYGEN_SUIT_LEGGINGS = ITEMS.registerItem(
            "oxygen_suit_leggings",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_MATERIAL, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> OXYGEN_SUIT_BOOTS = ITEMS.registerItem(
            "oxygen_suit_boots",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_MATERIAL, ArmorType.BOOTS)));

    /** The Tier 2 worn-armour asset key ({@code assets/nerospace/equipment/oxygen_suit_t2.json}). */
    public static final ResourceKey<EquipmentAsset> OXYGEN_SUIT_T2_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Nerospace.MODID, "oxygen_suit_t2"));

    /**
     * Tier 2 Oxygen Suit material — the cindrite-upgraded suit: tougher, repaired with cindrite. A
     * full Tier 2 set carries a bigger air tank and refills faster at airlocks (see
     * {@code GreenxertzAtmosphere}).
     */
    public static final ArmorMaterial OXYGEN_SUIT_T2_MATERIAL = new ArmorMaterial(
            36,
            Map.of(ArmorType.HELMET, 4, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 4),
            14,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            2.0F,
            0.0F,
            ModTags.Items.GEMS_CINDRITE,
            OXYGEN_SUIT_T2_ASSET);

    public static final DeferredItem<Item> OXYGEN_SUIT_T2_HELMET = ITEMS.registerItem(
            "oxygen_suit_t2_helmet",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_T2_MATERIAL, ArmorType.HELMET)));
    public static final DeferredItem<Item> OXYGEN_SUIT_T2_CHESTPLATE = ITEMS.registerItem(
            "oxygen_suit_t2_chestplate",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_T2_MATERIAL, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> OXYGEN_SUIT_T2_LEGGINGS = ITEMS.registerItem(
            "oxygen_suit_t2_leggings",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_T2_MATERIAL, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> OXYGEN_SUIT_T2_BOOTS = ITEMS.registerItem(
            "oxygen_suit_t2_boots",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_T2_MATERIAL, ArmorType.BOOTS)));

    // --- Hazard suit variants (SUIT_HAZARD_DESIGN.md) ------------------------
    // Sidegrades OF Tier 2: same protection class and Tier-2 tank; a full matching set counters
    // its dimension hazard (heat = Cindara, cold = Glacira). Detection lives in
    // GreenxertzAtmosphere.hazardShield.

    /** The Thermal Suit worn-armour asset key ({@code assets/nerospace/equipment/oxygen_suit_heat.json}). */
    public static final ResourceKey<EquipmentAsset> OXYGEN_SUIT_HEAT_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Nerospace.MODID, "oxygen_suit_heat"));

    /** Thermal Suit material — T2 numbers, repaired with cindrite. */
    public static final ArmorMaterial OXYGEN_SUIT_HEAT_MATERIAL = new ArmorMaterial(
            36,
            Map.of(ArmorType.HELMET, 4, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 4),
            14,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            2.0F,
            0.0F,
            ModTags.Items.GEMS_CINDRITE,
            OXYGEN_SUIT_HEAT_ASSET);

    public static final DeferredItem<Item> OXYGEN_SUIT_HEAT_HELMET = ITEMS.registerItem(
            "oxygen_suit_heat_helmet",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_HEAT_MATERIAL, ArmorType.HELMET)));
    public static final DeferredItem<Item> OXYGEN_SUIT_HEAT_CHESTPLATE = ITEMS.registerItem(
            "oxygen_suit_heat_chestplate",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_HEAT_MATERIAL, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> OXYGEN_SUIT_HEAT_LEGGINGS = ITEMS.registerItem(
            "oxygen_suit_heat_leggings",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_HEAT_MATERIAL, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> OXYGEN_SUIT_HEAT_BOOTS = ITEMS.registerItem(
            "oxygen_suit_heat_boots",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_HEAT_MATERIAL, ArmorType.BOOTS)));

    /** The Cryo Suit worn-armour asset key ({@code assets/nerospace/equipment/oxygen_suit_cold.json}). */
    public static final ResourceKey<EquipmentAsset> OXYGEN_SUIT_COLD_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Nerospace.MODID, "oxygen_suit_cold"));

    /** Cryo Suit material — T2 numbers, repaired with glacite. */
    public static final ArmorMaterial OXYGEN_SUIT_COLD_MATERIAL = new ArmorMaterial(
            36,
            Map.of(ArmorType.HELMET, 4, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 4),
            14,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            2.0F,
            0.0F,
            ModTags.Items.GEMS_GLACITE,
            OXYGEN_SUIT_COLD_ASSET);

    public static final DeferredItem<Item> OXYGEN_SUIT_COLD_HELMET = ITEMS.registerItem(
            "oxygen_suit_cold_helmet",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_COLD_MATERIAL, ArmorType.HELMET)));
    public static final DeferredItem<Item> OXYGEN_SUIT_COLD_CHESTPLATE = ITEMS.registerItem(
            "oxygen_suit_cold_chestplate",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_COLD_MATERIAL, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> OXYGEN_SUIT_COLD_LEGGINGS = ITEMS.registerItem(
            "oxygen_suit_cold_leggings",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_COLD_MATERIAL, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> OXYGEN_SUIT_COLD_BOOTS = ITEMS.registerItem(
            "oxygen_suit_cold_boots",
            props -> new Item(props.humanoidArmor(OXYGEN_SUIT_COLD_MATERIAL, ArmorType.BOOTS)));

    // --- Travel (Phase 3, temporary; replaced by rockets in Phase 4) --------

    /**
     * Greenxertz Navigator — a placeholder travel device. Right-click to toggle between the
     * overworld and the Greenxertz dimension (server-side). Crafted from overworld materials so it
     * is obtainable before ever reaching the planet.
     */
    public static final DeferredItem<Item> GREENXERTZ_NAVIGATOR = ITEMS.registerItem(
            "greenxertz_navigator",
            props -> new GreenxertzNavigatorItem(props.stacksTo(1)));

    // Creative-only destination compasses (Phase 7 polish) — one-click travel for testing/building.
    public static final DeferredItem<Item> STATION_COMPASS = ITEMS.registerItem(
            "station_compass",
            props -> new DestinationCompassItem(props.stacksTo(1), ModDimensions.STATION_LEVEL));
    public static final DeferredItem<Item> GREENXERTZ_COMPASS = ITEMS.registerItem(
            "greenxertz_compass",
            props -> new DestinationCompassItem(props.stacksTo(1), ModDimensions.GREENXERTZ_LEVEL));
    public static final DeferredItem<Item> CINDARA_COMPASS = ITEMS.registerItem(
            "cindara_compass",
            props -> new DestinationCompassItem(props.stacksTo(1), ModDimensions.CINDARA_LEVEL));
    public static final DeferredItem<Item> GLACIRA_COMPASS = ITEMS.registerItem(
            "glacira_compass",
            props -> new DestinationCompassItem(props.stacksTo(1), ModDimensions.GLACIRA_LEVEL));

    // --- Rockets (Phase 4) --------------------------------------------------

    /**
     * Rocket Fuel canister — a craftable, stackable refill worth {@code RocketEntity.CANISTER_MB}
     * millibuckets. Right-click a deployed rocket with it to top up the tank. (A full NeoForge
     * liquid-fuel {@code Fluid}/bucket is a deliberate follow-up so it can be validated in-client.)
     */
    public static final DeferredItem<Item> ROCKET_FUEL_CANISTER = ITEMS.registerSimpleItem("rocket_fuel_canister");

    /**
     * Station Charter (MULTI_STATION_DESIGN.md): consumed by the rocket's FOUND trajectory node to
     * found a new station. Rename it in an anvil to name the station (unnamed = "Station N").
     */
    public static final DeferredItem<Item> STATION_CHARTER = ITEMS.registerItem(
            "station_charter", props -> new Item(props.stacksTo(16)));

    /** A real bucket of the {@code rocket_fuel} fluid; right-click a rocket to pour it into the tank. */
    public static final DeferredItem<BucketItem> ROCKET_FUEL_BUCKET = ITEMS.registerItem(
            "rocket_fuel_bucket",
            props -> new BucketItem(ModFluids.ROCKET_FUEL.get(), props.stacksTo(1)));

    public static final DeferredItem<Item> ROCKET_TIER_1 = ITEMS.registerItem(
            "rocket_tier_1",
            props -> new RocketItem(props.stacksTo(1), RocketTier.TIER_1));
    public static final DeferredItem<Item> ROCKET_TIER_2 = ITEMS.registerItem(
            "rocket_tier_2",
            props -> new RocketItem(props.stacksTo(1), RocketTier.TIER_2));
    public static final DeferredItem<Item> ROCKET_TIER_3 = ITEMS.registerItem(
            "rocket_tier_3",
            props -> new RocketItem(props.stacksTo(1), RocketTier.TIER_3));
    /** Tier 4 — reaches Glacira; deploys ONLY on the Heavy Launch Complex (design doc §5). */
    public static final DeferredItem<Item> ROCKET_TIER_4 = ITEMS.registerItem(
            "rocket_tier_4",
            props -> new RocketItem(props.stacksTo(1), RocketTier.TIER_4));

    // --- Block items --------------------------------------------------------

    public static final DeferredItem<BlockItem> NEROSIUM_ORE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.NEROSIUM_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_NEROSIUM_ORE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_NEROSIUM_ORE);
    public static final DeferredItem<BlockItem> NEROSIUM_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.NEROSIUM_BLOCK);
    public static final DeferredItem<BlockItem> RAW_NEROSIUM_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.RAW_NEROSIUM_BLOCK);
    public static final DeferredItem<BlockItem> NEROSIUM_GRINDER_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.NEROSIUM_GRINDER);

    // Phase 3 block items.
    public static final DeferredItem<BlockItem> NEROSTEEL_ORE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.NEROSTEEL_ORE);
    public static final DeferredItem<BlockItem> XERTZ_QUARTZ_ORE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.XERTZ_QUARTZ_ORE);
    public static final DeferredItem<BlockItem> NEROSTEEL_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.NEROSTEEL_BLOCK);

    // Phase 7 — Cindara block items.
    public static final DeferredItem<BlockItem> CINDRITE_ORE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.CINDRITE_ORE);
    public static final DeferredItem<BlockItem> CINDRITE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.CINDRITE_BLOCK);

    // Glacira block items.
    public static final DeferredItem<BlockItem> GLACITE_ORE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.GLACITE_ORE);
    public static final DeferredItem<BlockItem> GLACITE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.GLACITE_BLOCK);

    // Phase 7c — station block items.
    public static final DeferredItem<BlockItem> STATION_FLOOR_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.STATION_FLOOR);
    public static final DeferredItem<BlockItem> STATION_WALL_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.STATION_WALL);

    // Phase 4 block item.
    public static final DeferredItem<BlockItem> ROCKET_LAUNCH_PAD_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.ROCKET_LAUNCH_PAD);

    // Heavy Launch Complex module.
    public static final DeferredItem<BlockItem> LAUNCH_GANTRY_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.LAUNCH_GANTRY);

    // Phase 8a block item.
    public static final DeferredItem<BlockItem> FUEL_TANK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.FUEL_TANK);

    // Phase 8c block item.
    public static final DeferredItem<BlockItem> OXYGEN_GENERATOR_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.OXYGEN_GENERATOR);

    // Terraform design — terraformer machine block item.
    public static final DeferredItem<BlockItem> TERRAFORMER_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.TERRAFORMER);

    // Hydration Module block item (DEEPER_TERRAFORM_DESIGN.md §3.1).
    public static final DeferredItem<BlockItem> HYDRATION_MODULE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.HYDRATION_MODULE);

    // Terraform Monitor block item (DEEPER_TERRAFORM_DESIGN.md §6).
    public static final DeferredItem<BlockItem> TERRAFORM_MONITOR_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.TERRAFORM_MONITOR);

    // Power grid block items.
    public static final DeferredItem<BlockItem> UNIVERSAL_PIPE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.UNIVERSAL_PIPE);
    public static final DeferredItem<BlockItem> COMBUSTION_GENERATOR_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.COMBUSTION_GENERATOR);
    public static final DeferredItem<BlockItem> PASSIVE_GENERATOR_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.PASSIVE_GENERATOR);

    // Storage endpoints + creative sources.
    public static final DeferredItem<BlockItem> BATTERY_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.BATTERY);
    public static final DeferredItem<BlockItem> CREATIVE_BATTERY_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.CREATIVE_BATTERY);
    public static final DeferredItem<BlockItem> FLUID_TANK_ITEM_BLOCK =
            ITEMS.registerSimpleBlockItem(ModBlocks.FLUID_TANK);
    public static final DeferredItem<BlockItem> CREATIVE_FLUID_TANK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.CREATIVE_FLUID_TANK);
    public static final DeferredItem<BlockItem> GAS_TANK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.GAS_TANK);
    public static final DeferredItem<BlockItem> CREATIVE_GAS_TANK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.CREATIVE_GAS_TANK);
    public static final DeferredItem<BlockItem> ITEM_STORE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.ITEM_STORE);
    public static final DeferredItem<BlockItem> CREATIVE_ITEM_STORE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.CREATIVE_ITEM_STORE);

    // The Configurator — the network tool (per-face I/O modes).
    public static final DeferredItem<Item> CONFIGURATOR = ITEMS.registerItem(
            "configurator", props -> new za.co.neroland.nerospace.item.ConfiguratorItem(props.stacksTo(1)));

    // Pipe logistics: per-face item filter + segment upgrades.
    public static final DeferredItem<Item> PIPE_FILTER = ITEMS.registerItem(
            "pipe_filter", props -> new za.co.neroland.nerospace.item.PipeFilterItem(props.stacksTo(16)));
    public static final DeferredItem<Item> SPEED_UPGRADE = ITEMS.registerItem(
            "speed_upgrade", props -> new za.co.neroland.nerospace.item.PipeUpgradeItem(props,
                    za.co.neroland.nerospace.item.PipeUpgradeItem.Kind.SPEED));
    public static final DeferredItem<Item> CAPACITY_UPGRADE = ITEMS.registerItem(
            "capacity_upgrade", props -> new za.co.neroland.nerospace.item.PipeUpgradeItem(props,
                    za.co.neroland.nerospace.item.PipeUpgradeItem.Kind.CAPACITY));

    // --- Star Guide (progression block, 1.0) ---------------------------------
    public static final DeferredItem<BlockItem> STAR_GUIDE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.STAR_GUIDE);
    public static final DeferredItem<Item> STAR_GUIDE_BOOK = ITEMS.registerItem(
            "star_guide_book", props -> new za.co.neroland.nerospace.item.StarGuideBookItem(props.stacksTo(1)));

    // --- Spawn eggs (Phase 10e) --------------------------------------------
    public static final DeferredItem<Item> XERTZ_STALKER_SPAWN_EGG = ITEMS.registerItem(
            "xertz_stalker_spawn_egg", props -> new NerospaceSpawnEggItem(props, ModEntities.XERTZ_STALKER));
    public static final DeferredItem<Item> QUARTZ_CRAWLER_SPAWN_EGG = ITEMS.registerItem(
            "quartz_crawler_spawn_egg", props -> new NerospaceSpawnEggItem(props, ModEntities.QUARTZ_CRAWLER));
    public static final DeferredItem<Item> GREENLING_SPAWN_EGG = ITEMS.registerItem(
            "greenling_spawn_egg", props -> new NerospaceSpawnEggItem(props, ModEntities.GREENLING));
    public static final DeferredItem<Item> CINDER_STALKER_SPAWN_EGG = ITEMS.registerItem(
            "cinder_stalker_spawn_egg", props -> new NerospaceSpawnEggItem(props, ModEntities.CINDER_STALKER));
    public static final DeferredItem<Item> FROST_STRIDER_SPAWN_EGG = ITEMS.registerItem(
            "frost_strider_spawn_egg", props -> new NerospaceSpawnEggItem(props, ModEntities.FROST_STRIDER));

    // --- Terraform livestock (DEEPER_TERRAFORM_DESIGN.md §5) ----------------
    public static final DeferredItem<Item> MEADOW_LOPER_SPAWN_EGG = ITEMS.registerItem(
            "meadow_loper_spawn_egg", props -> new NerospaceSpawnEggItem(props, ModEntities.MEADOW_LOPER));
    public static final DeferredItem<Item> EMBER_STRUTTER_SPAWN_EGG = ITEMS.registerItem(
            "ember_strutter_spawn_egg", props -> new NerospaceSpawnEggItem(props, ModEntities.EMBER_STRUTTER));
    public static final DeferredItem<Item> WOOLLY_DRIFT_SPAWN_EGG = ITEMS.registerItem(
            "woolly_drift_spawn_egg", props -> new NerospaceSpawnEggItem(props, ModEntities.WOOLLY_DRIFT));

    /** Meadow Loper drop: a hearty haunch (no cooked variant in slice 1 — design §13). */
    public static final DeferredItem<Item> LOPER_HAUNCH = ITEMS.registerItem(
            "loper_haunch", props -> new Item(props.food(new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(8).saturationModifier(0.8F).build())));
    /** Ember Strutter drop: a lean drumstick. */
    public static final DeferredItem<Item> STRUTTER_DRUMSTICK = ITEMS.registerItem(
            "strutter_drumstick", props -> new Item(props.food(new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(6).saturationModifier(0.6F).build())));
    /** Woolly Drift drop: insulating fleece — crafts into string (design §5). */
    public static final DeferredItem<Item> DRIFT_FLEECE = ITEMS.registerSimpleItem("drift_fleece");

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
