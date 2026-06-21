package za.co.neroland.nerospace.registry;

import java.util.function.UnaryOperator;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.fluid.RocketFuelLiquidBlock;
import za.co.neroland.nerospace.machine.CombustionGeneratorBlock;
import za.co.neroland.nerospace.machine.FuelRefineryBlock;
import za.co.neroland.nerospace.machine.FuelTankBlock;
import za.co.neroland.nerospace.machine.NerosiumGrinderBlock;
import za.co.neroland.nerospace.machine.OxygenGeneratorBlock;
import za.co.neroland.nerospace.machine.PassiveGeneratorBlock;
import za.co.neroland.nerospace.machine.SolarPanelBlock;
import za.co.neroland.nerospace.machine.quarry.MinerTier;
import za.co.neroland.nerospace.machine.quarry.QuarryControllerBlock;
import za.co.neroland.nerospace.machine.quarry.QuarryFrameBlock;
import za.co.neroland.nerospace.machine.quarry.QuarryLandmarkBlock;
import za.co.neroland.nerospace.meteor.MeteorCoreBlock;
import za.co.neroland.nerospace.pipe.UniversalPipeBlock;
import za.co.neroland.nerospace.rocket.LaunchGantryBlock;
import za.co.neroland.nerospace.rocket.RocketLaunchPadBlock;
import za.co.neroland.nerospace.storage.CreativeBatteryBlock;
import za.co.neroland.nerospace.storage.CreativeFluidTankBlock;
import za.co.neroland.nerospace.storage.CreativeGasTankBlock;
import za.co.neroland.nerospace.storage.CreativeItemStoreBlock;
import za.co.neroland.nerospace.storage.GasTankBlock;
import za.co.neroland.nerospace.storage.TrashCanBlock;
import za.co.neroland.nerospace.storage.BatteryBlock;
import za.co.neroland.nerospace.storage.FluidTankBlock;
import za.co.neroland.nerospace.storage.ItemStoreBlock;
import za.co.neroland.nerospace.village.VillageCoreBlock;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/**
 * Block registrations shared by both loaders, registered through
 * {@link RegistrationProvider}. Properties are configured via a
 * {@link UnaryOperator} (mirrors the root project's style), so per-block
 * variations (light level, tool requirement) stay inline.
 */
public final class ModBlocks {

    public static final RegistrationProvider<Block> BLOCKS =
            RegistrationProvider.get(Registries.BLOCK, NerospaceCommon.MOD_ID);

