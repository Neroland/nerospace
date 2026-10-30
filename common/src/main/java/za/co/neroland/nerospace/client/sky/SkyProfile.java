package za.co.neroland.nerospace.client.sky;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
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
 */
public record SkyProfile(boolean vanillaSkybox, Background background, Fog fog, Stars stars, List<Body> sun,
        List<Body> moons, List<Body> bodies, Optional<Galaxy> galaxy, Optional<Aurora> aurora,
        Optional<Eclipse> eclipse, String clouds, Optional<Haze> horizonHaze, Map<String, WeatherTint> weatherTints,
        List<GradientKey> gradient, Optional<PositionBlend> positionBlend, Optional<TerminatorScroll> terminatorScroll) {

    /** "Keep the vanilla sky": the profile used for any planet without a file. */
    public static final SkyProfile VANILLA = new SkyProfile(true, Background.DEFAULT, Fog.DEFAULT, Stars.DEFAULT,
            List.of(), List.of(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(), "none",
            Optional.empty(), Map.of(), List.of(), Optional.empty(), Optional.empty());

    public record Background(String zenith, String horizon, String nightZenith, String nightHorizon, String blend) {
        static final Background DEFAULT = new Background("#000000", "#000000", "#000000", "#000000", "daylight");
        static final Codec<Background> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("zenith", "#000000").forGetter(Background::zenith),
                Codec.STRING.optionalFieldOf("horizon", "#000000").forGetter(Background::horizon),
                Codec.STRING.optionalFieldOf("nightZenith", "#000000").forGetter(Background::nightZenith),
                Codec.STRING.optionalFieldOf("nightHorizon", "#000000").forGetter(Background::nightHorizon),
                Codec.STRING.optionalFieldOf("blend", "daylight").forGetter(Background::blend)
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

    public record Stars(int count, float brightness, boolean alwaysVisible, String tint) {
        static final Stars DEFAULT = new Stars(1500, 0.6F, false, "#FFFFFF");
        static final Codec<Stars> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("count", 1500).forGetter(Stars::count),
                Codec.FLOAT.optionalFieldOf("brightness", 0.6F).forGetter(Stars::brightness),
                Codec.BOOL.optionalFieldOf("alwaysVisible", false).forGetter(Stars::alwaysVisible),
                Codec.STRING.optionalFieldOf("tint", "#FFFFFF").forGetter(Stars::tint)
        ).apply(i, Stars::new));
    }

    public record FixedAngle(float yaw, float pitch) {
        static final Codec<FixedAngle> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(FixedAngle::yaw),
                Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(FixedAngle::pitch)
        ).apply(i, FixedAngle::new));
    }

    public record Body(String id, Optional<Identifier> texture, float size, String color, float glow,
            float orbitPeriodDays, float phaseOffset, float inclination, Optional<FixedAngle> fixedAngle,
            boolean phases) {
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
                Codec.BOOL.optionalFieldOf("phases", false).forGetter(Body::phases)
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

    public record Aurora(List<String> colors, float opacity, float speed, boolean nightOnly,
            Optional<String> boostOnWeather) {
        static final Codec<Aurora> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.listOf().optionalFieldOf("colors", List.of()).forGetter(Aurora::colors),
                Codec.FLOAT.optionalFieldOf("opacity", 0.0F).forGetter(Aurora::opacity),
                Codec.FLOAT.optionalFieldOf("speed", 1.0F).forGetter(Aurora::speed),
                Codec.BOOL.optionalFieldOf("nightOnly", true).forGetter(Aurora::nightOnly),
                Codec.STRING.optionalFieldOf("boostOnWeather").forGetter(Aurora::boostOnWeather)
        ).apply(i, Aurora::new));
    }

    public record Eclipse(String body, float phaseStart, float phaseEnd, float minLight) {
        static final Codec<Eclipse> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("body", "").forGetter(Eclipse::body),
                Codec.FLOAT.optionalFieldOf("phaseStart", 0.0F).forGetter(Eclipse::phaseStart),
                Codec.FLOAT.optionalFieldOf("phaseEnd", 0.0F).forGetter(Eclipse::phaseEnd),
                Codec.FLOAT.optionalFieldOf("minLight", 0.1F).forGetter(Eclipse::minLight)
        ).apply(i, Eclipse::new));
    }

    public record Haze(String color, float strength) {
        static final Codec<Haze> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("color", "#000000").forGetter(Haze::color),
                Codec.FLOAT.optionalFieldOf("strength", 0.0F).forGetter(Haze::strength)
        ).apply(i, Haze::new));
    }

    public record WeatherTint(String sky, String fog, float strengthAtPeak) {
        static final Codec<WeatherTint> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("sky", "#000000").forGetter(WeatherTint::sky),
                Codec.STRING.optionalFieldOf("fog", "#000000").forGetter(WeatherTint::fog),
                Codec.FLOAT.optionalFieldOf("strengthAtPeak", 0.0F).forGetter(WeatherTint::strengthAtPeak)
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

    public static final Codec<SkyProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
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

    /** Parses {@code "#RRGGBB"} (or {@code "RRGGBB"}) into 0xRRGGBB; malformed input yields black. */
    public static int rgb(String hex) {
        if (hex == null) {
            return 0;
        }
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return Integer.parseInt(h, 16) & 0xFFFFFF;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
