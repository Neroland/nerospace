package za.co.neroland.nerospace.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Cross-loader content registration, the Architectury way.
 *
 * <p>{@link DeferredRegister} works identically on Fabric and NeoForge, so
 * this single file replaces the root project's per-type NeoForge
 * {@code DeferredRegister.Blocks/Items/...} classes. During migration, each
 * existing registry class collapses into entries here.
 *
 * <p>The one example below proves the registration path end to end. Note
 * that vanilla constructor signatures (e.g. {@code Block} / {@code Item}
 * properties needing a registry key) drift between Minecraft versions — that
 * is exactly what Stonecutter's version comments handle on the version axis.
 */
public final class ModRegistries {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(NerospaceCommon.MOD_ID, Registries.BLOCK);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(NerospaceCommon.MOD_ID, Registries.ITEM);

    // --- example content -------------------------------------------------
    public static final RegistrySupplier<Block> NEROSIUM_BLOCK =
            BLOCKS.register("nerosium_block",
                    () -> new Block(BlockBehaviour.Properties.of().strength(3.0F)));

    public static final RegistrySupplier<Item> NEROSIUM_BLOCK_ITEM =
            ITEMS.register("nerosium_block",
                    () -> new BlockItem(NEROSIUM_BLOCK.get(), new Item.Properties()));

    private ModRegistries() {
    }

    /** Realises every DeferredRegister. Called from {@link NerospaceCommon#init()}. */
    public static void register() {
        BLOCKS.register();
        ITEMS.register();
    }
}
