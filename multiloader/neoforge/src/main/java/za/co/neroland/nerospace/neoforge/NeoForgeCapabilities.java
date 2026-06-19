package za.co.neroland.nerospace.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * NeoForge side of the item-storage capability seam. Exposes the item_store block entity's
 * inventory as {@code Capabilities.Item.BLOCK} (NeoForge 26.x transfer API: a
 * {@code ResourceHandler<ItemResource>}) so mod pipes/automation can move items in and out —
 * the counterpart to Fabric's {@code ItemStorage.SIDED}.
 */
public final class NeoForgeCapabilities {

    private NeoForgeCapabilities() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeCapabilities::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModBlockEntities.ITEM_STORE.get(),
                (be, side) -> side != null
                        ? new WorldlyContainerWrapper(be, side)
                        : VanillaContainerWrapper.of(be));
    }
}
