package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * Render state for the Universal Pipe: the item stacks physically travelling through the segment, with
 * their in-block offsets pre-computed during extraction. Entries are pooled and reused frame to frame.
 *
 * <p>Cross-loader port: a trimmed version of the standalone mod's render state — the coloured
 * energy/fluid/gas stream pulses are deferred (they rely on per-face connection blockstates the
 * multiloader pipe's single-cube model doesn't carry yet), so only the travelling-item lane is kept.</p>
 */
public class UniversalPipeRenderState extends BlockEntityRenderState {

    /** Animation clock (game time + partial tick). */
    public float time;

    /** Travelling items: pooled render states + their offsets within the block. */
    public final List<TravellingItemEntry> items = new ArrayList<>();
    public int visibleItems;

    public static final class TravellingItemEntry {
        public final ItemStackRenderState renderState = new ItemStackRenderState();
        public float x;
        public float y;
        public float z;
        public float spin;
    }

    /** Get (or grow to) the pooled entry at {@code index}. */
    public TravellingItemEntry entry(int index) {
        while (this.items.size() <= index) {
            this.items.add(new TravellingItemEntry());
        }
        return this.items.get(index);
    }
}
