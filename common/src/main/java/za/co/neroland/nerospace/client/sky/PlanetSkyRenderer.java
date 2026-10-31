package za.co.neroland.nerospace.client.sky;

import java.lang.ref.WeakReference;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;

import za.co.neroland.nerospace.client.ClientPlanetClock;
import za.co.neroland.nerospace.client.ClientPlanetWeather;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.nyxara.worldgen.FixedXBands;

/**
 * The single, data-driven planet sky (hybrid path). Each loader's sky hook asks {@link #frame} for the current level
 * and, when it returns a frame, turns {@link #scene} into draw calls; when it returns {@code null} the vanilla skybox
 * renders untouched. A profile with {@code vanillaSkybox: true} (or no profile file) keeps the vanilla sky.
 *
 * <p>Drawing exists on NeoForge 26.3 (its {@code CustomSkyboxRenderer}, selected per dimension type by the
 * {@code neoforge:custom_skybox} attribute). Every other cell shows the dimension type's own colour attributes, which
 * carry each planet's palette. Sky rendering is never a block-entity renderer and never reads block state.</p>
 *
 * <p>World fog: {@link #fogRgb} gives the scene's fog colour for the loaders' fog-colour hooks (NeoForge and Forge
 * {@code ViewportEvent.ComputeFogColor}, a Fabric client mixin), on every cell, so the world fog follows the planet's
 * phase, position, weather and eclipse even where the sky itself is the dimension's colour floor.</p>
 */
public final class PlanetSkyRenderer {

    private PlanetSkyRenderer() {
    }

    /** Everything a loader hook needs to draw one sky frame: profile, clock, weather and the client sky switches. */
    public record Frame(PlanetDefinition planet, SkyProfile profile, double phase, double days, double daylight,
            WeatherEvent weather, float weatherIntensity, int bandWidth, long dayLengthTicks, boolean frozen,
            WeatherEvent recoveryEvent, float recovery, SkyScene.Config config) {
    }

    // One scene for the session, recomputed in place (no per-frame allocation) and reused when the fog hook and the
    // sky pass ask for the same instant and camera position (client main thread only).
    private static final SkyScene SCENE = new SkyScene();
    private static final SkyScene.Inputs INPUTS = new SkyScene.Inputs(0.0, 0.0, 0.0, "NONE", 0.0F, "NONE", 0.0F, 0.0,
            0, 0.0, 0L, false, 0L, SkyScene.Config.FULL);
    private static boolean sceneValid;
    @Nullable
    private static SkyProfile cachedProfile;
    private static double cachedTime = Double.NaN;
    private static double cachedPhase = Double.NaN;
    private static float cachedIntensity = Float.NaN;
    private static float cachedRecovery = Float.NaN;
    private static double cachedPosition = Double.NaN;
    @Nullable
    private static SkyScene.Config cachedConfig;

    // The last frame, reused while the level, game tick, partial tick and sky switches are unchanged (the fog hook and
    // the sky pass both ask every frame).
    private static WeakReference<ClientLevel> frameLevel = new WeakReference<>(null);
    private static long frameGameTime = Long.MIN_VALUE;
    private static float framePartialTick = Float.NaN;
    @Nullable
    private static SkyScene.Config frameConfig;
    @Nullable
    private static Frame lastFrame;

    // The shooting-star seed of the last planet (the id string is built once per planet, not per frame).
    @Nullable
    private static PlanetDefinition seedPlanet;
    private static long seedValue;

    /**
     * The frame to draw for {@code level}, or null to leave the vanilla sky alone (not a planet, no profile, or a
     * profile that asks for the vanilla skybox).
     */
    @Nullable
    public static Frame frame(@Nullable ClientLevel level) {
        return frame(level, 1.0F);
    }

    /** {@link #frame(ClientLevel)} with the weather smoothing interpolated to {@code partialTick}. */
    @Nullable
    public static Frame frame(@Nullable ClientLevel level, float partialTick) {
        if (level == null) {
            lastFrame = null;
            frameLevel.clear();
            return null;
        }
        SkyScene.Config config = SkyScene.Config.of(NerospaceConfig.skyAurora(), NerospaceConfig.skyShootingStars(),
                NerospaceConfig.skyLite());
        long gameTime = level.getGameTime();
        if (frameLevel.get() == level && frameGameTime == gameTime && framePartialTick == partialTick
                && frameConfig == config) {
            return lastFrame;
        }
        Frame made = makeFrame(level, partialTick, config);
        if (frameLevel.get() != level) {
            frameLevel = new WeakReference<>(level);
        }
        frameGameTime = gameTime;
        framePartialTick = partialTick;
        frameConfig = config;
        lastFrame = made;
        return made;
    }

