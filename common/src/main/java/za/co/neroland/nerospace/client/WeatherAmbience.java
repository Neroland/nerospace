package za.co.neroland.nerospace.client;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.registry.ModParticles;
import za.co.neroland.nerospace.registry.ModSounds;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.WeatherEvent;

/**
 * Client-side planet ambience, ticked once per client tick by every loader: drifting particles around the player
 * (Mars dust, Cindara ash and embers, Greenxertz spores, Glacira diamond dust, Nyxara nebula motes) and the weather
 * loops (dust-storm roar, ash fall, diamond-dust chimes, spore bloom, flare hum). Nothing is sent to the server and
 * nothing is stored.
 *
 * <p>Budget: each type spawns {@code particles.<type>.perPlayerPerTick} at full weather, scaled down to the planet's
 * calm-weather floor when clear; the total is also capped by {@code weather.particleBudget} per second. Particles only
 * appear in open air under the sky, in a 24 x 16 x 24 box centred four blocks ahead of the camera.</p>
 */
public final class WeatherAmbience {

    private static final RandomSource RANDOM = RandomSource.create();
    private static double dustAcc;
    private static double ashAcc;
    private static double sporeAcc;
    private static double diamondAcc;
    private static double emberAcc;
    private static double moteAcc;
    private static int spentThisSecond;
    private static int secondTick;
    @Nullable
    private static WeatherLoop loop;
    private static WeatherEvent lastEvent = WeatherEvent.NONE;

