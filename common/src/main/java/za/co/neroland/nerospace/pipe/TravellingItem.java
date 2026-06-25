package za.co.neroland.nerospace.pipe;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * One stack of items visibly travelling through a Universal Pipe segment (the cosmetic half of the
 * advanced-pipes slice). {@code from} is the face it entered through, {@code to} the face it heads for
 * ({@code null} = parked at centre), {@code progress} runs 0&nbsp;&rarr;&nbsp;1 across the segment.
 *
 * <p>Cross-loader port: rebuilt on a plain vanilla {@link ItemStack} (the standalone mod used the
 * NeoForge {@code ItemResource}, which is not on the common classpath). These packets are a cosmetic
 * <em>echo</em> of the pipe's instant item relay — owned, advanced, expired and persisted by the pipe
 * block entity and synced to clients so {@code UniversalPipeRenderer} can draw the flow; they never
 * affect the actual transfer.</p>
 */
public final class TravellingItem {

    public static final @NonNull Codec<TravellingItem> CODEC = NerospaceCommon.requireNonNull(
            RecordCodecBuilder.create(i -> i.group(
            NerospaceCommon.ITEM_STACK_CODEC.fieldOf("item").forGetter(t -> NerospaceCommon.requireNonNull(t.stack)),
            Direction.CODEC.fieldOf("from").forGetter(t -> t.from),
            Direction.CODEC.optionalFieldOf("to").forGetter(t -> Optional.ofNullable(t.to)),
            Codec.FLOAT.fieldOf("progress").forGetter(t -> t.progress))
            .apply(i, (stack, from, to, progress) ->
                    new TravellingItem(stack, from, to.orElse(null), progress))));

    private final @NonNull ItemStack stack;
    private final @NonNull Direction from;
    @Nullable
    private final Direction to;
    private float progress;

    public TravellingItem(@NonNull ItemStack stack, @NonNull Direction from, @Nullable Direction to, float progress) {
        this.stack = stack;
        this.from = from;
        this.to = to;
        this.progress = progress;
    }

    public @NonNull ItemStack stack() {
        return this.stack;
    }

    public @NonNull Direction from() {
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

    public boolean isFinished() {
        return this.progress >= 1.0F;
    }

    /** Move along the segment (server advance + client-side smoothing between syncs). */
    public void advance(float by) {
        this.progress = Math.min(1.0F, this.progress + by);
    }
}
