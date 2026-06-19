package za.co.neroland.nerospace.neoforge;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * NeoForge side of the capability seams:
 * <ul>
 *   <li>item storage via the standard {@code Capabilities.Item.BLOCK} (26.x transfer API);</li>
 *   <li>energy via a mod-owned {@link #ENERGY} {@link BlockCapability} over
 *       {@link NerospaceEnergyStorage} (the Fabric side uses a matching {@code BlockApiLookup}) —
 *       self-contained until the platforms' energy libraries port to 26.x.</li>
 * </ul>
 */
public final class NeoForgeCapabilities {

    /** Mod-owned energy capability; mirrors the Fabric {@code BlockApiLookup} of the same id. */
    public static final BlockCapability<NerospaceEnergyStorage, Direction> ENERGY =
            BlockCapability.createSided(
                    Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "energy"),
                    NerospaceEnergyStorage.class);

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

        event.registerBlockEntity(
                ENERGY,
                ModBlockEntities.BATTERY.get(),
                (be, side) -> be.getEnergy());
    }
}
