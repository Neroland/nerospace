package za.co.neroland.nerospace.client.sky;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * A planet's sky, loaded from {@code assets/<namespace>/sky/<planet>.json}. Every field is
 * optional with a default; unknown fields are ignored. Colours are {@code "#RRGGBB"} strings.
 *
 * <p>{@code vanillaSkybox} means "draw nothing; leave the dimension's vanilla skybox as it is" (any profile file may
 * still ask for it).</p>
 *
 * <p>Three optional fields were added in 2.0.0, all additive (a profile without them decodes exactly as before):
 * {@code gradient} (zenith / horizon / fog keyframes by clock phase, piecewise-linear and wrapping; when present it
 * replaces the {@code background} / {@code fog} day-night blend), {@code positionBlend} (colours by the player's
 * position along one axis, Nyxara's sunward / band / galaxy sides) and {@code terminatorScroll} (slow roll of the
 * station's homeworld disk).</p>
 *
 * <p>The sky upgrade (2.0.x) adds three more top-level fields, {@code decals} (anchored set pieces: halos, corona,
 * flare rays, nebulae …), {@code cloudLayer} and {@code shootingStars}, plus defaulted fields inside the existing
 * records (aurora curtains, night shade, limb and horizon glow, star palette, weather fades). Everything is additive:
 * a 2.0.0 or 1.x profile decodes to the same values as before and every new accessor returns its default. The record
 * has more components than one {@link RecordCodecBuilder} group can hold (16), so {@link #CODEC} joins two map codecs
 * with {@link Codec#mapPair}; the JSON stays one flat object.</p>
 */
public record SkyProfile(boolean vanillaSkybox, Background background, Fog fog, Stars stars, List<Body> sun,
        List<Body> moons, List<Body> bodies, Optional<Galaxy> galaxy, Optional<Aurora> aurora,
        Optional<Eclipse> eclipse, String clouds, Optional<Haze> horizonHaze, Map<String, WeatherTint> weatherTints,
        List<GradientKey> gradient, Optional<PositionBlend> positionBlend, Optional<TerminatorScroll> terminatorScroll,
        List<Decal> decals, Optional<CloudLayer> cloudLayer, Optional<ShootingStars> shootingStars) {

    /** "Keep the vanilla sky": the profile used for any planet without a file. */
    public static final SkyProfile VANILLA = new SkyProfile(true, Background.DEFAULT, Fog.DEFAULT, Stars.DEFAULT,
            List.of(), List.of(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(), "none",
            Optional.empty(), Map.of(), List.of(), Optional.empty(), Optional.empty());

    /** A profile without the sky-upgrade top-level fields (they take their defaults). */
    public SkyProfile(boolean vanillaSkybox, Background background, Fog fog, Stars stars, List<Body> sun,
            List<Body> moons, List<Body> bodies, Optional<Galaxy> galaxy, Optional<Aurora> aurora,
            Optional<Eclipse> eclipse, String clouds, Optional<Haze> horizonHaze, Map<String, WeatherTint> weatherTints,
            List<GradientKey> gradient, Optional<PositionBlend> positionBlend,
            Optional<TerminatorScroll> terminatorScroll) {
        this(vanillaSkybox, background, fog, stars, sun, moons, bodies, galaxy, aurora, eclipse, clouds, horizonHaze,
                weatherTints, gradient, positionBlend, terminatorScroll, List.of(), Optional.empty(), Optional.empty());
    }

    /** The default horizon-ramp texture (the 2.0.0 ramp). */
    public static final Identifier DEFAULT_RAMP = sky("horizon_ramp");

    static Identifier sky(String name) {
        return Identifier.fromNamespaceAndPath("nerospace", "textures/sky/" + name + ".png");
    }

    public record Background(String zenith, String horizon, String nightZenith, String nightHorizon, String blend,
            float rampTopDeg, Optional<Identifier> rampTexture) {
        static final Background DEFAULT = new Background("#000000", "#000000", "#000000", "#000000", "daylight",
                55.0F, Optional.empty());
        static final Codec<Background> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("zenith", "#000000").forGetter(Background::zenith),
                Codec.STRING.optionalFieldOf("horizon", "#000000").forGetter(Background::horizon),
                Codec.STRING.optionalFieldOf("nightZenith", "#000000").forGetter(Background::nightZenith),
                Codec.STRING.optionalFieldOf("nightHorizon", "#000000").forGetter(Background::nightHorizon),
                Codec.STRING.optionalFieldOf("blend", "daylight").forGetter(Background::blend),
                Codec.FLOAT.optionalFieldOf("rampTopDeg", 55.0F).forGetter(Background::rampTopDeg),
                Identifier.CODEC.optionalFieldOf("rampTexture").forGetter(Background::rampTexture)
        ).apply(i, Background::new));
    }

    public record Fog(String color, Optional<String> nightColor, float start, float end) {
        static final Fog DEFAULT = new Fog("#000000", Optional.empty(), 0.6F, 1.0F);
        static final Codec<Fog> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("color", "#000000").forGetter(Fog::color),
                Codec.STRING.optionalFieldOf("nightColor").forGetter(Fog::nightColor),
                Codec.FLOAT.optionalFieldOf("start", 0.6F).forGetter(Fog::start),
                Codec.FLOAT.optionalFieldOf("end", 1.0F).forGetter(Fog::end)
        ).apply(i, Fog::new));
    }

    /**
     * Stars. {@code palette} {@code "natural"} selects the temperature-coloured sheet; {@code twinkle} 0..1 (0 = off)
     * swings two halves of the field against each other; {@code sizeScale} multiplies the star quad size.
     */
    public record Stars(int count, float brightness, boolean alwaysVisible, String tint, String palette,
            float twinkle, float sizeScale) {
        static final Stars DEFAULT = new Stars(1500, 0.6F, false, "#FFFFFF", "white", 0.0F, 1.0F);
        static final Codec<Stars> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("count", 1500).forGetter(Stars::count),
                Codec.FLOAT.optionalFieldOf("brightness", 0.6F).forGetter(Stars::brightness),
                Codec.BOOL.optionalFieldOf("alwaysVisible", false).forGetter(Stars::alwaysVisible),
                Codec.STRING.optionalFieldOf("tint", "#FFFFFF").forGetter(Stars::tint),
                Codec.STRING.optionalFieldOf("palette", "white").forGetter(Stars::palette),
                Codec.FLOAT.optionalFieldOf("twinkle", 0.0F).forGetter(Stars::twinkle),
                Codec.FLOAT.optionalFieldOf("sizeScale", 1.0F).forGetter(Stars::sizeScale)
        ).apply(i, Stars::new));
    }

    public record FixedAngle(float yaw, float pitch) {
        static final Codec<FixedAngle> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(FixedAngle::yaw),
                Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(FixedAngle::pitch)
        ).apply(i, FixedAngle::new));
    }

    /** An additive glow quad drawn before a body ({@code limbGlow}): {@code size × scale}, tinted. */
    public record Glow(Identifier texture, String color, float scale, float strength) {
        static final Codec<Glow> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.optionalFieldOf("texture", sky("limb_glow")).forGetter(Glow::texture),
                Codec.STRING.optionalFieldOf("color", "#FFFFFF").forGetter(Glow::color),
                Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(Glow::scale),
                Codec.FLOAT.optionalFieldOf("strength", 1.0F).forGetter(Glow::strength)
        ).apply(i, Glow::new));
    }

    /** An additive glow around a sun near the horizon (Mars' blue sunset): full below the horizon, fading by {@code pitchBelow}. */
    public record HorizonGlow(Identifier texture, String color, float size, float pitchBelow, float strength) {
        static final Codec<HorizonGlow> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.optionalFieldOf("texture", sky("sun_aureole")).forGetter(HorizonGlow::texture),
                Codec.STRING.optionalFieldOf("color", "#FFFFFF").forGetter(HorizonGlow::color),
                Codec.FLOAT.optionalFieldOf("size", 20.0F).forGetter(HorizonGlow::size),
                Codec.FLOAT.optionalFieldOf("pitchBelow", 12.0F).forGetter(HorizonGlow::pitchBelow),
                Codec.FLOAT.optionalFieldOf("strength", 1.0F).forGetter(HorizonGlow::strength)
        ).apply(i, HorizonGlow::new));
    }

    /**
     * The dark side of a body: a phase-sheet frame centred on the body and turned away from the first sun, plus optional
     * additive city lights that fade out as the lit fraction grows. {@code texture} names the sheet's base: the
     * renderer draws {@code <texture>_00.png … _15.png} (frame {@code round(k · 15)} for the lit fraction {@code k}).
     * {@code softness} documents the baked edge of the frames.
     */
    public record NightShade(Identifier texture, float strength, float softness, Optional<Identifier> lightsTexture,
            String lightsColor, float lightsStrength) {
        static final Codec<NightShade> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.optionalFieldOf("texture", sky("terminator")).forGetter(NightShade::texture),
                Codec.FLOAT.optionalFieldOf("strength", 0.9F).forGetter(NightShade::strength),
                Codec.FLOAT.optionalFieldOf("softness", 0.3F).forGetter(NightShade::softness),
                Identifier.CODEC.optionalFieldOf("lightsTexture").forGetter(NightShade::lightsTexture),
                Codec.STRING.optionalFieldOf("lightsColor", "#FFD9A0").forGetter(NightShade::lightsColor),
                Codec.FLOAT.optionalFieldOf("lightsStrength", 0.6F).forGetter(NightShade::lightsStrength)
        ).apply(i, NightShade::new));
    }

    public record Body(String id, Optional<Identifier> texture, float size, String color, float glow,
            float orbitPeriodDays, float phaseOffset, float inclination, Optional<FixedAngle> fixedAngle,
            boolean phases, Optional<Identifier> glowTexture, Optional<Glow> limbGlow,
            Optional<HorizonGlow> horizonGlow, Optional<NightShade> nightShade, float earthshine) {

        /** A body without the sky-upgrade fields. */
        public Body(String id, Optional<Identifier> texture, float size, String color, float glow,
                float orbitPeriodDays, float phaseOffset, float inclination, Optional<FixedAngle> fixedAngle,
                boolean phases) {
            this(id, texture, size, color, glow, orbitPeriodDays, phaseOffset, inclination, fixedAngle, phases,
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0.0F);
        }

        static final Codec<Body> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("id", "").forGetter(Body::id),
                Identifier.CODEC.optionalFieldOf("texture").forGetter(Body::texture),
                Codec.FLOAT.optionalFieldOf("size", 5.0F).forGetter(Body::size),
                Codec.STRING.optionalFieldOf("color", "#FFFFFF").forGetter(Body::color),
                Codec.FLOAT.optionalFieldOf("glow", 0.0F).forGetter(Body::glow),
                Codec.FLOAT.optionalFieldOf("orbitPeriodDays", 1.0F).forGetter(Body::orbitPeriodDays),
                Codec.FLOAT.optionalFieldOf("phaseOffset", 0.0F).forGetter(Body::phaseOffset),
                Codec.FLOAT.optionalFieldOf("inclination", 0.0F).forGetter(Body::inclination),
                FixedAngle.CODEC.optionalFieldOf("fixedAngle").forGetter(Body::fixedAngle),
                Codec.BOOL.optionalFieldOf("phases", false).forGetter(Body::phases),
                Identifier.CODEC.optionalFieldOf("glowTexture").forGetter(Body::glowTexture),
                Glow.CODEC.optionalFieldOf("limbGlow").forGetter(Body::limbGlow),
                HorizonGlow.CODEC.optionalFieldOf("horizonGlow").forGetter(Body::horizonGlow),
                NightShade.CODEC.optionalFieldOf("nightShade").forGetter(Body::nightShade),
                Codec.FLOAT.optionalFieldOf("earthshine", 0.0F).forGetter(Body::earthshine)
        ).apply(i, Body::new));
    }

    public record Galaxy(Optional<Identifier> texture, float opacity, float yaw, float pitch) {
        static final Codec<Galaxy> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.optionalFieldOf("texture").forGetter(Galaxy::texture),
                Codec.FLOAT.optionalFieldOf("opacity", 0.0F).forGetter(Galaxy::opacity),
                Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(Galaxy::yaw),
                Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(Galaxy::pitch)
        ).apply(i, Galaxy::new));
    }

    /** How the aurora curtain folds: amplitude in degrees, folds along the arc, animation speed. */
    public record Fold(float amplitudeDeg, float frequency, float speed) {
        static final Fold DEFAULT = new Fold(6.0F, 2.2F, 1.0F);
        static final Codec<Fold> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("amplitudeDeg", 6.0F).forGetter(Fold::amplitudeDeg),
                Codec.FLOAT.optionalFieldOf("frequency", 2.2F).forGetter(Fold::frequency),
                Codec.FLOAT.optionalFieldOf("speed", 1.0F).forGetter(Fold::speed)
        ).apply(i, Fold::new));
    }

    /** Slow swing of the curtain centre: {@code yaw += amplitude · sin(2π · days / periodDays)}. */
    public record Drift(float amplitudeDeg, float periodDays) {
        static final Codec<Drift> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("amplitudeDeg", 0.0F).forGetter(Drift::amplitudeDeg),
                Codec.FLOAT.optionalFieldOf("periodDays", 1.0F).forGetter(Drift::periodDays)
        ).apply(i, Drift::new));
    }

    /**
     * A weather gate: the smoothed intensity while {@code event} is active (when {@code active}), plus the recovery
     * after-image once it has cleared (when {@code recovery}).
     */
    public record WeatherGate(String event, boolean active, boolean recovery) {
        static final Codec<WeatherGate> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("event", "NONE").forGetter(WeatherGate::event),
                Codec.BOOL.optionalFieldOf("active", true).forGetter(WeatherGate::active),
                Codec.BOOL.optionalFieldOf("recovery", false).forGetter(WeatherGate::recovery)
        ).apply(i, WeatherGate::new));
    }

    /** An extra upper-curtain layer shown while {@code weather} is active (Glacira's rose fringe in diamond dust). */
    public record Fringe(Identifier texture, String weather, float strength) {
        static final Codec<Fringe> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.optionalFieldOf("texture", sky("aurora_fringe_rose")).forGetter(Fringe::texture),
                Codec.STRING.optionalFieldOf("weather", "NONE").forGetter(Fringe::weather),
                Codec.FLOAT.optionalFieldOf("strength", 1.0F).forGetter(Fringe::strength)
        ).apply(i, Fringe::new));
    }

    /**
     * Aurora curtains. The record is at the 16-field {@link RecordCodecBuilder} maximum: a later field must go into
     * {@link Fold} or {@link Drift}. When {@code textures} is empty the 2.0.0 strips are used and {@code colors} tints
     * them; otherwise the textures carry the colour run.
     */
    public record Aurora(List<String> colors, float opacity, float speed, boolean nightOnly,
            Optional<String> boostOnWeather, List<Identifier> textures, int layers, float yaw, float arcDeg,
            float baseDeg, float topDeg, Fold fold, Optional<Drift> drift, Optional<WeatherGate> weather,
            Optional<Fringe> fringe, List<String> positionSides) {

        /** An aurora without the sky-upgrade fields. */
        public Aurora(List<String> colors, float opacity, float speed, boolean nightOnly,
                Optional<String> boostOnWeather) {
            this(colors, opacity, speed, nightOnly, boostOnWeather, List.of(), 2, 180.0F, 160.0F, 12.0F, 50.0F,
                    Fold.DEFAULT, Optional.empty(), Optional.empty(), Optional.empty(), List.of());
        }

        static final Codec<Aurora> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.listOf().optionalFieldOf("colors", List.of()).forGetter(Aurora::colors),
                Codec.FLOAT.optionalFieldOf("opacity", 0.0F).forGetter(Aurora::opacity),
                Codec.FLOAT.optionalFieldOf("speed", 1.0F).forGetter(Aurora::speed),
                Codec.BOOL.optionalFieldOf("nightOnly", true).forGetter(Aurora::nightOnly),
                Codec.STRING.optionalFieldOf("boostOnWeather").forGetter(Aurora::boostOnWeather),
                Identifier.CODEC.listOf().optionalFieldOf("textures", List.of()).forGetter(Aurora::textures),
                Codec.INT.optionalFieldOf("layers", 2).forGetter(Aurora::layers),
                Codec.FLOAT.optionalFieldOf("yaw", 180.0F).forGetter(Aurora::yaw),
                Codec.FLOAT.optionalFieldOf("arcDeg", 160.0F).forGetter(Aurora::arcDeg),
                Codec.FLOAT.optionalFieldOf("baseDeg", 12.0F).forGetter(Aurora::baseDeg),
                Codec.FLOAT.optionalFieldOf("topDeg", 50.0F).forGetter(Aurora::topDeg),
                Fold.CODEC.optionalFieldOf("fold", Fold.DEFAULT).forGetter(Aurora::fold),
                Drift.CODEC.optionalFieldOf("drift").forGetter(Aurora::drift),
                WeatherGate.CODEC.optionalFieldOf("weather").forGetter(Aurora::weather),
                Fringe.CODEC.optionalFieldOf("fringe").forGetter(Aurora::fringe),
                Codec.STRING.listOf().optionalFieldOf("positionSides", List.of()).forGetter(Aurora::positionSides)
        ).apply(i, Aurora::new));
    }

    /** The eclipse corona: additive around the first sun, {@code size} × the sun's size, alpha {@code strength × totality²}. */
    public record Corona(Identifier texture, float size, String color, float strength) {
        static final Codec<Corona> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.optionalFieldOf("texture", sky("corona")).forGetter(Corona::texture),
                Codec.FLOAT.optionalFieldOf("size", 2.5F).forGetter(Corona::size),
                Codec.STRING.optionalFieldOf("color", "#FFFFFF").forGetter(Corona::color),
                Codec.FLOAT.optionalFieldOf("strength", 1.0F).forGetter(Corona::strength)
        ).apply(i, Corona::new));
    }

    /** The diamond-ring flash on the sun's limb just before and after totality ({@code window} of totality). */
    public record DiamondRing(Identifier texture, float size, String color, float window) {
        static final Codec<DiamondRing> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.optionalFieldOf("texture", sky("diamond_ring")).forGetter(DiamondRing::texture),
                Codec.FLOAT.optionalFieldOf("size", 8.0F).forGetter(DiamondRing::size),
                Codec.STRING.optionalFieldOf("color", "#FFFFFF").forGetter(DiamondRing::color),
                Codec.FLOAT.optionalFieldOf("window", 0.1F).forGetter(DiamondRing::window)
        ).apply(i, DiamondRing::new));
    }

    public record Eclipse(String body, float phaseStart, float phaseEnd, float minLight, Optional<Corona> corona,
            Optional<DiamondRing> diamondRing) {
        static final Codec<Eclipse> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("body", "").forGetter(Eclipse::body),
                Codec.FLOAT.optionalFieldOf("phaseStart", 0.0F).forGetter(Eclipse::phaseStart),
                Codec.FLOAT.optionalFieldOf("phaseEnd", 0.0F).forGetter(Eclipse::phaseEnd),
                Codec.FLOAT.optionalFieldOf("minLight", 0.1F).forGetter(Eclipse::minLight),
                Corona.CODEC.optionalFieldOf("corona").forGetter(Eclipse::corona),
                DiamondRing.CODEC.optionalFieldOf("diamondRing").forGetter(Eclipse::diamondRing)
        ).apply(i, Eclipse::new));
    }

    /** Horizon haze; colour and strength lerp to the optional night pair by daylight. */
    public record Haze(String color, float strength, Optional<String> nightColor, Optional<Float> nightStrength) {
        static final Codec<Haze> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("color", "#000000").forGetter(Haze::color),
                Codec.FLOAT.optionalFieldOf("strength", 0.0F).forGetter(Haze::strength),
                Codec.STRING.optionalFieldOf("nightColor").forGetter(Haze::nightColor),
                Codec.FLOAT.optionalFieldOf("nightStrength").forGetter(Haze::nightStrength)
        ).apply(i, Haze::new));
    }

    /**
     * How a weather event tints the sky. The sky-upgrade fields scale with the smoothed intensity: bodies' and decals'
     * alpha × {@code (1 − bodyFade·i)}, stars × {@code (1 − starFade·i)}, haze strength + {@code hazeBoost·i} with its
     * colour pulled toward {@code hazeColor}.
     */
    public record WeatherTint(String sky, String fog, float strengthAtPeak, float bodyFade, float starFade,
            float hazeBoost, Optional<String> hazeColor) {
        static final Codec<WeatherTint> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("sky", "#000000").forGetter(WeatherTint::sky),
                Codec.STRING.optionalFieldOf("fog", "#000000").forGetter(WeatherTint::fog),
                Codec.FLOAT.optionalFieldOf("strengthAtPeak", 0.0F).forGetter(WeatherTint::strengthAtPeak),
                Codec.FLOAT.optionalFieldOf("bodyFade", 0.0F).forGetter(WeatherTint::bodyFade),
                Codec.FLOAT.optionalFieldOf("starFade", 0.0F).forGetter(WeatherTint::starFade),
                Codec.FLOAT.optionalFieldOf("hazeBoost", 0.0F).forGetter(WeatherTint::hazeBoost),
                Codec.STRING.optionalFieldOf("hazeColor").forGetter(WeatherTint::hazeColor)
        ).apply(i, WeatherTint::new));
    }

    /** One colour keyframe: the sky at clock {@code phase} (0..1; 1.0 is the same instant as 0.0). */
    public record GradientKey(float phase, String zenith, String horizon, String fog) {
        static final Codec<GradientKey> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("phase", 0.0F).forGetter(GradientKey::phase),
                Codec.STRING.optionalFieldOf("zenith", "#000000").forGetter(GradientKey::zenith),
                Codec.STRING.optionalFieldOf("horizon", "#000000").forGetter(GradientKey::horizon),
                Codec.STRING.optionalFieldOf("fog", "#000000").forGetter(GradientKey::fog)
        ).apply(i, GradientKey::new));
    }

    /** A zenith / horizon / fog triple (one side of a {@link PositionBlend}). */
    public record SkyColors(String zenith, String horizon, String fog) {
        static final SkyColors BLACK = new SkyColors("#000000", "#000000", "#000000");
        static final Codec<SkyColors> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("zenith", "#000000").forGetter(SkyColors::zenith),
                Codec.STRING.optionalFieldOf("horizon", "#000000").forGetter(SkyColors::horizon),
                Codec.STRING.optionalFieldOf("fog", "#000000").forGetter(SkyColors::fog)
        ).apply(i, SkyColors::new));
    }

    /**
     * Sky colours by the player's position along {@code axis} ("x" or "z"): {@code west} below the band, {@code band}
     * inside it, {@code east} beyond it. The band is centred on 0 and is as wide as the config key named by
     * {@code bandWidthConfig}; colours fade across each edge.
     */
    public record PositionBlend(String axis, String bandWidthConfig, SkyColors west, SkyColors band, SkyColors east) {
        static final Codec<PositionBlend> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("axis", "x").forGetter(PositionBlend::axis),
                Codec.STRING.optionalFieldOf("bandWidthConfig", "").forGetter(PositionBlend::bandWidthConfig),
                SkyColors.CODEC.optionalFieldOf("west", SkyColors.BLACK).forGetter(PositionBlend::west),
                SkyColors.CODEC.optionalFieldOf("band", SkyColors.BLACK).forGetter(PositionBlend::band),
                SkyColors.CODEC.optionalFieldOf("east", SkyColors.BLACK).forGetter(PositionBlend::east)
        ).apply(i, PositionBlend::new));
    }

    /** Rolls the fixed bodies (the station's homeworld disk) once per {@code periodTicks}; 0 = no roll. */
    public record TerminatorScroll(int periodTicks) {
        static final Codec<TerminatorScroll> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("periodTicks", 0).forGetter(TerminatorScroll::periodTicks)
        ).apply(i, TerminatorScroll::new));
    }

    /** {@link Visibility#weatherBoost}: alpha × {@code (1 + strength · intensity)} while {@code event} is active. */
    public record WeatherBoost(String event, float strength) {
        static final Codec<WeatherBoost> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("event", "NONE").forGetter(WeatherBoost::event),
                Codec.FLOAT.optionalFieldOf("strength", 0.0F).forGetter(WeatherBoost::strength)
        ).apply(i, WeatherBoost::new));
    }

    /** When a decal shows: the product of every factor it names (an absent block is always visible). */
    public record Visibility(boolean night, boolean day, Optional<WeatherGate> weather,
            Optional<WeatherBoost> weatherBoost, List<String> positionSides, float pitchBelow, boolean eclipse) {
        static final Codec<Visibility> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("night", false).forGetter(Visibility::night),
                Codec.BOOL.optionalFieldOf("day", false).forGetter(Visibility::day),
                WeatherGate.CODEC.optionalFieldOf("weather").forGetter(Visibility::weather),
                WeatherBoost.CODEC.optionalFieldOf("weatherBoost").forGetter(Visibility::weatherBoost),
                Codec.STRING.listOf().optionalFieldOf("positionSides", List.of()).forGetter(Visibility::positionSides),
                Codec.FLOAT.optionalFieldOf("pitchBelow", 0.0F).forGetter(Visibility::pitchBelow),
                Codec.BOOL.optionalFieldOf("eclipse", false).forGetter(Visibility::eclipse)
        ).apply(i, Visibility::new));
    }

    /**
     * A textured quad anchored in the sky: {@code fixed} (yaw / pitch), {@code sun} (the first sun, plus offsets),
     * {@code antisun} or {@code body} ({@code bodyId}). {@code size} is the height in degrees, {@code aspect} the
     * width over the height; {@code roll + spinDegPerSecond · seconds} turns it. 16 fields, the codec maximum.
     */
    public record Decal(String id, Identifier texture, String anchor, String bodyId, float yaw, float pitch,
            float offsetYaw, float offsetPitch, float size, float aspect, String color, float opacity,
            boolean additive, float roll, float spinDegPerSecond, Optional<Visibility> visibility) {
        static final Codec<Decal> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("id", "").forGetter(Decal::id),
                Identifier.CODEC.optionalFieldOf("texture", sky("nebula_a")).forGetter(Decal::texture),
                Codec.STRING.optionalFieldOf("anchor", "fixed").forGetter(Decal::anchor),
                Codec.STRING.optionalFieldOf("bodyId", "").forGetter(Decal::bodyId),
                Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(Decal::yaw),
                Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(Decal::pitch),
                Codec.FLOAT.optionalFieldOf("offsetYaw", 0.0F).forGetter(Decal::offsetYaw),
                Codec.FLOAT.optionalFieldOf("offsetPitch", 0.0F).forGetter(Decal::offsetPitch),
                Codec.FLOAT.optionalFieldOf("size", 10.0F).forGetter(Decal::size),
                Codec.FLOAT.optionalFieldOf("aspect", 1.0F).forGetter(Decal::aspect),
                Codec.STRING.optionalFieldOf("color", "#FFFFFF").forGetter(Decal::color),
                Codec.FLOAT.optionalFieldOf("opacity", 1.0F).forGetter(Decal::opacity),
                Codec.BOOL.optionalFieldOf("additive", true).forGetter(Decal::additive),
                Codec.FLOAT.optionalFieldOf("roll", 0.0F).forGetter(Decal::roll),
                Codec.FLOAT.optionalFieldOf("spinDegPerSecond", 0.0F).forGetter(Decal::spinDegPerSecond),
                Visibility.CODEC.optionalFieldOf("visibility").forGetter(Decal::visibility)
        ).apply(i, Decal::new));
    }

    /** A tiled cloud deck from the horizon up to {@code altitudeDeg}, optionally lit from below ({@code underglow}). */
    public record CloudLayer(Identifier texture, String color, Optional<String> nightColor, Optional<String> underglow,
            float underglowStrength, float altitudeDeg, float opacity, float driftDegPerSecond, int tilesAround,
            Map<String, Float> weatherOpacity) {
        static final Codec<CloudLayer> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.optionalFieldOf("texture", sky("clouds_ash")).forGetter(CloudLayer::texture),
                Codec.STRING.optionalFieldOf("color", "#FFFFFF").forGetter(CloudLayer::color),
                Codec.STRING.optionalFieldOf("nightColor").forGetter(CloudLayer::nightColor),
                Codec.STRING.optionalFieldOf("underglow").forGetter(CloudLayer::underglow),
                Codec.FLOAT.optionalFieldOf("underglowStrength", 0.5F).forGetter(CloudLayer::underglowStrength),
                Codec.FLOAT.optionalFieldOf("altitudeDeg", 30.0F).forGetter(CloudLayer::altitudeDeg),
                Codec.FLOAT.optionalFieldOf("opacity", 0.5F).forGetter(CloudLayer::opacity),
                Codec.FLOAT.optionalFieldOf("driftDegPerSecond", 0.1F).forGetter(CloudLayer::driftDegPerSecond),
                Codec.INT.optionalFieldOf("tilesAround", 6).forGetter(CloudLayer::tilesAround),
                Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("weatherOpacity", Map.of())
                        .forGetter(CloudLayer::weatherOpacity)
        ).apply(i, CloudLayer::new));
    }

    /** Deterministic local shooting stars (not synced): {@code ratePerMinute} on average, at most {@code maxConcurrent}. */
    public record ShootingStars(float ratePerMinute, int maxConcurrent, Identifier texture, String color,
            float lengthDeg, int durationTicks, boolean nightOnly, float minPitch) {
        static final Codec<ShootingStars> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("ratePerMinute", 2.0F).forGetter(ShootingStars::ratePerMinute),
                Codec.INT.optionalFieldOf("maxConcurrent", 2).forGetter(ShootingStars::maxConcurrent),
                Identifier.CODEC.optionalFieldOf("texture", sky("meteor_streak")).forGetter(ShootingStars::texture),
                Codec.STRING.optionalFieldOf("color", "#FFFFFF").forGetter(ShootingStars::color),
                Codec.FLOAT.optionalFieldOf("lengthDeg", 10.0F).forGetter(ShootingStars::lengthDeg),
                Codec.INT.optionalFieldOf("durationTicks", 12).forGetter(ShootingStars::durationTicks),
                Codec.BOOL.optionalFieldOf("nightOnly", true).forGetter(ShootingStars::nightOnly),
                Codec.FLOAT.optionalFieldOf("minPitch", 12.0F).forGetter(ShootingStars::minPitch)
        ).apply(i, ShootingStars::new));
    }

    /** The sky-upgrade top-level fields, decoded beside the 16 original ones. */
    private record Extras(List<Decal> decals, Optional<CloudLayer> cloudLayer, Optional<ShootingStars> shootingStars) {
    }

    /** The 16 original top-level fields (decodes to a profile whose sky-upgrade fields are still defaults). */
    private static final MapCodec<SkyProfile> CORE = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("vanillaSkybox", false).forGetter(SkyProfile::vanillaSkybox),
            Background.CODEC.optionalFieldOf("background", Background.DEFAULT).forGetter(SkyProfile::background),
            Fog.CODEC.optionalFieldOf("fog", Fog.DEFAULT).forGetter(SkyProfile::fog),
            Stars.CODEC.optionalFieldOf("stars", Stars.DEFAULT).forGetter(SkyProfile::stars),
            Body.CODEC.listOf().optionalFieldOf("sun", List.of()).forGetter(SkyProfile::sun),
            Body.CODEC.listOf().optionalFieldOf("moons", List.of()).forGetter(SkyProfile::moons),
            Body.CODEC.listOf().optionalFieldOf("bodies", List.of()).forGetter(SkyProfile::bodies),
            Galaxy.CODEC.optionalFieldOf("galaxy").forGetter(SkyProfile::galaxy),
            Aurora.CODEC.optionalFieldOf("aurora").forGetter(SkyProfile::aurora),
            Eclipse.CODEC.optionalFieldOf("eclipse").forGetter(SkyProfile::eclipse),
            Codec.STRING.optionalFieldOf("clouds", "none").forGetter(SkyProfile::clouds),
            Haze.CODEC.optionalFieldOf("horizonHaze").forGetter(SkyProfile::horizonHaze),
            Codec.unboundedMap(Codec.STRING, WeatherTint.CODEC).optionalFieldOf("weatherTints", Map.of())
                    .forGetter(SkyProfile::weatherTints),
            GradientKey.CODEC.listOf().optionalFieldOf("gradient", List.of()).forGetter(SkyProfile::gradient),
            PositionBlend.CODEC.optionalFieldOf("positionBlend").forGetter(SkyProfile::positionBlend),
            TerminatorScroll.CODEC.optionalFieldOf("terminatorScroll").forGetter(SkyProfile::terminatorScroll)
    ).apply(i, SkyProfile::new));

    private static final MapCodec<Extras> EXTRAS = RecordCodecBuilder.mapCodec(i -> i.group(
            Decal.CODEC.listOf().optionalFieldOf("decals", List.of()).forGetter(Extras::decals),
            CloudLayer.CODEC.optionalFieldOf("cloudLayer").forGetter(Extras::cloudLayer),
            ShootingStars.CODEC.optionalFieldOf("shootingStars").forGetter(Extras::shootingStars)
    ).apply(i, Extras::new));

    public static final Codec<SkyProfile> CODEC = Codec.mapPair(CORE, EXTRAS).xmap(
            pair -> pair.getFirst().withExtras(pair.getSecond()),
            profile -> Pair.of(profile, new Extras(profile.decals(), profile.cloudLayer(), profile.shootingStars())))
            .codec();

    private SkyProfile withExtras(Extras e) {
        return new SkyProfile(this.vanillaSkybox, this.background, this.fog, this.stars, this.sun, this.moons,
                this.bodies, this.galaxy, this.aurora, this.eclipse, this.clouds, this.horizonHaze, this.weatherTints,
                this.gradient, this.positionBlend, this.terminatorScroll, e.decals(), e.cloudLayer(),
                e.shootingStars());
    }

    // Parsed colours by string: profiles hold a few dozen distinct strings, and the sky reads them every frame, so
    // each is parsed once (a lookup allocates nothing). Bounded in case a resource pack churns through colours.
    private static final class RgbCache {
        static final java.util.concurrent.ConcurrentHashMap<String, Integer> MAP =
                new java.util.concurrent.ConcurrentHashMap<>();
    }

    /**
     * Parses {@code "#RRGGBB"} (or {@code "RRGGBB"}) into 0xRRGGBB; malformed input yields black. Each distinct string
     * is parsed once and remembered, so the per-frame sky calls allocate nothing.
     */
    public static int rgb(String hex) {
        if (hex == null) {
            return 0;
        }
        Integer known = RgbCache.MAP.get(hex);
        if (known != null) {
            return known;
        }
        int value = parseRgb(hex);
        if (RgbCache.MAP.size() < 1024) {
            RgbCache.MAP.put(hex, value);
        }
        return value;
    }

    private static int parseRgb(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return Integer.parseInt(h, 16) & 0xFFFFFF;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
