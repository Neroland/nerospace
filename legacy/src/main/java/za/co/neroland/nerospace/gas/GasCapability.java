package za.co.neroland.nerospace.gas;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.transfer.ResourceHandler;

import za.co.neroland.nerospace.Nerospace;

/**
 * The dedicated <b>gas</b> block capability: any block entity exposing a
 * {@code ResourceHandler<GasResource>} here can be tapped by the Universal Pipe's gas layer (and by
 * other mods that look the capability up by its id, {@code nerospace:gas_handler}).
 */
public final class GasCapability {

    @SuppressWarnings("unchecked")
    public static final BlockCapability<ResourceHandler<GasResource>, Direction> BLOCK =
            BlockCapability.createSided(
                    Identifier.fromNamespaceAndPath(Nerospace.MODID, "gas_handler"),
                    (Class<ResourceHandler<GasResource>>) (Class<?>) ResourceHandler.class);

    private GasCapability() {
    }
}
