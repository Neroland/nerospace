package za.co.neroland.nerospace.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
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
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.village.AlienTrades;
import za.co.neroland.nerospace.village.Reputation;

/**
 * Alien Villager (ALIEN_VILLAGERS_DESIGN.md). A social alien NPC of the nerospace planets: a
 * wary-neutral wanderer that the player wins over to unlock trades.
 *
 * <p>Phase 0/1: wanders its home biome, carries a per-individual variant (planet, biome, colour seed)
 * that drives a unique render tint + per-biome skin.
 *
 * <p>Phase 2: it is now a {@link Merchant}. Each villager tracks a per-player reputation score
 * (0..{@link Reputation#MAX}) -> 6 tiers. Gifting palette-appropriate goods raises reputation; at T1+
 * the villager opens the vanilla trading screen with a tier-gated offer list ({@link AlienTrades}).
 * Completing trades nudges reputation up. Reputation is stored on the villager for now; the Village
 * Core block (Phase 3/4) will aggregate it per-village.
 */
public class AlienVillager extends PathfinderMob implements Merchant {

    /** Which planet's species this villager belongs to (drives palette + silhouette later). */
    public enum Planet {
        GREENXERTZ, CINDARA, GLACIRA;

        public static Planet byOrdinal(int i) {
            Planet[] values = values();
            return values[Math.floorMod(i, values.length)];
        }
    }

    private static final @NonNull EntityDataAccessor<Integer> DATA_PLANET =
            NerospaceCommon.requireNonNull(SynchedEntityData.defineId(AlienVillager.class, EntityDataSerializers.INT));
    private static final @NonNull EntityDataAccessor<@org.jspecify.annotations.Nullable String> DATA_BIOME =
            NerospaceCommon.requireNonNull(SynchedEntityData.defineId(AlienVillager.class, EntityDataSerializers.STRING));
    private static final @NonNull EntityDataAccessor<Integer> DATA_COLOR_SEED =
            NerospaceCommon.requireNonNull(SynchedEntityData.defineId(AlienVillager.class, EntityDataSerializers.INT));
    private static final @NonNull EntityDataAccessor<Integer> DATA_DISPLAY_TIER =
            NerospaceCommon.requireNonNull(SynchedEntityData.defineId(AlienVillager.class, EntityDataSerializers.INT));

    private static final @NonNull Codec<Map<String, Integer>> REP_CODEC =
            NerospaceCommon.requireNonNull(Codec.unboundedMap(Codec.STRING, Codec.INT));

    private boolean variantAssigned;

    private final Map<UUID, Integer> reputation = new HashMap<>();

