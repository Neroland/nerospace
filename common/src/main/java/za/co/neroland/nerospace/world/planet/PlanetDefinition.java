package za.co.neroland.nerospace.world.planet;

import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.api.PlanetId;
import za.co.neroland.nerospace.api.PlanetTraits;

/**
 * Server-side definition of one Nerospace world: its traits, clock, weather, solar constant, terraform payoff,
 * villager culture and the handful of per-planet numbers that used to be hardcoded across the mod. One subclass
 * per planet lives in {@code world/planet/<name>/}; {@link Planets} is the registry.
 *
 * <p>Plain Java on purpose: the table is available during static init and in unit tests (no registry event, no
 * datapack). Nothing here is persisted — saved data refers to planets by dimension id.</p>
 */
public abstract class PlanetDefinition {

    private final PlanetTraits traits;
    private final String langId;
    private final String englishName;
    private final int unlockTier;
    private final Optional<Identifier> coreGate;
    private final ClockSource clockSource;
    private final double pinnedPhase;
    private final DaylightCurve daylight;
    private final double planetSolar;
    private final WeatherParams weather;
    private final long structureSalt;
    private final boolean meteorSurface;
    private final double miningSpeed;
    private final int minerTier;
    private final TreeStyle treeStyle;
    @Nullable
    private final Supplier<EntityType<?>> livestock;

    protected PlanetDefinition(Spec spec) {
        this.traits = spec.traits;
        this.langId = spec.langId;
        this.englishName = spec.englishName;
        this.unlockTier = spec.unlockTier;
        this.coreGate = spec.coreGate;
        this.clockSource = spec.clockSource;
        this.pinnedPhase = spec.pinnedPhase;
        this.daylight = spec.daylight;
        this.planetSolar = spec.planetSolar;
        this.weather = spec.weather;
        this.structureSalt = spec.structureSalt;
        this.meteorSurface = spec.meteorSurface;
        this.miningSpeed = spec.miningSpeed;
        this.minerTier = spec.minerTier;
        this.treeStyle = spec.treeStyle;
        this.livestock = spec.livestock;
    }

    /** The planet id ({@code nerospace:<name>}). */
    public PlanetId id() {
        return this.traits.planet();
    }

    /** The dimension this planet lives in ({@code == id().dimension()}). */
    public ResourceKey<Level> dimension() {
        return this.traits.planet().dimension();
    }

    /** The planet's traits as stored (gravity unscaled by config). */
    public PlanetTraits traits() {
        return this.traits;
    }

    /** Short id used for lang keys ({@code destination.nerospace.<langId>}). */
    public String langId() {
        return this.langId;
    }

    /** Stable English name for logs, telemetry and the public route API. Never localised, never changed. */
    public String englishName() {
        return this.englishName;
    }

    /** The lowest rocket tier (1..4) that can fly here. */
    public int unlockTier() {
        return this.unlockTier;
    }

    /** A Neroland Core progression gate the pilot must have open before launching here, if any. */
    public Optional<Identifier> coreGate() {
        return this.coreGate;
    }

    public ClockSource clockSource() {
        return this.clockSource;
    }

    /** The phase a fixed clock sits at (only read when {@code traits().fixedClock()}). */
    public double pinnedPhase() {
        return this.pinnedPhase;
    }

    /**
     * The clock phases a bed may be used in on this planet (moving clocks only; fixed clocks decide by place, see
     * {@link PlanetSleepRules}). Override to give a planet its own night.
     */
    public PlanetSleepRules.Window sleepWindow() {
        return PlanetSleepRules.DEFAULT_WINDOW;
    }

    /** Daylight in [0,1] as a function of clock phase. */
    public DaylightCurve daylight() {
        return this.daylight;
    }

    /** Solar constant: a ground panel's factor at full daylight (1.6.0 was a flat 2.0 on every Nero world). */
    public double planetSolar() {
        return this.planetSolar;
    }

    /**
     * The config key that switches this planet's weather on or off ({@code weather.<langId>.enabled}). Every planet
     * with weather must have this key in the config schema ({@code ConfigSchemaCompatTest} checks it).
     */
    public String weatherConfigKey() {
        return "weather." + this.langId + ".enabled";
    }

    /** Default weather tuning (before config multipliers). */
    public WeatherParams weather() {
        return this.weather;
    }

    /**
     * Whether this planet's weather event may begin at clock {@code phase} (Cindara never during its eclipse,
     * Glacira only in the dark). Default: any time.
     */
    public boolean weatherMayStart(double phase) {
        return true;
    }

