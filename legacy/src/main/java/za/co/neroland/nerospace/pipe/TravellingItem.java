package za.co.neroland.nerospace.pipe;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.item.ItemResource;

import org.jetbrains.annotations.Nullable;

/**
 * One stack of items physically travelling through a Universal Pipe segment. {@code from} is the face
 * it entered through, {@code to} the face it is heading for ({@code null} = parked, waiting for a
 * route), {@code progress} runs 0 → 1 across the segment. Owned (and persisted) by the pipe block
 * entity the items are currently inside, so packets survive saves and network rebuilds; the network
 * advances, routes and hands them off each tick. Synced to clients so the renderer can draw the items.
 */
public final class TravellingItem {

    public static final Codec<TravellingItem> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemResource.CODEC.fieldOf("item").forGetter(t -> t.resource),
            Codec.INT.fieldOf("amount").forGetter(t -> t.amount),
            Direction.CODEC.fieldOf("from").forGetter(t -> t.from),
            Direction.CODEC.optionalFieldOf("to").forGetter(t -> Optional.ofNullable(t.to)),
            Codec.FLOAT.fieldOf("progress").forGetter(t -> t.progress))
            .apply(i, (resource, amount, from, to, progress) ->
                    new TravellingItem(resource, amount, from, to.orElse(null), progress)));

    private ItemResource resource;
    private int amount;
    private Direction from;
    @Nullable
    private Direction to;
    private float progress;

    public TravellingItem(ItemResource resource, int amount, Direction from, @Nullable Direction to, float progress) {
        this.resource = resource;
        this.amount = amount;
        this.from = from;
        this.to = to;
        this.progress = progress;
    }

    public ItemResource resource() {
        return this.resource;
    }

    public int amount() {
        return this.amount;
    }

    public Direction from() {
        return this.from;
    }

    @Nullable
    public Direction to() {
        return this.to;
    }

    public float progress() {
        return this.progress;
    }

    public boolean isParked() {
        return this.to == null;
    }

    void shrink(int by) {
        this.amount -= by;
    }

    /** Move along the segment (also used client-side for smooth motion between syncs). */
    public void advance(float by) {
        this.progress = Math.min(1.0F, this.progress + by);
    }

    /** Re-aim at a new exit face, restarting the run from the face it was at. */
    void redirect(Direction newFrom, @Nullable Direction newTo, float newProgress) {
        this.from = newFrom;
        this.to = newTo;
        this.progress = newProgress;
    }
}
