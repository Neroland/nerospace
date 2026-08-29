package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.item.NerospaceItemStore;

/**
 * NeoForge query of the PLATFORM-STANDARD item handler, {@code Capabilities.Item.BLOCK}.
 *
 * <p>There is no mod-private item capability to try first: every Nerospace inventory is a vanilla
 * {@link net.minecraft.world.Container}, and both callers of this seam — {@code PipeNetwork.itemStore}
 * and {@code UniversalPipeBlock.canConnect} — take the container path before they ever ask here. What
 * reaches this class is therefore a neighbour that is NOT a container, i.e. exactly the foreign machine
 * the Universal Pipe used to refuse to connect to.</p>
 */
public final class NeoForgeItemLookup implements ItemLookup {

    @Nullable
    @Override
    public NerospaceItemStore find(Level level, BlockPos pos, @Nullable Direction side) {
        // Five-arg query: the null state/block-entity let NeoForge resolve them itself.
        ResourceHandler<ItemResource> handler =
                Capabilities.Item.BLOCK.getCapability(level, pos, null, null, side);
        return NeoForgeItemStoreAdapter.of(handler);
    }
}
