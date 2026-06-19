package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/**
 * Block registrations shared by both loaders. First migrated slice: a single
 * storage block, registered through {@link RegistrationProvider} so the exact
 * same definition drives Fabric and NeoForge.
 */
public final class ModBlocks {

    public static final RegistrationProvider<Block> BLOCKS =
            RegistrationProvider.get(Registries.BLOCK, NerospaceCommon.MOD_ID);

    public static final RegistryEntry<Block> NEROSIUM_BLOCK = BLOCKS.register(
            "nerosium_block",
            key -> new Block(BlockBehaviour.Properties.of()
                    .setId(key)
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    private ModBlocks() {
    }

    /** Touch to force class-init (and thus registration). */
    public static void init() {
    }
}
