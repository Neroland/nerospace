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
import za.co.neroland.nerospace.machine.FuelTankBlock;
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

    // --- Machines (Phase 2) -------------------------------------------------

    public static final DeferredBlock<NerosiumGrinderBlock> NEROSIUM_GRINDER = BLOCKS.registerBlock(
            "nerosium_grinder",
            NerosiumGrinderBlock::new,
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    // --- Rockets (Phase 4) --------------------------------------------------

    /** The launch mount: a short metal pad a rocket is deployed onto before launch. */
    public static final DeferredBlock<RocketLaunchPadBlock> ROCKET_LAUNCH_PAD = BLOCKS.registerBlock(
            "rocket_launch_pad",
            RocketLaunchPadBlock::new,
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

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
                    .sound(SoundType.METAL));

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
                    .sound(SoundType.METAL));

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
                    .sound(SoundType.METAL));

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
                    .sound(SoundType.METAL));

    /** Passive Generator: trickles energy from a nerosium core. */
    public static final DeferredBlock<PassiveGeneratorBlock> PASSIVE_GENERATOR = BLOCKS.registerBlock(
            "passive_generator",
            PassiveGeneratorBlock::new,
            props -> props
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    // --- Storage endpoints (battery / tanks / item store + creative sources) ---

    public static final DeferredBlock<za.co.neroland.nerospace.storage.BatteryBlock> BATTERY =
            BLOCKS.registerBlock("battery", za.co.neroland.nerospace.storage.BatteryBlock::new,
                    props -> props.mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL));

    public static final DeferredBlock<za.co.neroland.nerospace.storage.CreativeBatteryBlock> CREATIVE_BATTERY =
            BLOCKS.registerBlock("creative_battery", za.co.neroland.nerospace.storage.CreativeBatteryBlock::new,
                    props -> props.mapColor(MapColor.COLOR_PINK).strength(-1.0F, 3_600_000.0F)
                            .sound(SoundType.METAL));

    public static final DeferredBlock<za.co.neroland.nerospace.storage.FluidTankBlock> FLUID_TANK =
            BLOCKS.registerBlock("fluid_tank", za.co.neroland.nerospace.storage.FluidTankBlock::new,
                    props -> props.mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL));

    public static final DeferredBlock<za.co.neroland.nerospace.storage.CreativeFluidTankBlock> CREATIVE_FLUID_TANK =
            BLOCKS.registerBlock("creative_fluid_tank", za.co.neroland.nerospace.storage.CreativeFluidTankBlock::new,
                    props -> props.mapColor(MapColor.COLOR_PINK).strength(-1.0F, 3_600_000.0F)
                            .sound(SoundType.METAL));

    public static final DeferredBlock<za.co.neroland.nerospace.storage.GasTankBlock> GAS_TANK =
            BLOCKS.registerBlock("gas_tank", za.co.neroland.nerospace.storage.GasTankBlock::new,
                    props -> props.mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL));

    public static final DeferredBlock<za.co.neroland.nerospace.storage.CreativeGasTankBlock> CREATIVE_GAS_TANK =
            BLOCKS.registerBlock("creative_gas_tank", za.co.neroland.nerospace.storage.CreativeGasTankBlock::new,
                    props -> props.mapColor(MapColor.COLOR_PINK).strength(-1.0F, 3_600_000.0F)
                            .sound(SoundType.METAL));

    public static final DeferredBlock<za.co.neroland.nerospace.storage.ItemStoreBlock> ITEM_STORE =
            BLOCKS.registerBlock("item_store", za.co.neroland.nerospace.storage.ItemStoreBlock::new,
                    props -> props.mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL));

    public static final DeferredBlock<za.co.neroland.nerospace.storage.CreativeItemStoreBlock> CREATIVE_ITEM_STORE =
            BLOCKS.registerBlock("creative_item_store", za.co.neroland.nerospace.storage.CreativeItemStoreBlock::new,
                    props -> props.mapColor(MapColor.COLOR_PINK).strength(-1.0F, 3_600_000.0F)
                            .sound(SoundType.METAL));

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

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