    private WeatherAmbience() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (!mc.isPaused()) {
            // Eases the client's weather copy (sky fades, particle ramp) once per tick on every loader.
            ClientPlanetWeather.clientTick(level);
        }
        if (level == null || player == null || mc.isPaused()) {
            if (level == null) {
                stopLoop();
                lastEvent = WeatherEvent.NONE;
            }
            return;
        }
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null) {
            stopLoop();
            lastEvent = WeatherEvent.NONE;
            return;
        }
        // The smoothed copy, so a forced gallery state or a cleared storm fades in and out with the sky.
        WeatherEvent event = ClientPlanetWeather.smoothedEvent(level);
        float intensity = ClientPlanetWeather.smoothedIntensity(level, 1.0F);
        sounds(mc, planet.langId(), event, intensity);
        if (NerospaceConfig.particlesEnabled()) {
            particles(level, player, planet.langId(), event, intensity);
        }
    }

    // ------------------------------------------------------------------------------------------------ particles

    private static void particles(ClientLevel level, LocalPlayer player, String planet, WeatherEvent event,
            float intensity) {
        if (++secondTick >= 20) {
            secondTick = 0;
            spentThisSecond = 0;
        }
        int budget = NerospaceConfig.weatherParticleBudget();
        Vec3 eye = player.getEyePosition();
        Vec3 centre = eye.add(player.getLookAngle().scale(4.0));
        switch (planet) {
            case "mars" -> {
                float k = weatherScale(0.15F, event == WeatherEvent.DUST_STORM ? intensity : 0.0F);
                dustAcc += NerospaceConfig.particleRate("dust") * k;
                // Wind: the storm blows from the west, gusting with intensity.
                double speed = 0.08 + 0.17 * (event == WeatherEvent.DUST_STORM ? intensity : 0.0F);
                dustAcc = spawn(level, centre, ModParticles.DUST.isPresent() ? ModParticles.DUST.get() : null, dustAcc,
                        budget, speed, 0.0, speed * 0.3);
            }
            case "cindara" -> {
                float storm = event == WeatherEvent.ASH_FALL ? intensity : 0.0F;
                ashAcc += NerospaceConfig.particleRate("ash") * weatherScale(0.2F, storm);
                ashAcc = spawn(level, centre, ModParticles.ASH.isPresent() ? ModParticles.ASH.get() : null, ashAcc,
                        budget, 0.0, -0.01, 0.0);
                emberAcc += NerospaceConfig.particleRate("ember") * (1.0F + 0.5F * storm);
                emberAcc = embers(level, player, emberAcc, budget);
            }
            case "greenxertz" -> {
                float k = weatherScale(0.25F, event == WeatherEvent.SPORE_BLOOM ? intensity : 0.0F);
                sporeAcc += NerospaceConfig.particleRate("spore") * k;
                sporeAcc = spawn(level, centre, ModParticles.SPORE.isPresent() ? ModParticles.SPORE.get() : null,
                        sporeAcc, budget, 0.0, 0.0, 0.0);
            }
            case "glacira" -> {
                float k = weatherScale(0.3F, event == WeatherEvent.DIAMOND_DUST ? intensity : 0.0F);
                diamondAcc += NerospaceConfig.particleRate("diamond_dust") * k;
                diamondAcc = spawn(level, centre,
                        ModParticles.DIAMOND_DUST.isPresent() ? ModParticles.DIAMOND_DUST.get() : null, diamondAcc,
                        budget, 0.0, -0.01, 0.0);
            }
            case "nyxara" -> {
                // Twilight band and nightside only (the band is centred on x = 0).
                if (player.getX() < -ClientPlanetClock.bandWidth(level, NerospaceConfig.nyxaraTwilightBandWidth()) / 2.0) {
                    moteAcc = 0.0;
                    return;
                }
                float k = weatherScale(0.3F, event == WeatherEvent.FLARE ? intensity : 0.0F);
                moteAcc += NerospaceConfig.particleRate("nebula_mote") * k;
                moteAcc = spawn(level, centre,
                        ModParticles.NEBULA_MOTE.isPresent() ? ModParticles.NEBULA_MOTE.get() : null, moteAcc, budget,
                        0.01, 0.0, 0.0);
            }
            default -> {
                // Luna and the stations: no ambient particles.
            }
        }
    }

    /** Calm floor plus the weather share: {@code floor + intensity * (1 - floor)}. */
    static float weatherScale(float floor, float intensity) {
        float i = Math.max(0.0F, Math.min(1.0F, intensity));
        return floor + i * (1.0F - floor);
    }

    private static double spawn(ClientLevel level, Vec3 centre, @Nullable SimpleParticleType type, double acc,
            int budget, double vx, double vy, double vz) {
        if (type == null) {
            return 0.0;
        }
        int n = (int) acc;
        double rest = acc - n;
        for (int i = 0; i < n && spentThisSecond < budget; i++) {
            double x = centre.x + (RANDOM.nextDouble() - 0.5) * 24.0;
            double y = centre.y + (RANDOM.nextDouble() - 0.5) * 16.0;
            double z = centre.z + (RANDOM.nextDouble() - 0.5) * 24.0;
            BlockPos pos = BlockPos.containing(x, y, z);
            if (!level.getBlockState(pos).isAir() || !level.canSeeSky(pos)) {
                continue;
            }
            level.addParticle(type, x, y, z, vx, vy, vz);
            spentThisSecond++;
        }
        return Math.min(rest, 1.0);
    }

    private static double embers(ClientLevel level, LocalPlayer player, double acc, int budget) {
        if (!ModParticles.EMBER.isPresent()) {
            return 0.0;
        }
        int n = (int) acc;
        double rest = acc - n;
        BlockPos origin = player.blockPosition();
        for (int i = 0; i < n && spentThisSecond < budget; i++) {
            BlockPos pos = origin.offset(RANDOM.nextInt(13) - 6, RANDOM.nextInt(13) - 6, RANDOM.nextInt(13) - 6);
            BlockState state = level.getBlockState(pos);
            boolean hot = state.is(Blocks.MAGMA_BLOCK) || state.getFluidState().is(FluidTags.LAVA);
            if (!hot || !level.getBlockState(pos.above()).isAir()) {
                continue;
            }
            level.addParticle(ModParticles.EMBER.get(), pos.getX() + RANDOM.nextDouble(), pos.getY() + 1.05,
                    pos.getZ() + RANDOM.nextDouble(), 0.0, 0.04, 0.0);
            spentThisSecond++;
        }
        return Math.min(rest, 1.0);
    }

    // ------------------------------------------------------------------------------------------------ sounds

    private static void sounds(Minecraft mc, String planet, WeatherEvent event, float intensity) {
        RegistryEntry<SoundEvent> wanted = switch (event) {
            case DUST_STORM -> "mars".equals(planet) ? ModSounds.AMBIENT_MARS_STORM : null;
            case ASH_FALL -> "cindara".equals(planet) ? ModSounds.AMBIENT_CINDARA_ASHFALL : null;
            case DIAMOND_DUST -> "glacira".equals(planet) ? ModSounds.AMBIENT_GLACIRA_DIAMOND_DUST : null;
            case SPORE_BLOOM -> "greenxertz".equals(planet) ? ModSounds.AMBIENT_GREENXERTZ_BLOOM : null;
            case FLARE -> "nyxara".equals(planet) ? ModSounds.AMBIENT_NYXARA_FLARE : null;
            case NONE -> null;
        };
        if (event == WeatherEvent.FLARE && lastEvent != WeatherEvent.FLARE && "nyxara".equals(planet)
                && ModSounds.AMBIENT_NYXARA_FLARE_ONSET.isPresent()) {
            // On the Weather bus (the player's Weather slider), not the UI channel.
            mc.getSoundManager().play(new WeatherCue(ModSounds.AMBIENT_NYXARA_FLARE_ONSET.get()));
        }
        lastEvent = event;
        if (wanted == null || !wanted.isPresent()) {
            stopLoop();
            return;
        }
        WeatherLoop current = loop;
        if (current != null && current.event == event && !current.isStopped()) {
            current.target = loopVolume(intensity);
            return;
        }
        stopLoop();
        if (intensity > 0.1F) {
            WeatherLoop next = new WeatherLoop(wanted.get(), event);
            next.target = loopVolume(intensity);
            loop = next;
            mc.getSoundManager().play(next);
        }
    }

    /** 0 below intensity 0.1 rising to 1 at full intensity (the event's peak volume is set in sounds.json). */
    static float loopVolume(float intensity) {
        if (intensity < 0.05F) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (intensity - 0.1F) / 0.9F));
    }

    private static void stopLoop() {
        WeatherLoop current = loop;
        if (current != null) {
            current.target = 0.0F;
        }
        loop = null;
    }

    /** A camera-relative one-shot weather cue (the flare onset chime) on the Weather channel. */
    static final class WeatherCue extends AbstractSoundInstance {
        WeatherCue(SoundEvent sound) {
            super(sound, SoundSource.WEATHER, SoundInstance.createUnseededRandom());
            this.looping = false;
            this.delay = 0;
            this.volume = 1.0F;
            this.pitch = 1.0F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }
    }

    /** A camera-relative loop whose volume eases toward {@link #target}; it ends itself once faded out. */
    static final class WeatherLoop extends AbstractTickableSoundInstance {
        final WeatherEvent event;
        float target;

        WeatherLoop(SoundEvent sound, WeatherEvent event) {
            super(sound, SoundSource.WEATHER, SoundInstance.createUnseededRandom());
            this.event = event;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            this.volume += (this.target - this.volume) * 0.05F;
            if (this.target <= 0.0F && this.volume < 0.01F) {
                this.stop();
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