    // --- Ores / materials ---------------------------------------------------
    public static final RegistryEntry<Block> NEROSIUM_ORE = block("nerosium_ore",
            p -> p.mapColor(MapColor.STONE).strength(3.0F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final RegistryEntry<Block> DEEPSLATE_NEROSIUM_ORE = block("deepslate_nerosium_ore",
            p -> p.mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final RegistryEntry<Block> NEROSIUM_BLOCK = block("nerosium_block",
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final RegistryEntry<Block> RAW_NEROSIUM_BLOCK = block("raw_nerosium_block",
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final RegistryEntry<Block> NEROSTEEL_ORE = block("nerosteel_ore",
            p -> p.mapColor(MapColor.STONE).strength(3.0F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final RegistryEntry<Block> XERTZ_QUARTZ_ORE = block("xertz_quartz_ore",
            p -> p.mapColor(MapColor.STONE).strength(3.0F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.NETHER_ORE));
    public static final RegistryEntry<Block> NEROSTEEL_BLOCK = block("nerosteel_block",
            p -> p.mapColor(MapColor.COLOR_GRAY).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final RegistryEntry<Block> CINDRITE_ORE = block("cindrite_ore",
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(3.5F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final RegistryEntry<Block> CINDRITE_BLOCK = block("cindrite_block",
            p -> p.mapColor(MapColor.COLOR_RED).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final RegistryEntry<Block> GLACITE_ORE = block("glacite_ore",
            p -> p.mapColor(MapColor.ICE).strength(3.5F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final RegistryEntry<Block> GLACITE_BLOCK = block("glacite_block",
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));

    // --- Station + alien decorative + meteor --------------------------------
    public static final RegistryEntry<Block> STATION_FLOOR = block("station_floor",
            p -> p.mapColor(MapColor.METAL).strength(4.0F, 12.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final RegistryEntry<Block> STATION_WALL = block("station_wall",
            p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(4.0F, 12.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final RegistryEntry<Block> ALIEN_BRICKS = block("alien_bricks",
            p -> p.mapColor(MapColor.COLOR_GREEN).strength(1.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final RegistryEntry<Block> CRACKED_ALIEN_BRICKS = block("cracked_alien_bricks",
            p -> p.mapColor(MapColor.COLOR_GREEN).strength(1.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final RegistryEntry<Block> ALIEN_TILE = block("alien_tile",
            p -> p.mapColor(MapColor.COLOR_GREEN).strength(1.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final RegistryEntry<Block> ALIEN_PILLAR = block("alien_pillar",
            p -> p.mapColor(MapColor.COLOR_GREEN).strength(1.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final RegistryEntry<Block> ALIEN_LAMP = block("alien_lamp",
            p -> p.mapColor(MapColor.COLOR_GREEN).strength(1.5F, 6.0F).lightLevel(s -> 15).sound(SoundType.METAL));
    public static final RegistryEntry<Block> ALIEN_CRYSTAL_BLOCK = block("alien_crystal_block",
            p -> p.mapColor(MapColor.EMERALD).strength(1.5F, 6.0F).lightLevel(s -> 12).sound(SoundType.AMETHYST));
    public static final RegistryEntry<VillageCoreBlock> VILLAGE_CORE = BLOCKS.register("village_core",
            key -> new VillageCoreBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.EMERALD).strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops().lightLevel(s -> 10).sound(SoundType.AMETHYST)));
    public static final RegistryEntry<Block> METEOR_ROCK = block("meteor_rock",
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(3.0F, 4.0F).requiresCorrectToolForDrops().lightLevel(s -> 3).sound(SoundType.STONE));
    /** The glowing, loot-bearing core at a crater's centre. World-generated only (no block item); breaking it spills the rolled loot. */
    public static final RegistryEntry<MeteorCoreBlock> METEOR_CORE = BLOCKS.register("meteor_core",
            key -> new MeteorCoreBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.COLOR_BLACK).strength(4.0F, 6.0F)
                    .requiresCorrectToolForDrops().lightLevel(s -> 9).sound(SoundType.AMETHYST)));

    // Block entity — item storage (pilot for the block-entity + capability seam).
    public static final RegistryEntry<ItemStoreBlock> ITEM_STORE = BLOCKS.register("item_store",
            key -> new ItemStoreBlock(BlockBehaviour.Properties.of()
                    .setId(key)
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));


    public static final RegistryEntry<BatteryBlock> BATTERY = BLOCKS.register("battery",
            key -> new BatteryBlock(BlockBehaviour.Properties.of()
                    .setId(key)
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));


    public static final RegistryEntry<FluidTankBlock> FLUID_TANK = BLOCKS.register("fluid_tank",
            key -> new FluidTankBlock(BlockBehaviour.Properties.of()
                    .setId(key)
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));


    public static final RegistryEntry<CombustionGeneratorBlock> COMBUSTION_GENERATOR = BLOCKS.register("combustion_generator",
            key -> new CombustionGeneratorBlock(BlockBehaviour.Properties.of()
                    .setId(key)
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));


    public static final RegistryEntry<NerosiumGrinderBlock> NEROSIUM_GRINDER = BLOCKS.register("nerosium_grinder",
            key -> new NerosiumGrinderBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.METAL).strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));


    public static final RegistryEntry<PassiveGeneratorBlock> PASSIVE_GENERATOR = BLOCKS.register("passive_generator",
            key -> new PassiveGeneratorBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.METAL).strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));


    public static final RegistryEntry<UniversalPipeBlock> UNIVERSAL_PIPE = BLOCKS.register("universal_pipe",
            key -> new UniversalPipeBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.METAL).strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));


    public static final RegistryEntry<TrashCanBlock> TRASH_CAN = BLOCKS.register("trash_can",
            key -> new TrashCanBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.COLOR_GRAY).strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));


    public static final RegistryEntry<CreativeBatteryBlock> CREATIVE_BATTERY = BLOCKS.register("creative_battery",
            key -> new CreativeBatteryBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.COLOR_PINK).strength(-1.0F, 3_600_000.0F)
                    .sound(SoundType.METAL).noOcclusion()));

    public static final RegistryEntry<CreativeFluidTankBlock> CREATIVE_FLUID_TANK = BLOCKS.register("creative_fluid_tank",
            key -> new CreativeFluidTankBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.COLOR_PINK).strength(-1.0F, 3_600_000.0F)
                    .sound(SoundType.METAL).noOcclusion()));

    public static final RegistryEntry<CreativeGasTankBlock> CREATIVE_GAS_TANK = BLOCKS.register("creative_gas_tank",
            key -> new CreativeGasTankBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.COLOR_PINK).strength(-1.0F, 3_600_000.0F)
                    .sound(SoundType.METAL).noOcclusion()));

    public static final RegistryEntry<CreativeItemStoreBlock> CREATIVE_ITEM_STORE = BLOCKS.register("creative_item_store",
            key -> new CreativeItemStoreBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.COLOR_PINK).strength(-1.0F, 3_600_000.0F)
                    .sound(SoundType.METAL).noOcclusion()));

    public static final RegistryEntry<GasTankBlock> GAS_TANK = BLOCKS.register("gas_tank",
            key -> new GasTankBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));

    public static final RegistryEntry<OxygenGeneratorBlock> OXYGEN_GENERATOR = BLOCKS.register("oxygen_generator",
            key -> new OxygenGeneratorBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.METAL).strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));

    public static final RegistryEntry<SolarPanelBlock> SOLAR_PANEL = BLOCKS.register("solar_panel",
            key -> new SolarPanelBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.COLOR_BLUE).strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));

    // --- Fuel machines ------------------------------------------------------
    public static final RegistryEntry<FuelTankBlock> FUEL_TANK = BLOCKS.register("fuel_tank",
            key -> new FuelTankBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.METAL).strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));

    public static final RegistryEntry<FuelRefineryBlock> FUEL_REFINERY = BLOCKS.register("fuel_refinery",
            key -> new FuelRefineryBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.METAL).strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));

    // --- Quarry -------------------------------------------------------------
    public static final RegistryEntry<QuarryControllerBlock> QUARRY_CONTROLLER = BLOCKS.register("quarry_controller",
            key -> new QuarryControllerBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.METAL).strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL), MinerTier.TIER_1));

    public static final RegistryEntry<QuarryFrameBlock> QUARRY_FRAME = BLOCKS.register("quarry_frame",
            key -> new QuarryFrameBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.METAL).strength(1.5F, 6.0F)
                    .sound(SoundType.METAL).lightLevel(s -> 7).noOcclusion().noLootTable()));

    public static final RegistryEntry<QuarryLandmarkBlock> QUARRY_LANDMARK = BLOCKS.register("quarry_landmark",
            key -> new QuarryLandmarkBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.COLOR_RED).strength(1.0F, 3.0F)
                    .sound(SoundType.METAL).lightLevel(s -> 7).noOcclusion()));

    // --- Rockets ------------------------------------------------------------
    public static final RegistryEntry<RocketLaunchPadBlock> ROCKET_LAUNCH_PAD = BLOCKS.register("rocket_launch_pad",
            key -> new RocketLaunchPadBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));

    public static final RegistryEntry<LaunchGantryBlock> LAUNCH_GANTRY = BLOCKS.register("launch_gantry",
            key -> new LaunchGantryBlock(BlockBehaviour.Properties.of()
                    .setId(key).mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));

    // Rocket fuel world block (placed by the bucket). LiquidBlock holds the source fluid, resolved
    // lazily on NeoForge / after ModFluids.init() on Fabric — hence ModFluids registers first.
    public static final RegistryEntry<RocketFuelLiquidBlock> ROCKET_FUEL_BLOCK = BLOCKS.register("rocket_fuel",
            key -> new RocketFuelLiquidBlock((FlowingFluid) ModFluids.ROCKET_FUEL.get(),
                    BlockBehaviour.Properties.of().setId(key)
                            .mapColor(MapColor.COLOR_ORANGE).replaceable().noCollision()
                            .strength(100.0F).noLootTable()));

    private static RegistryEntry<Block> block(String name, UnaryOperator<BlockBehaviour.Properties> props) {
        return BLOCKS.register(name, key -> new Block(props.apply(BlockBehaviour.Properties.of().setId(key))));
    }

    private ModBlocks() {
    }

    public static void init() {
    }
}
