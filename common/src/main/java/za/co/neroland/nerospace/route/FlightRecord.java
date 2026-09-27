package za.co.neroland.nerospace.route;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.api.route.FlightHandle;
import za.co.neroland.nerospace.api.route.FlightState;

/**
 * One cargo flight (internal, persisted, mutable — the state machine in {@link CargoFlights} drives it).
 * Holds exactly what gameplay needs: ids, the manifest still aboard, the departure/arrival ticks, the
 * state and its hold bookkeeping, and the owner UUID string ({@code ""} once erased). No player names,
 * no timestamps beyond the flight itself.
 */
public final class FlightRecord {

    /** Codec intermediaries: DFU groups cap at 16 fields, so the record is split three ways. */
    private record Ident(int id, int route, int origin, int destination, String owner, boolean returnLeg) {
        static final Codec<Ident> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("id").forGetter(Ident::id),
                Codec.INT.optionalFieldOf("route", -1).forGetter(Ident::route),
                Codec.INT.fieldOf("origin").forGetter(Ident::origin),
                Codec.INT.fieldOf("destination").forGetter(Ident::destination),
                Codec.STRING.optionalFieldOf("owner", "").forGetter(Ident::owner),
                Codec.BOOL.optionalFieldOf("return_leg", false).forGetter(Ident::returnLeg)
        ).apply(inst, Ident::new));
    }

    private record Path(String originDim, BlockPos originPos, String destDim, BlockPos destPos, int fuel,
            List<ItemStack> items, long departedAt, long arrivesAt) {
        static final Codec<Path> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("origin_dim").forGetter(Path::originDim),
                BlockPos.CODEC.fieldOf("origin_pos").forGetter(Path::originPos),
                Codec.STRING.fieldOf("dest_dim").forGetter(Path::destDim),
                BlockPos.CODEC.fieldOf("dest_pos").forGetter(Path::destPos),
                Codec.INT.optionalFieldOf("fuel", 0).forGetter(Path::fuel),
                ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(Path::items),
                Codec.LONG.fieldOf("departed_at").forGetter(Path::departedAt),
                Codec.LONG.fieldOf("arrives_at").forGetter(Path::arrivesAt)
        ).apply(inst, Path::new));
    }

    private record Progress(String state, int holdAttempts, long holdSince, long nextRetryAt, long completedAt,
            boolean landed, boolean cancelRequested) {
        static final Codec<Progress> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.optionalFieldOf("state", FlightState.IN_FLIGHT.name()).forGetter(Progress::state),
                Codec.INT.optionalFieldOf("hold_attempts", 0).forGetter(Progress::holdAttempts),
                Codec.LONG.optionalFieldOf("hold_since", -1L).forGetter(Progress::holdSince),
                Codec.LONG.optionalFieldOf("next_retry_at", -1L).forGetter(Progress::nextRetryAt),
                Codec.LONG.optionalFieldOf("completed_at", -1L).forGetter(Progress::completedAt),
                Codec.BOOL.optionalFieldOf("landed", false).forGetter(Progress::landed),
                Codec.BOOL.optionalFieldOf("cancel_requested", false).forGetter(Progress::cancelRequested)
        ).apply(inst, Progress::new));
    }

    public static final Codec<FlightRecord> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Ident.CODEC.fieldOf("ident").forGetter(f -> new Ident(f.id, f.routeId, f.originPadId, f.destinationPadId,
                    f.owner, f.returnLeg)),
            Path.CODEC.fieldOf("path").forGetter(f -> new Path(f.originDim, f.originPos, f.destDim, f.destPos, f.fuel,
                    List.copyOf(f.items), f.departedAt, f.arrivesAt)),
            Progress.CODEC.fieldOf("progress").forGetter(f -> new Progress(f.state.name(), f.holdAttempts, f.holdSince,
                    f.nextRetryAt, f.completedAt, f.landed, f.cancelRequested))
    ).apply(inst, FlightRecord::of));

    private final int id;
    private final int routeId;
    private final int originPadId;
    private final int destinationPadId;
    private String owner;
    private final boolean returnLeg;
    /** Where it left from / is going, snapshotted at launch so cargo can still be crated if a pad vanishes. */
    private final String originDim;
    private final BlockPos originPos;
    private final String destDim;
    private final BlockPos destPos;
    /** Fuel carried aboard (what was left after the quote), restored to the rocket on arrival. */
    private final int fuel;
    private final List<ItemStack> items;
    private final long departedAt;
    private final long arrivesAt;
    private FlightState state;
    private int holdAttempts;
    private long holdSince;
    private long nextRetryAt;
    private long completedAt;
    /** Whether the rocket has materialised on the destination pad (unloading may still be in progress). */
    private boolean landed;
    private boolean cancelRequested;

    FlightRecord(int id, int routeId, int originPadId, int destinationPadId, String owner, boolean returnLeg,
            String originDim, BlockPos originPos, String destDim, BlockPos destPos, int fuel,
            List<ItemStack> items, long departedAt, long arrivesAt) {
        this.id = id;
        this.routeId = routeId;
        this.originPadId = originPadId;
        this.destinationPadId = destinationPadId;
        this.owner = owner == null ? "" : owner;
        this.returnLeg = returnLeg;
        this.originDim = originDim;
        this.originPos = originPos.immutable();
        this.destDim = destDim;
        this.destPos = destPos.immutable();
        this.fuel = Math.max(0, fuel);
        this.items = new ArrayList<>();
        for (ItemStack stack : items) {
            if (stack != null && !stack.isEmpty()) {
                this.items.add(stack.copy());
            }
        }
        this.departedAt = departedAt;
        this.arrivesAt = arrivesAt;
        this.state = FlightState.IN_FLIGHT;
        this.holdSince = -1L;
        this.nextRetryAt = -1L;
        this.completedAt = -1L;
    }

    private static FlightRecord of(Ident ident, Path path, Progress progress) {
        FlightRecord f = new FlightRecord(ident.id(), ident.route(), ident.origin(), ident.destination(), ident.owner(),
                ident.returnLeg(), path.originDim(), path.originPos(), path.destDim(), path.destPos(), path.fuel(),
                path.items(), path.departedAt(), path.arrivesAt());
        f.state = FlightState.byName(progress.state());
        f.holdAttempts = progress.holdAttempts();
        f.holdSince = progress.holdSince();
        f.nextRetryAt = progress.nextRetryAt();
        f.completedAt = progress.completedAt();
        f.landed = progress.landed();
        f.cancelRequested = progress.cancelRequested();
        return f;
    }

    // --- Read ------------------------------------------------------------------------------

    public int id() {
        return this.id;
    }

    public int routeId() {
        return this.routeId;
    }

    public int originPadId() {
        return this.originPadId;
    }

    public int destinationPadId() {
        return this.destinationPadId;
    }

    /** Owner UUID string or {@code ""}. Internal — never log it, never send it; the API answers {@link #ownedBy}. */
    public String owner() {
        return this.owner;
    }

    public boolean ownedBy(UUID player) {
        return player != null && !this.owner.isEmpty() && this.owner.equals(player.toString());
    }

    public boolean returnLeg() {
        return this.returnLeg;
    }

    public String originDim() {
        return this.originDim;
    }

    public BlockPos originPos() {
        return this.originPos;
    }

    public String destDim() {
        return this.destDim;
    }

    public BlockPos destPos() {
        return this.destPos;
    }

    public int fuel() {
        return this.fuel;
    }

    /** The live manifest list — internal; {@link CargoFlights} consumes from it. */
    List<ItemStack> items() {
        return this.items;
    }

    public long departedAt() {
        return this.departedAt;
    }

    public long arrivesAt() {
        return this.arrivesAt;
    }

    public FlightState state() {
        return this.state;
    }

    public int holdAttempts() {
        return this.holdAttempts;
    }

    public long holdSince() {
        return this.holdSince;
    }

    public long nextRetryAt() {
        return this.nextRetryAt;
    }

    public long completedAt() {
        return this.completedAt;
    }

    public boolean landed() {
        return this.landed;
    }

    public boolean cancelRequested() {
        return this.cancelRequested;
    }

    public int itemCount() {
        int n = 0;
        for (ItemStack stack : this.items) {
            n += stack.getCount();
        }
        return n;
    }

    // --- Mutate (CargoFlights + erasure only) ----------------------------------------------

    void setState(FlightState state) {
        this.state = state;
        if (state.isTerminal()) {
            this.cancelRequested = false;
        }
    }

    void markHeld(long now, long retryAt) {
        if (this.holdSince < 0) {
            this.holdSince = now;
        }
        this.holdAttempts++;
        this.nextRetryAt = retryAt;
        this.state = FlightState.HOLDING;
    }

    void clearHold() {
        this.holdSince = -1L;
        this.nextRetryAt = -1L;
    }

    void markLanded() {
        this.landed = true;
    }

    void complete(FlightState terminal, long now) {
        this.state = terminal;
        this.completedAt = now;
        this.nextRetryAt = -1L;
        this.cancelRequested = false;
    }

    void requestCancel() {
        this.cancelRequested = true;
    }

    /** Erasure: forget who owned this flight. The cargo keeps flying. */
    void anonymise() {
        this.owner = "";
    }

    /** Immutable snapshot for API consumers. */
    public FlightHandle view() {
        return new FlightView(this);
    }

    /** Immutable {@link FlightHandle}; the owner stays behind {@link #ownedBy}. */
    private static final class FlightView implements FlightHandle {

        private final int id;
        private final int routeId;
        private final int originPadId;
        private final int destinationPadId;
        private final FlightState state;
        private final long departedAt;
        private final long arrivesAt;
        private final boolean returnLeg;
        private final List<ItemStack> manifest;
        private final int itemCount;
        private final String owner;

        FlightView(FlightRecord f) {
            this.id = f.id;
            this.routeId = f.routeId;
            this.originPadId = f.originPadId;
            this.destinationPadId = f.destinationPadId;
            this.state = f.state;
            this.departedAt = f.departedAt;
            this.arrivesAt = f.arrivesAt;
            this.returnLeg = f.returnLeg;
            List<ItemStack> copies = new ArrayList<>(f.items.size());
            for (ItemStack stack : f.items) {
                copies.add(stack.copy());
            }
            this.manifest = List.copyOf(copies);
            this.itemCount = f.itemCount();
            this.owner = f.owner;
        }

        @Override
        public int id() {
            return this.id;
        }

        @Override
        public int routeId() {
            return this.routeId;
        }

        @Override
        public int originPadId() {
            return this.originPadId;
        }

        @Override
        public int destinationPadId() {
            return this.destinationPadId;
        }

        @Override
        public FlightState state() {
            return this.state;
        }

        @Override
        public long departedAt() {
            return this.departedAt;
        }

        @Override
        public long arrivesAt() {
            return this.arrivesAt;
        }

        @Override
        public boolean returnLeg() {
            return this.returnLeg;
        }

        @Override
        public List<ItemStack> manifest() {
            List<ItemStack> copies = new ArrayList<>(this.manifest.size());
            for (ItemStack stack : this.manifest) {
                copies.add(stack.copy());
            }
            return copies;
        }

        @Override
        public int itemCount() {
            return this.itemCount;
        }

        @Override
        public boolean ownedBy(UUID player) {
            return player != null && !this.owner.isEmpty() && this.owner.equals(player.toString());
        }

        @Override
        public String toString() {
            return "Flight#" + this.id + "[" + this.state + "]";
        }
    }
}
