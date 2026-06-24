package za.co.neroland.nerospace.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * Render state for the Universal Pipe: the item stacks physically travelling through the segment, with
 * their in-block offsets pre-computed during extraction. Entries are pooled and reused frame to frame.
 *
 * <p>Cross-loader port of the standalone mod's render state — both the travelling-item lane and the
 * coloured energy/fluid/gas stream pulses (the latter now that the pipe carries per-face connection
 * blockstates).</p>
 */
public class UniversalPipeRenderState extends BlockEntityRenderState {

    /** Animation clock (game time + partial tick). */
    public float time;

    /** Connected arms by {@code Direction.get3DDataValue()}. */
    public final boolean[] connections = new boolean[6];

    /**
     * Stream activity per direction (6) and stream layer (0=energy, 1=fluid, 2=gas): the arm is
     * connected, the face's mode allows flow and the pipe holds that resource.
     */
    public final boolean[][] streams = new boolean[6][3];
    /** Stream flows toward the pipe centre (face mode IN) instead of outward. */
    public final boolean[][] inward = new boolean[6][3];
    /** ARGB colour per stream layer (energy red, fluid blue, gas cyan). */
    public final int[] streamColors = new int[3];

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
