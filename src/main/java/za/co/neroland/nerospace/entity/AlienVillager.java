package za.co.neroland.nerospace.entity;

import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerospace.registry.ModDimensions;

/**
 * Alien Villager (Alien Villagers &amp; Structures, Phase 0) — the social alien NPC of the nerospace
 * planets. For now it is a harmless, <b>wary-neutral</b> wanderer: it strolls its home biome, watches
 * the player and backs off if approached too closely, but never attacks. Trading, reputation and the
 * teach-and-grow village loop arrive in later phases.
 *
 * <p>Each villager carries a {@code Variant} (planet, home-biome id, colour seed) so it can look
 * native to where it spawned. The variant is synced to clients (for the render-layer stack added in
 * Phase 1) and persisted in NBT. It is assigned lazily on the first server tick from the dimension and
 * biome the villager is standing in, so naturally-spawned and command-spawned villagers both get one.
 */
public class AlienVillager extends PathfinderMob {

    /** Which planet's species this villager belongs to (drives palette + silhouette later). */
    public enum Planet {
        GREENXERTZ, CINDARA, GLACIRA;

        public static Planet byOrdinal(int i) {
            Planet[] values = values();
            return values[Math.floorMod(i, values.length)];
        }
    }

    private static final EntityDataAccessor<Integer> DATA_PLANET =
            SynchedEntityData.defineId(AlienVillager.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_BIOME =
            SynchedEntityData.defineId(AlienVillager.class, EntityDataSerializers.STRING);
    /** A 32-bit per-individual seed; palette jitter (Phase 1) is derived from it. */
    private static final EntityDataAccessor<Integer> DATA_COLOR_SEED =
            SynchedEntityData.defineId(AlienVillager.class, EntityDataSerializers.INT);

    /** Server-side: whether the lazy variant assignment has run (biome == "" is the unset sentinel). */
    private boolean variantAssigned;

    public AlienVillager(EntityType<? extends AlienVillager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PLANET, Planet.GREENXERTZ.ordinal());
        builder.define(DATA_BIOME, "");
        builder.define(DATA_COLOR_SEED, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Wary, not panicked: it edges away from a nearby player at a calm pace rather than fleeing.
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 4.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && !this.variantAssigned) {
            assignVariant();
            this.variantAssigned = true;
        }
    }

    /** Derives the variant from the dimension + biome the villager is standing in. */
    private void assignVariant() {
        setColorSeed(this.random.nextInt() | 1); // avoid the 0 sentinel-ish value; any 32-bit seed is fine
        setPlanet(planetForDimension());
        Holder<Biome> biome = this.level().getBiome(this.blockPosition());
        biome.unwrapKey().ifPresent(key -> setBiomeId(key.identifier().toString()));
    }

    private Planet planetForDimension() {
        var dim = this.level().dimension();
        if (dim == ModDimensions.CINDARA_LEVEL) {
            return Planet.CINDARA;
        }
        if (dim == ModDimensions.GLACIRA_LEVEL) {
            return Planet.GLACIRA;
        }
        return Planet.GREENXERTZ;
    }

    // --- Variant accessors -----------------------------------------------------

    public Planet getPlanet() {
        return Planet.byOrdinal(this.entityData.get(DATA_PLANET));
    }

    public void setPlanet(Planet planet) {
        this.entityData.set(DATA_PLANET, planet.ordinal());
    }

    public String getBiomeId() {
        return this.entityData.get(DATA_BIOME);
    }

    public void setBiomeId(String id) {
        this.entityData.set(DATA_BIOME, id);
    }

    public int getColorSeed() {
        return this.entityData.get(DATA_COLOR_SEED);
    }

    public void setColorSeed(int seed) {
        this.entityData.set(DATA_COLOR_SEED, seed);
    }

    // --- Persistence -----------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Planet", this.entityData.get(DATA_PLANET));
        output.putString("HomeBiome", getBiomeId());
        output.putInt("ColorSeed", getColorSeed());
        output.putBoolean("VariantAssigned", this.variantAssigned);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_PLANET, input.getIntOr("Planet", Planet.GREENXERTZ.ordinal()));
        setBiomeId(input.getStringOr("HomeBiome", ""));
        setColorSeed(input.getIntOr("ColorSeed", 0));
        this.variantAssigned = input.getBooleanOr("VariantAssigned", false);
    }

    // --- Sounds (vanilla villager voice for now; bespoke sounds can come later) -

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }
}
