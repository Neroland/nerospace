package za.co.neroland.nerospace.route;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import za.co.neroland.nerospace.api.route.RouteHandle;
import za.co.neroland.nerospace.api.route.ScheduleMode;

/**
 * One saved route (internal, persisted): {@code origin pad → destination pad}, owned by the origin pad's
 * owner, with the pad's schedule and {@code return_empty} setting. At most one route exists per origin
 * pad — the pad's destination selector re-points it.
 */
public record RouteRecord(int id, int originPadId, int destinationPadId, String owner, ScheduleMode mode,
        int intervalTicks, boolean returnEmpty) {

    /** Default {@link ScheduleMode#EVERY_INTERVAL} period: five minutes. */
    public static final int DEFAULT_INTERVAL_TICKS = 6_000;
    public static final int MIN_INTERVAL_TICKS = 1_200;
    public static final int MAX_INTERVAL_TICKS = 72_000;
    /** GUI step for the interval buttons (one minute). */
    public static final int INTERVAL_STEP_TICKS = 1_200;

    public static final Codec<RouteRecord> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("id").forGetter(RouteRecord::id),
            Codec.INT.fieldOf("origin").forGetter(RouteRecord::originPadId),
            Codec.INT.fieldOf("destination").forGetter(RouteRecord::destinationPadId),
            Codec.STRING.optionalFieldOf("owner", "").forGetter(RouteRecord::owner),
            Codec.STRING.optionalFieldOf("schedule", ScheduleMode.MANUAL.name()).forGetter(r -> r.mode().name()),
            Codec.INT.optionalFieldOf("interval_ticks", DEFAULT_INTERVAL_TICKS).forGetter(RouteRecord::intervalTicks),
            Codec.BOOL.optionalFieldOf("return_empty", false).forGetter(RouteRecord::returnEmpty)
    ).apply(inst, RouteRecord::of));

    public RouteRecord {
        owner = owner == null ? "" : owner;
        mode = mode == null ? ScheduleMode.MANUAL : mode;
        intervalTicks = clampInterval(intervalTicks);
    }

    private static RouteRecord of(Integer id, Integer origin, Integer destination, String owner, String mode,
            Integer interval, Boolean returnEmpty) {
        return new RouteRecord(id.intValue(), origin.intValue(), destination.intValue(), owner,
                parseMode(mode), interval.intValue(), returnEmpty.booleanValue());
    }

    private static ScheduleMode parseMode(String name) {
        for (ScheduleMode mode : ScheduleMode.values()) {
            if (mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return ScheduleMode.MANUAL;
    }

    public static int clampInterval(int ticks) {
        return Math.max(MIN_INTERVAL_TICKS, Math.min(MAX_INTERVAL_TICKS, ticks));
    }

    public boolean ownedBy(UUID player) {
        return player != null && !this.owner.isEmpty() && this.owner.equals(player.toString());
    }

    RouteRecord withDestination(int destination) {
        return new RouteRecord(this.id, this.originPadId, destination, this.owner, this.mode, this.intervalTicks,
                this.returnEmpty);
    }

    RouteRecord withOwner(String newOwner) {
        return new RouteRecord(this.id, this.originPadId, this.destinationPadId, newOwner, this.mode,
                this.intervalTicks, this.returnEmpty);
    }

    RouteRecord withSchedule(ScheduleMode newMode, int newInterval) {
        return new RouteRecord(this.id, this.originPadId, this.destinationPadId, this.owner, newMode, newInterval,
                this.returnEmpty);
    }

    RouteRecord withReturnEmpty(boolean value) {
        return new RouteRecord(this.id, this.originPadId, this.destinationPadId, this.owner, this.mode,
                this.intervalTicks, value);
    }

    /** The API-facing snapshot. */
    public RouteHandle view() {
        return new RouteView(this);
    }

    /** Immutable {@link RouteHandle}; the owner stays behind {@link #ownedBy}. */
    private static final class RouteView implements RouteHandle {

        private final RouteRecord record;

        RouteView(RouteRecord record) {
            this.record = record;
        }

        @Override
        public int id() {
            return this.record.id();
        }

        @Override
        public int originPadId() {
            return this.record.originPadId();
        }

        @Override
        public int destinationPadId() {
            return this.record.destinationPadId();
        }

        @Override
        public ScheduleMode scheduleMode() {
            return this.record.mode();
        }

        @Override
        public int intervalTicks() {
            return this.record.intervalTicks();
        }

        @Override
        public boolean returnEmpty() {
            return this.record.returnEmpty();
        }

        @Override
        public boolean ownedBy(UUID player) {
            return this.record.ownedBy(player);
        }

        @Override
        public String toString() {
            return "Route#" + id() + "(" + originPadId() + "->" + destinationPadId() + ")";
        }
    }
}