    private Player tradingPlayer;
    private MerchantOffers offers;
    private int villagerXp;

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
        builder.define(DATA_DISPLAY_TIER, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 4.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (!this.variantAssigned) {
                assignVariant();
                this.variantAssigned = true;
            }
            Player player = this.tradingPlayer;
            if (player != null && (player.isRemoved() || this.distanceToSqr(NerospaceCommon.requireNonNull(player)) > 100.0D)) {
                this.setTradingPlayer(null);
            }
        }
    }

    private void assignVariant() {
        setColorSeed(this.random.nextInt() | 1);
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

    // --- Interaction: gifts + trading -----------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean gift = isGift(held);
        boolean canTrade = getTier(player) >= 1 && !this.isBaby();

        if (!gift && !canTrade) {
            if (!this.level().isClientSide()) {
                this.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (gift) {
            receiveGift(player, held);
            return InteractionResult.SUCCESS;
        }
        if (this.getTradingPlayer() == null && this.isAlive()) {
            startTrading(player);
        }
        return InteractionResult.SUCCESS;
    }

    private void startTrading(Player player) {
        rebuildOffers(player);
        this.setTradingPlayer(player);
        OptionalInt opt = player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new MerchantMenu(id, inv, this),
                NerospaceCommon.requireNonNull(this.getDisplayName())));
        if (opt.isPresent()) {
            player.sendMerchantOffers(opt.getAsInt(), this.getOffers(), 1, this.getVillagerXp(),
                    this.showProgressBar(), false);
        }
    }

    private void receiveGift(Player player, ItemStack held) {
        int gain = giftValue(held);
        if (gain <= 0) {
            return;
        }
        held.consume(1, player);
        addReputation(player, gain);
        this.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    this.getX(), this.getY() + 1.6D, this.getZ(), 5, 0.3D, 0.3D, 0.3D, 0.0D);
        }
    }

    private static boolean isGift(ItemStack stack) {
        return !stack.isEmpty() && giftValue(stack) > 0;
    }

    private static int giftValue(ItemStack stack) {
        if (stack.is(ModItems.XERTZ_QUARTZ.get())) {
            return 3;
        }
        if (stack.is(ModItems.NEROSIUM_INGOT.get())) {
            return 5;
        }
        if (stack.is(ModItems.ALIEN_FRAGMENT.get())) {
            return 6;
        }
        if (stack.is(Items.EMERALD)) {
            return 4;
        }
        return 0;
    }

    // --- Reputation -----------------------------------------------------------

    public int getReputation(Player player) {
        return this.reputation.getOrDefault(player.getUUID(), 0);
    }

    public int getTier(Player player) {
        return Reputation.tier(getReputation(player));
    }

    public void addReputation(Player player, int amount) {
        int value = Reputation.clamp(getReputation(player) + amount);
        this.reputation.put(player.getUUID(), value);
        refreshDisplayTier();
        if (this.tradingPlayer == player) {
            this.offers = null;
        }
    }

    private void refreshDisplayTier() {
        int best = 0;
        for (int score : this.reputation.values()) {
            best = Math.max(best, Reputation.tier(score));
        }
        this.entityData.set(DATA_DISPLAY_TIER, best);
    }

    public int getDisplayTier() {
        return this.entityData.get(DATA_DISPLAY_TIER);
    }

    private void rebuildOffers(Player player) {
        int tier = player != null ? getTier(player) : 1;
        this.offers = AlienTrades.forTier(Math.max(1, tier));
    }

    // --- Merchant -------------------------------------------------------------

    @Override
    public void setTradingPlayer(Player player) {
        this.tradingPlayer = player;
        if (player == null) {
            this.offers = null;
        }
    }

    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            rebuildOffers(this.tradingPlayer);
        }
        return NerospaceCommon.requireNonNull(this.offers);
    }

    @Override
    public void overrideOffers(MerchantOffers newOffers) {
        this.offers = newOffers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        this.villagerXp += Math.max(1, offer.getXp());
        if (this.tradingPlayer != null) {
            addReputation(this.tradingPlayer, 1);
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        // No price-demand simulation in Phase 2.
    }

    @Override
    public int getVillagerXp() {
        return this.villagerXp;
    }

    @Override
    public void overrideXp(int xp) {
        this.villagerXp = xp;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.tradingPlayer == player && this.isAlive() && this.distanceToSqr(player) <= 100.0D;
    }

    // --- Variant accessors -----------------------------------------------------

    public Planet getPlanet() {
        return Planet.byOrdinal(this.entityData.get(DATA_PLANET));
    }

    public void setPlanet(Planet planet) {
        this.entityData.set(DATA_PLANET, planet.ordinal());
    }

    public @NonNull String getBiomeId() {
        return NerospaceCommon.requireNonNull(this.entityData.get(DATA_BIOME));
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
        output.putString("HomeBiome", NerospaceCommon.requireNonNull(getBiomeId()));
        output.putInt("ColorSeed", getColorSeed());
        output.putBoolean("VariantAssigned", this.variantAssigned);
        output.putInt("VillagerXp", this.villagerXp);
        Map<String, Integer> serial = new HashMap<>();
        this.reputation.forEach((uuid, score) -> serial.put(uuid.toString(), score));
        output.store("Reputation", REP_CODEC, serial);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_PLANET, input.getIntOr("Planet", Planet.GREENXERTZ.ordinal()));
        setBiomeId(input.getStringOr("HomeBiome", ""));
        setColorSeed(input.getIntOr("ColorSeed", 0));
        this.variantAssigned = input.getBooleanOr("VariantAssigned", false);
        this.villagerXp = input.getIntOr("VillagerXp", 0);
        this.reputation.clear();
        Optional<Map<String, Integer>> stored = input.read("Reputation", REP_CODEC);
        stored.ifPresent(map -> map.forEach((key, score) -> {
            try {
                this.reputation.put(UUID.fromString(key), score);
            } catch (IllegalArgumentException ignored) {
                // skip malformed UUID keys
            }
        }));
        refreshDisplayTier();
    }

    // --- Sounds ---------------------------------------------------------------

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
