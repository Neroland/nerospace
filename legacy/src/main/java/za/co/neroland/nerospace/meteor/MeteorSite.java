package za.co.neroland.nerospace.meteor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;

/**
 * One tracked meteor impact site (meteor-events design §3). Mutable so the {@link MeteorEventManager}
 * can advance its state/timer in place each tick. {@code timer} means "ticks until the fall" while
 * {@link #SCHEDULED}, and "ticks until this record expires" once {@link #LANDED}.
 */
public final class MeteorSite {

    /** Scheduled near a player; the tracker shows it as incoming during the warning window. */
    public static final int SCHEDULED = 0;
    /** The meteor entity is descending. */
    public static final int FALLING = 1;
    /** Landed — the crater + loot core exist; kept briefly so the tracker leads players in. */
    public static final int LANDED = 2;

    public static final Codec<MeteorSite> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.LONG.fieldOf("pos").forGetter(s -> s.pos),
            Codec.INT.fieldOf("state").forGetter(s -> s.state),
            Codec.INT.fieldOf("timer").forGetter(s -> s.timer)
    ).apply(inst, MeteorSite::new));

    public long pos;
    public int state;
    public int timer;

    public MeteorSite(long pos, int state, int timer) {
        this.pos = pos;
        this.state = state;
        this.timer = timer;
    }

    public BlockPos blockPos() {
        return BlockPos.of(this.pos);
    }
}