    @Nullable
    private static Frame makeFrame(ClientLevel level, float partialTick, SkyScene.Config config) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null) {
            return null;
        }
        SkyProfile profile = SkyProfiles.get(planet.traits().skyProfile());
        if (profile.vanillaSkybox()) {
            return null;
        }
        double phase = ClientPlanetClock.phase(level);
        return new Frame(planet, profile, phase, ClientPlanetClock.days(level), planet.daylight().at(phase),
                ClientPlanetWeather.smoothedEvent(level), ClientPlanetWeather.smoothedIntensity(level, partialTick),
                ClientPlanetClock.bandWidth(level, NerospaceConfig.nyxaraTwilightBandWidth()),
                ClientPlanetClock.dayLength(level), ClientPlanetClock.frozen(level),
                ClientPlanetWeather.recoveryEvent(level), ClientPlanetWeather.recovery(level), config);
    }

    /** Whether a loader hook should take over the sky for this level this frame. */
    public static boolean overridesSky(@Nullable ClientLevel level) {
        return frame(level) != null;
    }

    /**
     * What the frame shows: colours, stars, galaxy, aurora, decals, clouds, shooting stars, haze and every body. The
     * returned scene is the renderer's single reusable instance: read it now, do not keep it across frames.
     *
     * @param position  the camera's coordinate on the profile's {@code positionBlend} axis
     * @param timeTicks game time plus the partial tick
     */
    public static SkyScene scene(Frame frame, double position, double timeTicks) {
        if (sceneValid && cachedProfile == frame.profile() && cachedTime == timeTicks
                && cachedPhase == frame.phase() && cachedIntensity == frame.weatherIntensity()
                && cachedRecovery == frame.recovery() && cachedPosition == position && cachedConfig == frame.config()) {
            return SCENE;
        }
        fill(INPUTS, frame, position, timeTicks);
        SCENE.update(frame.profile(), INPUTS);
        sceneValid = true;
        cachedProfile = frame.profile();
        cachedTime = timeTicks;
        cachedPhase = frame.phase();
        cachedIntensity = frame.weatherIntensity();
        cachedRecovery = frame.recovery();
        cachedPosition = position;
        cachedConfig = frame.config();
        return SCENE;
    }

    /** The scene inputs for a frame (pure; the fog test uses it). */
    public static SkyScene.Inputs inputs(Frame frame, double position, double timeTicks) {
        return fill(new SkyScene.Inputs(0.0, 0.0, 0.0, "NONE", 0.0F, "NONE", 0.0F, 0.0, 0, 0.0, 0L, false, 0L,
                SkyScene.Config.FULL), frame, position, timeTicks);
    }

    private static SkyScene.Inputs fill(SkyScene.Inputs in, Frame frame, double position, double timeTicks) {
        int bandWidth = FixedXBands.DEFAULT_WIDTH;
        if (frame.profile().positionBlend().isPresent()
                && "nyxara.twilightBandWidth".equals(frame.profile().positionBlend().get().bandWidthConfig())) {
            bandWidth = frame.bandWidth();
        }
        return in.set(frame.phase(), frame.days(), frame.daylight(), frame.weather().name(), frame.weatherIntensity(),
                frame.recoveryEvent().name(), frame.recovery(), position, bandWidth, timeTicks, frame.dayLengthTicks(),
                frame.frozen(), seed(frame.planet()), frame.config());
    }

    /** Per-planet shooting-star seed: the sky id's hash (worked out once per planet). */
    static long seed(PlanetDefinition planet) {
        if (planet != seedPlanet) {
            seedValue = planet.traits().skyProfile().toString().hashCode();
            seedPlanet = planet;
        }
        return seedValue;
    }

    /** The coordinate {@link #scene} wants for the profile's blend axis ({@code "z"} or, by default, x). */
    public static double blendPosition(Frame frame, double x, double z) {
        return frame.profile().positionBlend().isPresent()
                && "z".equalsIgnoreCase(frame.profile().positionBlend().get().axis()) ? z : x;
    }

    /**
     * The world fog colour (0xRRGGBB) the loaders' fog hooks push for this camera, or -1 to leave vanilla's colour:
     * not a planet with a sky profile, the camera in water, lava or powder snow, or the camera entity blind, in
     * darkness or with Night Vision (vanilla's Night Vision brightening of the fog is kept, so the fog does not turn
     * night-dark under a brightened sky). The colour is the scene's {@code fog}: the profile's per-phase fog with the position blend, weather tint
     * and eclipse dimming applied, the same value the 26.3 sky disc fades into.
     */
    public static int fogRgb(Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        FogType fluid = camera.getFluidInCamera();
        if (fluid == FogType.WATER || fluid == FogType.LAVA || fluid == FogType.POWDER_SNOW) {
            return -1;
        }
        Entity entity = mc.getCameraEntity();
        if (entity instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS)
                        || living.hasEffect(MobEffects.NIGHT_VISION))) {
            return -1;
        }
        Frame frame = frame(level, partialTick);
        if (frame == null || level == null) {
            return -1;
        }
        double x = entity != null ? entity.getX() : 0.0;
        double z = entity != null ? entity.getZ() : 0.0;
        return scene(frame, blendPosition(frame, x, z), level.getGameTime() + (double) partialTick).fog;
    }
}
