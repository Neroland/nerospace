package za.co.neroland.nerospace.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.item.DestinationCompassItem;
import za.co.neroland.nerospace.item.GreenxertzNavigatorItem;
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

    // --- Tools --------------------------------------------------------------

    public static final DeferredItem<Item> NEROSIUM_PICKAXE = ITEMS.registerItem(
            "nerosium_pickaxe",
            props -> new Item(props.pickaxe(NEROSIUM_TOOL_MATERIAL, 1.0F, -2.8F)));

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

    // --- Rockets (Phase 4) --------------------------------------------------

    /**
     * Rocket Fuel canister — a craftable, stackable refill worth {@code RocketEntity.CANISTER_MB}
     * millibuckets. Right-click a deployed rocket with it to top up the tank. (A full NeoForge
     * liquid-fuel {@code Fluid}/bucket is a deliberate follow-up so it can be validated in-client.)
     */
    public static final DeferredItem<Item> ROCKET_FUEL_CANISTER = ITEMS.registerSimpleItem("rocket_fuel_canister");

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

    // Phase 7c — station block items.
    public static final DeferredItem<BlockItem> STATION_FLOOR_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.STATION_FLOOR);
    public static final DeferredItem<BlockItem> STATION_WALL_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.STATION_WALL);

    // Phase 4 block item.
    public static final DeferredItem<BlockItem> ROCKET_LAUNCH_PAD_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.ROCKET_LAUNCH_PAD);

    // Phase 8a block item.
    public static final DeferredItem<BlockItem> FUEL_TANK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.FUEL_TANK);

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