    /** Per-planet structure placement salt; 0 for Greenxertz (the only 1.x world with structures) so its structure positions never move. */
    public long structureSalt() {
        return this.structureSalt;
    }

    /** Whether natural meteors fall here (surface worlds only). */
    public boolean meteorSurface() {
        return this.meteorSurface;
    }

    /** Quarry speed multiplier on this planet. */
    public double miningSpeed() {
        return this.miningSpeed;
    }

    /** The lowest quarry miner tier that can operate here. */
    public int minerTier() {
        return this.minerTier;
    }

    /** The mature (stage 3) terraformed biome, or empty where the terraformer refuses. */
    public Optional<ResourceKey<Biome>> terraformLiving() {
        return this.traits.terraformTarget();
    }

    /**
     * This planet's terraform table: the biome trio, surface swap, hydration liquid, flora, fauna, ore seeds
     * and planet rules. Empty where the terraformer refuses (the station).
     */
    public Optional<TerraformTable> terraform() {
        return TerraformTables.forLangId(this.langId);
    }

    /** Which tree family grows on this planet's Living ground. */
    public TreeStyle treeStyle() {
        return this.treeStyle;
    }

    /** The livestock seeded on Living ground, or null for none. */
    @Nullable
    public EntityType<?> livestock() {
        return this.livestock == null ? null : this.livestock.get();
    }

    /**
     * Whether this planet's dimension exists on {@code server}. Planets whose dimension is not shipped yet (or
     * was removed by a datapack) are hidden from rockets, launch controllers, compasses and commands.
     */
    public boolean isAvailable(@Nullable MinecraftServer server) {
        return server != null && server.getLevel(dimension()) != null;
    }

    /**
     * Where a rocket with no pad to land on searches for its Landing Pod site: X/Z only, Y ignored.
     * {@code (0, 0)} for every planet unless a planet package overrides it. Not config, not saved.
     */
    public net.minecraft.core.BlockPos arrivalAnchor() {
        return net.minecraft.core.BlockPos.ZERO;
    }

    @Override
    public String toString() {
        return id().asString();
    }

    protected static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, path);
    }

    protected static PlanetId planet(String path) {
        return new PlanetId(id(path));
    }

    protected static ResourceKey<Biome> biome(String path) {
        return ResourceKey.create(Registries.BIOME, id(path));
    }

    /** Constructor arguments, so each planet class reads as a table row. */
    protected static final class Spec {
        PlanetTraits traits;
        String langId;
        String englishName;
        int unlockTier = 1;
        Optional<Identifier> coreGate = Optional.empty();
        ClockSource clockSource = ClockSource.PLANET_CLOCK;
        double pinnedPhase = 0.25;
        DaylightCurve daylight = DaylightCurve.constant(1.0);
        double planetSolar = 2.0;
        WeatherParams weather = WeatherParams.NONE;
        long structureSalt;
        boolean meteorSurface = true;
        double miningSpeed = 1.0;
        int minerTier = 1;
        TreeStyle treeStyle = TreeStyle.OAK_BIRCH;
        @Nullable
        Supplier<EntityType<?>> livestock;

        public Spec(PlanetTraits traits, String langId, String englishName) {
            this.traits = traits;
            this.langId = langId;
            this.englishName = englishName;
        }

        public Spec unlockTier(int tier) {
            this.unlockTier = tier;
            return this;
        }

        public Spec coreGate(Identifier gate) {
            this.coreGate = Optional.of(gate);
            return this;
        }

        public Spec clock(ClockSource source, double pinned) {
            this.clockSource = source;
            this.pinnedPhase = pinned;
            return this;
        }

        public Spec daylight(DaylightCurve curve, double solar) {
            this.daylight = curve;
            this.planetSolar = solar;
            return this;
        }

        public Spec weather(WeatherParams params) {
            this.weather = params;
            return this;
        }

        public Spec salt(long salt) {
            this.structureSalt = salt;
            return this;
        }

        public Spec noMeteors() {
            this.meteorSurface = false;
            return this;
        }

        public Spec mining(double speed, int tier) {
            this.miningSpeed = speed;
            this.minerTier = tier;
            return this;
        }

        public Spec terraform(TreeStyle trees, @Nullable Supplier<EntityType<?>> fauna) {
            this.treeStyle = trees;
            this.livestock = fauna;
            return this;
        }
    }
}
