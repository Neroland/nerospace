package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/**
 * Block registrations shared by both loaders (ore / material families),
 * registered through {@link RegistrationProvider}. All entries are plain
 * {@link Block}s with {@code requiresCorrectToolForDrops} (see the block tags in
 * data/minecraft/tags/block for the mining tier).
 */
public final class ModBlocks {

    public static final RegistrationProvider<Block> BLOCKS =
            RegistrationProvider.get(Registries.BLOCK, NerospaceCommon.MOD_ID);

    // Nerosium
    public static final RegistryEntry<Block> NEROSIUM_ORE =
            simple("nerosium_ore", MapColor.STONE, 3.0F, 3.0F, SoundType.STONE);
    public static final RegistryEntry<Block> DEEPSLATE_NEROSIUM_ORE =
            simple("deepslate_nerosium_ore", MapColor.DEEPSLATE, 4.5F, 3.0F, SoundType.DEEPSLATE);
    public static final RegistryEntry<Block> NEROSIUM_BLOCK =
            simple("nerosium_block", MapColor.COLOR_LIGHT_BLUE, 5.0F, 6.0F, SoundType.METAL);
    public static final RegistryEntry<Block> RAW_NEROSIUM_BLOCK =
            simple("raw_nerosium_block", MapColor.COLOR_LIGHT_BLUE, 5.0F, 6.0F, SoundType.METAL);

    // Greenxertz (nerosteel + xertz quartz)
    public static final RegistryEntry<Block> NEROSTEEL_ORE =
            simple("nerosteel_ore", MapColor.STONE, 3.0F, 3.0F, SoundType.STONE);
    public static final RegistryEntry<Block> XERTZ_QUARTZ_ORE =
            simple("xertz_quartz_ore", MapColor.STONE, 3.0F, 3.0F, SoundType.NETHER_ORE);
    public static final RegistryEntry<Block> NEROSTEEL_BLOCK =
            simple("nerosteel_block", MapColor.COLOR_GRAY, 5.0F, 6.0F, SoundType.METAL);

    // Cindara
    public static final RegistryEntry<Block> CINDRITE_ORE =
            simple("cindrite_ore", MapColor.COLOR_BLACK, 3.5F, 3.0F, SoundType.STONE);
    public static final RegistryEntry<Block> CINDRITE_BLOCK =
            simple("cindrite_block", MapColor.COLOR_RED, 5.0F, 6.0F, SoundType.METAL);

    // Glacira
    public static final RegistryEntry<Block> GLACITE_ORE =
            simple("glacite_ore", MapColor.ICE, 3.5F, 3.0F, SoundType.STONE);
    public static final RegistryEntry<Block> GLACITE_BLOCK =
            simple("glacite_block", MapColor.COLOR_LIGHT_BLUE, 5.0F, 6.0F, SoundType.METAL);

    private static RegistryEntry<Block> simple(String name, MapColor color, float hardness, float resistance, SoundType sound) {
        return BLOCKS.register(name, key -> new Block(BlockBehaviour.Properties.of()
                .setId(key)
                .mapColor(color)
                .strength(hardness, resistance)
                .requiresCorrectToolForDrops()
                .sound(sound)));
    }

    private ModBlocks() {
    }

    public static void init() {
    }
}
