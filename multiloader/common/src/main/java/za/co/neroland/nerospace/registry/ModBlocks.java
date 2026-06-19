package za.co.neroland.nerospace.registry;

import java.util.function.UnaryOperator;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.storage.BatteryBlock;
import za.co.neroland.nerospace.storage.FluidTankBlock;
import za.co.neroland.nerospace.storage.ItemStoreBlock;
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
    public static final RegistryEntry<Block> METEOR_ROCK = block("meteor_rock",
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(3.0F, 4.0F).requiresCorrectToolForDrops().lightLevel(s -> 3).sound(SoundType.STONE));

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

    private static RegistryEntry<Block> block(String name, UnaryOperator<BlockBehaviour.Properties> props) {
        return BLOCKS.register(name, key -> new Block(props.apply(BlockBehaviour.Properties.of().setId(key))));
    }

    private ModBlocks() {
    }

    public static void init() {
    }
}
