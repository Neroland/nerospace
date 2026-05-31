package za.co.neroland.nerospace.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.NerosiumGrinderBlock;
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

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
