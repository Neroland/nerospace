package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/**
 * Item registrations shared by both loaders. The block item for
 * {@link ModBlocks#NEROSIUM_BLOCK} — proves a second registry flows through the
 * same abstraction and that cross-entry references resolve in registry order.
 */
public final class ModItems {

    public static final RegistrationProvider<Item> ITEMS =
            RegistrationProvider.get(Registries.ITEM, NerospaceCommon.MOD_ID);

    public static final RegistryEntry<BlockItem> NEROSIUM_BLOCK_ITEM = ITEMS.register(
            "nerosium_block",
            key -> new BlockItem(ModBlocks.NEROSIUM_BLOCK.get(), new Item.Properties().setId(key)));

    private ModItems() {
    }

    /** Touch to force class-init (and thus registration). */
    public static void init() {
    }
}
