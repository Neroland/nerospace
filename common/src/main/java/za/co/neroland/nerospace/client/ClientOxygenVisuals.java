package za.co.neroland.nerospace.client;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Oxygen-field visual FX (terraform design §1.7) — makes the otherwise-invisible breathable field
 * readable: a soft crossfade note when the player crosses the breathable boundary, and sparse drifting
 * GLOW particles inside oxygenated cells near the player. Reads the client-synced
 * {@link ClientOxygenField}; client-only, called once per client tick from each loader's hook.
 *
 * <p>Cross-loader port note: the haze fog-tint layer (root layer 2) is deferred — it rode a NeoForge
 * {@code ViewportEvent.ComputeFogColor} with no portable Fabric counterpart. Visual config is inlined
 * (quality FULL; the config seam is deferred).</p>
 */
public final class ClientOxygenVisuals {

    // --- Inlined visual config (root shipped defaults; quality = FULL) ---
    private static final int BREATHABLE_THRESHOLD = 6;
    private static final int MAX_CONCENTRATION = 15;
    private static final double PARTICLE_INTENSITY = 1.0D;
    private static final double BOUNDARY_INTENSITY = 1.0D;

    /** Spawn ambient particles only every Nth tick — heavy iteration, kept sparse for performance. */
    private static final int PARTICLE_INTERVAL_TICKS = 8;
    private static final long MAX_DIST_SQ = 18L * 18L;
    private static final int PARTICLE_BUDGET = 2; // FULL quality

    private static boolean wasBreathable;
    private static int fxTick;

    private ClientOxygenVisuals() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused()) {
            return;
        }

        BlockPos playerPos = player.blockPosition();

        // Boundary sound: crossfade an ambient note when the player crosses the breathable boundary.
        boolean breathingNow = ClientOxygenField.concentrationAt(playerPos.above()) >= BREATHABLE_THRESHOLD
                || ClientOxygenField.concentrationAt(playerPos) >= BREATHABLE_THRESHOLD;
        if (breathingNow != wasBreathable && BOUNDARY_INTENSITY > 0.0D) {
            level.playLocalSound(playerPos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.AMBIENT,
                    0.25F, breathingNow ? 1.3F : 0.8F, false);
        }
        wasBreathable = breathingNow;

        // Particles are the expensive layer — run them only every Nth tick with a tiny budget.
        if (++fxTick % PARTICLE_INTERVAL_TICKS != 0 || PARTICLE_INTENSITY <= 0.0D) {
            return;
        }
        Long2ByteMap field = ClientOxygenField.view();
        if (field.isEmpty()) {
            return;
        }
        RandomSource rnd = level.getRandom();
        int spawned = 0;
        for (Long2ByteMap.Entry e : field.long2ByteEntrySet()) {
            if (spawned >= PARTICLE_BUDGET) {
                break;
            }
            int conc = e.getByteValue() & 0xFF;
            if (conc < BREATHABLE_THRESHOLD) {
                continue;
            }
            BlockPos p = BlockPos.of(e.getLongKey());
            if (p.distSqr(playerPos) > MAX_DIST_SQ) {
                continue;
            }
            // A single drifting ambient GLOW, rate proportional to concentration.
            if (rnd.nextDouble() < PARTICLE_INTENSITY * (conc / (double) MAX_CONCENTRATION) * 0.08D) {
                level.addParticle(ParticleTypes.GLOW,
                        p.getX() + rnd.nextDouble(), p.getY() + rnd.nextDouble(), p.getZ() + rnd.nextDouble(),
                        0.0D, 0.004D, 0.0D);
                spawned++;
            }
        }
    }
}
