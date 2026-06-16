package za.co.neroland.nerospace.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.machine.CombustionGeneratorBlock;
import za.co.neroland.nerospace.machine.FuelRefineryBlock;
import za.co.neroland.nerospace.machine.FuelTankBlock;
import za.co.neroland.nerospace.machine.HydrationModuleBlock;
import za.co.neroland.nerospace.machine.TerraformMonitorBlock;
import za.co.neroland.nerospace.machine.NerosiumGrinderBlock;
import za.co.neroland.nerospace.machine.OxygenGeneratorBlock;
import za.co.neroland.nerospace.machine.PassiveGeneratorBlock;
import za.co.neroland.nerospace.machine.TerraformerBlock;
import za.co.neroland.nerospace.pipe.UniversalPipeBlock;
import za.co.neroland.nerospace.rocket.RocketLaunchPadBlock;

/**
 * Central block registry for Nerospace (Phase 1 — materials slice).
 *
 * <p>Properties are mutated through the {@code UnaryOperator<Properties>} passed to the
 * {@code registerSimpleBlock} helpers; the block id is set automatically by
 * {@link DeferredRegister.Blocks}.</p>
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nerospace.MODID);

    // --- Ores ---------------------------------------------------------------

    public static final DeferredBlock<Block> NEROSIUM_ORE = BLOCKS.registerSimpleBlock(
            "nerosium_ore",
            props -> props
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE));

    public static final DeferredBlock<Block> DEEPSLATE_NEROSIUM_ORE = BLOCKS.registerSimpleBlock(
            "deepslate_nerosium_ore",
            props -> props
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE));

    // --- Storage blocks -----------------------------------------------------

    public static final DeferredBlock<Block> NEROSIUM_BLOCK = BLOCKS.registerSimpleBlock(
            "nerosium_block",
            props -> props
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    public static final DeferredBlock<Block> RAW_NEROSIUM_BLOCK = BLOCKS.registerSimpleBlock(
            "raw_nerosium_block",
            props -> props
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    // --- Greenxertz dimension ores (Phase 3) --------------------------------

    /**
     * Nerosteel ore — the planet's primary metal source. Iron-tier like nerosium; drops raw
     * nerosteel which smelts to an ingot. Spawns in the Greenxertz biome (stone + deepslate).
     */
    public static final DeferredBlock<Block> NEROSTEEL_ORE = BLOCKS.registerSimpleBlock(
            "nerosteel_ore",
            props -> props
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE));

    /**
     * Xertz quartz ore — behaves like nether quartz (drops the gem directly), but is its own
     * material with room to grow. Any-tier pickaxe mines it.
     */
    public static final DeferredBlock<Block> XERTZ_QUARTZ_ORE = BLOCKS.registerSimpleBlock(
            "xertz_quartz_ore",
            props -> props
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHER_ORE));

    /** Storage block for nerosteel ingots. */
    public static final DeferredBlock<Block> NEROSTEEL_BLOCK = BLOCKS.registerSimpleBlock(
            "nerosteel_block",
            props -> props
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    // --- Cindara dimension (Phase 7) ----------------------------------------

    /**
     * Cindrite ore — the volcanic moon Cindara's signature crystal. Drops the {@code cindrite} gem
     * directly (fortune-affected), iron-tier. Hosted in stone/deepslate like the Greenxertz ores.
     */
    public static final DeferredBlock<Block> CINDRITE_ORE = BLOCKS.registerSimpleBlock(
            "cindrite_ore",
            props -> props
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.5F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE));

    /** Storage block for cindrite gems. */
    public static final DeferredBlock<Block> CINDRITE_BLOCK = BLOCKS.registerSimpleBlock(
            "cindrite_block",
            props -> props
                    .mapColor(MapColor.COLOR_RED)
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    // --- Glacira dimension (NEW_DESTINATION_DESIGN.md) -----------------------

    /**
     * Glacite ore — the frozen moon Glacira's signature crystal. Drops the {@code glacite} gem
     * directly (fortune-affected), iron-tier. Hosted in stone/deepslate like the other planet ores.
     */
    public static final DeferredBlock<Block> GLACITE_ORE = BLOCKS.registerSimpleBlock(
            "glacite_ore",
            props -> props
                    .mapColor(MapColor.ICE)
                    .strength(3.5F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE));

    /** Storage block for glacite gems. */
    public static final DeferredBlock<Block> GLACITE_BLOCK = BLOCKS.registerSimpleBlock(
            "glacite_block",
            props -> props
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    // --- Orbital station building blocks (Phase 7c) -------------------------

    /** Station floor plating — the landing platform is built from this; craft more to expand. */
    public static final DeferredBlock<Block> STATION_FLOOR = BLOCKS.registerSimpleBlock(
            "station_floor",
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 12.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    /** Station wall/hull panelling. */
    public static final DeferredBlock<Block> STATION_WALL = BLOCKS.registerSimpleBlock(
            "station_wall",
            props -> props
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(4.0F, 12.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    /**
     * Station Core (MULTI_STATION_DESIGN.md): the anchor of a player-founded station. Placed only
     * by the founding flow (no recipe, no loot table — breaking pops a named Station Charter via
     * the block entity and unregisters the station).
     */
    public static final DeferredBlock<za.co.neroland.nerospace.rocket.StationCoreBlock> STATION_CORE =
            BLOCKS.registerBlock("station_core", za.co.neroland.nerospace.rocket.StationCoreBlock::new,
                    props -> props
                            .mapColor(MapColor.COLOR_CYAN)
                            .strength(4.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noLootTable());

    // --- Machines (Phase 2) -------------------------------------------------

    public static final DeferredBlock<NerosiumGrinderBlock> NEROSIUM_GRINDER = BLOCKS.registerBlock(
            "nerosium_grinder",
            NerosiumGrinderBlock::new,
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    // Shaped model (art overhaul §3): without this the renderer culls the
                    // faces of blocks behind the model's gaps and you see through the world.
                    .noOcclusion());

    // --- Rockets (Phase 4) --------------------------------------------------

    /** The launch mount: a short metal pad a rocket is deployed onto before launch. */
    public static final DeferredBlock<RocketLaunchPadBlock> ROCKET_LAUNCH_PAD = BLOCKS.registerBlock(
            "rocket_launch_pad",
            RocketLaunchPadBlock::new,
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion());   // 3px plate model — don't cull neighbours' faces

    /**
     * Launch Gantry module (LAUNCH_PAD_DESIGN.md): on a 5x5 pad's border ring it forms the Heavy
     * Launch Complex; right-click boards the rocket on the pad.
     */
    public static final DeferredBlock<za.co.neroland.nerospace.rocket.LaunchGantryBlock> LAUNCH_GANTRY =
            BLOCKS.registerBlock("launch_gantry", za.co.neroland.nerospace.rocket.LaunchGantryBlock::new,
                    props -> props
                            .mapColor(MapColor.METAL)
                            .strength(3.5F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()); // open tower model (art overhaul §3)

    /**
     * Fuel Tank (Phase 8a): a fuel-storage machine that auto-fuels a rocket on an adjacent launch
     * pad. Backed by {@link za.co.neroland.nerospace.machine.FuelTankBlockEntity}.
     */
    public static final DeferredBlock<FuelTankBlock> FUEL_TANK = BLOCKS.registerBlock(
            "fuel_tank",
            FuelTankBlock::new,
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()); // frame-and-core tank model (art overhaul §3)

    /**
     * Fuel Refinery (BALANCE_COMPAT_AUDIT.md §3): refines coal + blaze powder + grid energy into
     * pipeable liquid rocket fuel — the logistics-grade fuel source. Backed by
     * {@link za.co.neroland.nerospace.machine.FuelRefineryBlockEntity}.
     */
    public static final DeferredBlock<FuelRefineryBlock> FUEL_REFINERY = BLOCKS.registerBlock(
            "fuel_refinery",
            FuelRefineryBlock::new,
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()); // shaped body model

    /**
     * Oxygen Generator (Phase 8c): a machine that projects a breathable bubble while powered. Backed
     * by {@link za.co.neroland.nerospace.machine.OxygenGeneratorBlockEntity}.
     */
    public static final DeferredBlock<OxygenGeneratorBlock> OXYGEN_GENERATOR = BLOCKS.registerBlock(
            "oxygen_generator",
            OxygenGeneratorBlock::new,
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()); // domed model (art overhaul §3)

    /**
     * Terraformer (terraform design §2): a machine that advances an expanding terrain-conversion
     * frontier while powered. Backed by {@link za.co.neroland.nerospace.machine.TerraformerBlockEntity}.
     */
    public static final DeferredBlock<TerraformerBlock> TERRAFORMER = BLOCKS.registerBlock(
            "terraformer",
            TerraformerBlock::new,
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()); // tray-and-body model (art overhaul §3)

    /**
     * Hydration Module (DEEPER_TERRAFORM_DESIGN.md §3.1): melts glacite into hydration units for a
     * TOUCHING Terraformer's water stage. Backed by
     * {@link za.co.neroland.nerospace.machine.HydrationModuleBlockEntity}.
     */
    public static final DeferredBlock<HydrationModuleBlock> HYDRATION_MODULE = BLOCKS.registerBlock(
            "hydration_module",
            HydrationModuleBlock::new,
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()); // windowed model (art overhaul §3)

    /**
     * Terraform Monitor (DEEPER_TERRAFORM_DESIGN.md §6): stage/radii/stall readout; comparator =
     * local terraform stage. Backed by
     * {@link za.co.neroland.nerospace.machine.TerraformMonitorBlockEntity}.
     */
    public static final DeferredBlock<TerraformMonitorBlock> TERRAFORM_MONITOR = BLOCKS.registerBlock(
            "terraform_monitor",
            TerraformMonitorBlock::new,
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()); // pedestal-screen model (art overhaul §3)

    // --- Quarry / Miner (MINER_DESIGN) --------------------------------------

    /**
     * Quarry Controller (Tier 1): the brain of the miner. Marks out a region with landmarks, builds a
     * frame and excavates it layer-by-layer. Backed by
     * {@link za.co.neroland.nerospace.machine.quarry.QuarryControllerBlockEntity}.
     */
    public static final DeferredBlock<za.co.neroland.nerospace.machine.quarry.QuarryControllerBlock> QUARRY_CONTROLLER =
            BLOCKS.registerBlock("quarry_controller",
                    za.co.neroland.nerospace.machine.quarry.QuarryControllerBlock::new,
                    props -> props
                            .mapColor(MapColor.METAL)
                            .strength(3.5F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion());

    /** Quarry Landmark: a corner marker; three forming an L define the region to mine. */
    public static final DeferredBlock<za.co.neroland.nerospace.machine.quarry.QuarryLandmarkBlock> QUARRY_LANDMARK =
            BLOCKS.registerBlock("quarry_landmark",
                    za.co.neroland.nerospace.machine.quarry.QuarryLandmarkBlock::new,
                    props -> props
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(0.8F, 1.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion());

    /**
     * Quarry Frame: the glowing structural ring the controller materialises around its region.
     * Machine-placed only (no block item), drops nothing ({@code noLootTable}); broken when the
     * controller is removed.
     */
    public static final DeferredBlock<za.co.neroland.nerospace.machine.quarry.QuarryFrameBlock> QUARRY_FRAME =
            BLOCKS.registerBlock("quarry_frame",
                    za.co.neroland.nerospace.machine.quarry.QuarryFrameBlock::new,
                    props -> props
                            .mapColor(MapColor.COLOR_CYAN)
                            .strength(1.0F, 1.0F)
                            .sound(SoundType.METAL)
                            .lightLevel(state -> 10)
                            .noOcclusion()
                            .noLootTable());

    // --- Power grid (Universal Pipe + generators) ---------------------------

    /** The Universal Pipe: a connection-aware transmitter that moves energy (and later items/fluids/gas). */
    public static final DeferredBlock<UniversalPipeBlock> UNIVERSAL_PIPE = BLOCKS.registerBlock(
            "universal_pipe",
            UniversalPipeBlock::new,
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion());

    /** Combustion Generator: burns fuel into energy for the grid. */
    public static final DeferredBlock<CombustionGeneratorBlock> COMBUSTION_GENERATOR = BLOCKS.registerBlock(
            "combustion_generator",
            CombustionGeneratorBlock::new,
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()); // chimney model (art overhaul §3)

    /** Passive Generator: trickles energy from a nerosium core. */
    public static final DeferredBlock<PassiveGeneratorBlock> PASSIVE_GENERATOR = BLOCKS.registerBlock(
            "passive_generator",
            PassiveGeneratorBlock::new,
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()); // pedestal-and-panel model (art overhaul §3)

    /**
     * Tier 1 Solar Panel: a sun-tracking generator that pools with adjacent same-tier panels into one
     * array (SOLAR_PANEL_DESIGN). noOcclusion — its flat housing must not cull neighbours, and the
     * tilting deck is renderer-drawn above the block.
     */
    public static final DeferredBlock<za.co.neroland.nerospace.solar.SolarPanelBlock> SOLAR_PANEL_T1 =
            BLOCKS.registerBlock("solar_panel_t1",
                    props -> new za.co.neroland.nerospace.solar.SolarPanelBlock(
                            za.co.neroland.nerospace.solar.SolarTier.TIER_1, props),
                    props -> props
                            .mapColor(MapColor.METAL)
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion());

    /** Tier 2 Solar Panel: a 2x2 multiblock array (placing one item fills the footprint). */
    public static final DeferredBlock<za.co.neroland.nerospace.solar.SolarPanelBlock> SOLAR_PANEL_T2 =
            BLOCKS.registerBlock("solar_panel_t2",
                    props -> new za.co.neroland.nerospace.solar.SolarPanelBlock(
                            za.co.neroland.nerospace.solar.SolarTier.TIER_2, props),
                    props -> props
                            .mapColor(MapColor.METAL)
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion());

    /** Tier 3 Solar Panel: a 3x3 multiblock array (placing one item fills the footprint). */
    public static final DeferredBlock<za.co.neroland.nerospace.solar.SolarPanelBlock> SOLAR_PANEL_T3 =
            BLOCKS.registerBlock("solar_panel_t3",
                    props -> new za.co.neroland.nerospace.solar.SolarPanelBlock(
                            za.co.neroland.nerospace.solar.SolarTier.TIER_3, props),
                    props -> props
                            .mapColor(MapColor.METAL)
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion());

    // --- Storage endpoints (battery / tanks / item store + creative sources) ---

    // All storage endpoints carry shaped models (art overhaul §3) — noOcclusion stops the renderer
    // culling the world behind their frames/caps.
    public static final DeferredBlock<za.co.neroland.nerospace.storage.BatteryBlock> BATTERY =
            BLOCKS.registerBlock("battery", za.co.neroland.nerospace.storage.BatteryBlock::new,
                    props -> props.mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion());

    public static final DeferredBlock<za.co.neroland.nerospace.storage.CreativeBatteryBlock> CREATIVE_BATTERY =
            BLOCKS.registerBlock("creative_battery", za.co.neroland.nerospace.storage.CreativeBatteryBlock::new,
                    props -> props.mapColor(MapColor.COLOR_PINK).strength(-1.0F, 3_600_000.0F)
                            .sound(SoundType.METAL).noOcclusion());

    public static final DeferredBlock<za.co.neroland.nerospace.storage.FluidTankBlock> FLUID_TANK =
            BLOCKS.registerBlock("fluid_tank", za.co.neroland.nerospace.storage.FluidTankBlock::new,
                    props -> props.mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion());

    public static final DeferredBlock<za.co.neroland.nerospace.storage.CreativeFluidTankBlock> CREATIVE_FLUID_TANK =
            BLOCKS.registerBlock("creative_fluid_tank", za.co.neroland.nerospace.storage.CreativeFluidTankBlock::new,
                    props -> props.mapColor(MapColor.COLOR_PINK).strength(-1.0F, 3_600_000.0F)
                            .sound(SoundType.METAL).noOcclusion());

    public static final DeferredBlock<za.co.neroland.nerospace.storage.GasTankBlock> GAS_TANK =
            BLOCKS.registerBlock("gas_tank", za.co.neroland.nerospace.storage.GasTankBlock::new,
                    props -> props.mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion());

    public static final DeferredBlock<za.co.neroland.nerospace.storage.CreativeGasTankBlock> CREATIVE_GAS_TANK =
            BLOCKS.registerBlock("creative_gas_tank", za.co.neroland.nerospace.storage.CreativeGasTankBlock::new,
                    props -> props.mapColor(MapColor.COLOR_PINK).strength(-1.0F, 3_600_000.0F)
                            .sound(SoundType.METAL).noOcclusion());

    public static final DeferredBlock<za.co.neroland.nerospace.storage.ItemStoreBlock> ITEM_STORE =
            BLOCKS.registerBlock("item_store", za.co.neroland.nerospace.storage.ItemStoreBlock::new,
                    props -> props.mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion());

    public static final DeferredBlock<za.co.neroland.nerospace.storage.CreativeItemStoreBlock> CREATIVE_ITEM_STORE =
            BLOCKS.registerBlock("creative_item_store", za.co.neroland.nerospace.storage.CreativeItemStoreBlock::new,
                    props -> props.mapColor(MapColor.COLOR_PINK).strength(-1.0F, 3_600_000.0F)
                            .sound(SoundType.METAL));

    /** Trash Can: voids any item / fluid / gas piped into it. */
    public static final DeferredBlock<za.co.neroland.nerospace.storage.TrashCanBlock> TRASH_CAN =
            BLOCKS.registerBlock("trash_can", za.co.neroland.nerospace.storage.TrashCanBlock::new,
                    props -> props.mapColor(MapColor.COLOR_GRAY).strength(2.0F, 6.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion());

    // --- Star Guide (progression block, 1.0) ---------------------------------

    /**
     * The Star Guide pedestal: holds the Star Guide Book and shows the interactive progression
     * tree (see {@code STAR_GUIDE_DESIGN.md}). Deliberately cheap/early — it's the tutorial block.
     */
    public static final DeferredBlock<za.co.neroland.nerospace.progression.StarGuideBlock> STAR_GUIDE =
            BLOCKS.registerBlock("star_guide", za.co.neroland.nerospace.progression.StarGuideBlock::new,
                    props -> props
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)
                            .noOcclusion()); // pedestal model (art overhaul §3)

    // --- Rocket Fuel liquid block (Phase 7b) --------------------------------

    /** The world block for the {@code rocket_fuel} fluid (placed by its bucket). */
    public static final DeferredBlock<LiquidBlock> ROCKET_FUEL_BLOCK = BLOCKS.registerBlock(
            "rocket_fuel",
            props -> new LiquidBlock(ModFluids.ROCKET_FUEL.get(), props),
            props -> props
                    .mapColor(MapColor.COLOR_ORANGE)
                    .replaceable()
                    .noCollision()
                    .strength(100.0F)
                    .noLootTable());

    // --- Meteor events (meteor-events-design.md) ----------------------------

    /**
     * Meteor Rock: the charred crater body left by an impact. A mineable space-rock building block
     * (any pickaxe), faintly glowing with alien heat. Drops itself.
     */
    public static final DeferredBlock<Block> METEOR_ROCK = BLOCKS.registerSimpleBlock(
            "meteor_rock",
            props -> props
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.0F, 4.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 3)
                    .sound(SoundType.STONE));

    /**
     * Meteor Core: the loot-bearing block at a crater's centre (meteor-events design §5). Backed by
     * {@link za.co.neroland.nerospace.meteor.MeteorCoreBlockEntity}; break-to-loot, so it has no loot
     * table — the rolled contents spill from the block entity on removal.
     */
    public static final DeferredBlock<za.co.neroland.nerospace.meteor.MeteorCoreBlock> METEOR_CORE =
            BLOCKS.registerBlock("meteor_core", za.co.neroland.nerospace.meteor.MeteorCoreBlock::new,
                    props -> props
                            .mapColor(MapColor.COLOR_CYAN)
                            .strength(4.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .lightLevel(state -> 10)
                            .sound(SoundType.METAL)
                            .noLootTable());

    // --- Developer diagnostics ----------------------------------------------

    /**
     * Sentry test block: a hidden diagnostic, deliberately kept OUT of the creative menu (no
     * {@code displayItems} entry) so it is only obtainable via {@code /give nerospace:sentry_test}.
     * Placing it fires one synthetic Sentry event to verify error reporting end to end. Backed by
     * {@link za.co.neroland.nerospace.telemetry.SentryTestBlock}.
     */
    public static final DeferredBlock<za.co.neroland.nerospace.telemetry.SentryTestBlock> SENTRY_TEST =
            BLOCKS.registerBlock("sentry_test", za.co.neroland.nerospace.telemetry.SentryTestBlock::new,
                    props -> props
                            .mapColor(MapColor.COLOR_RED)
                            .strength(1.0F, 1.0F)
                            .sound(SoundType.METAL));

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
