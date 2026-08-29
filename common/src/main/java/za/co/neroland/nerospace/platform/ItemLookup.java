package za.co.neroland.nerospace.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.item.NerospaceItemStore;

/**
 * Query side of the item seam: find the PLATFORM-STANDARD item handler exposed by the block at
 * {@code pos} on {@code side}, adapted to {@link NerospaceItemStore}.
 *
 * <p>Unlike {@link FluidLookup}/{@link GasLookup}, this seam is not about Nerospace's own blocks — every
 * Nerospace inventory is already a vanilla {@link net.minecraft.world.Container} and is found without
 * it. It exists purely for CROSS-MOD reach: a neighbour that exposes only NeoForge
 * {@code Capabilities.Item.BLOCK}, Fabric {@code ItemStorage.SIDED} or Forge {@code ITEM_HANDLER} is
 * invisible to a {@code Container}-only pipe, which is why the Universal Pipe used to refuse to connect
 * to other mods' machines. Implementations should return {@code null} when the block is a plain vanilla
 * container, so the container path stays authoritative.</p>
 *
 * <p>Resolved via {@link Services}.</p>
 */
public interface ItemLookup {

    ItemLookup INSTANCE = Services.load(ItemLookup.class);

    @Nullable
    NerospaceItemStore find(Level level, BlockPos pos, @Nullable Direction side);
}
