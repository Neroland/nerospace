package za.co.neroland.nerospace.world.planet.mars.entity;

import java.util.OptionalInt;

import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.menu.MenuOpener;
import za.co.neroland.nerospace.village.AlienTrades;

/**
 * Salvage Drone — the only merchant on Mars. It hovers over a Rover Wreck and trades salvaged rover parts for
 * raw hematite. Deliberately simpler than an Alien Villager: <b>no reputation, no village core, no per-player
 * state at all</b> — every player sees the same single-tier stock, and nothing about who traded is kept, in memory
 * or on disk (POPIA/GDPR: nothing to erase).
 *
 * <p>Stock is limited: the offers (with their uses) are saved with the drone as {@code Offers}, using the vanilla
 * merchant-offer codec, which holds items and counts only, never a player. The list restocks once a day
 * ({@link SalvageStock}), so the {@code maxUses} of each row are a real daily cap.</p>
 */
public class SalvageDrone extends PathfinderMob implements Merchant {

    private Player tradingPlayer;
    private MerchantOffers offers;
    private long lastRestock = SalvageStock.NEVER;

    public SalvageDrone(EntityType<? extends SalvageDrone> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 12.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        // A slow hover bob; it never wanders off its wreck.
        this.setDeltaMovement(new Vec3(0.0D, Math.sin(this.tickCount * 0.08D) * 0.01D, 0.0D));
        if (!this.level().isClientSide()) {
            if (this.tradingPlayer != null
                    && (this.tradingPlayer.isRemoved() || this.distanceToSqr(this.tradingPlayer) > 100.0D)) {
                this.setTradingPlayer(null);
            }
        } else if (this.tickCount % 20 == 0) {
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.2D, this.getZ(),
                    0.0D, 0.02D, 0.0D);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (this.getTradingPlayer() == null && this.isAlive()) {
            restockIfDue();
            this.setTradingPlayer(player);
            OptionalInt opt = MenuOpener.open(player, new SimpleMenuProvider(
                    (id, inv, p) -> new MerchantMenu(id, inv, this), this.getDisplayName()));
            if (opt.isEmpty()) {
                this.setTradingPlayer(null);
                return InteractionResult.CONSUME;
            }
            player.sendMerchantOffers(opt.getAsInt(), this.getOffers(), 1, 0, false, false);
        }
        return InteractionResult.SUCCESS;
    }

    /** Rebuilds the whole price list (every row back to full) when it was never stocked or a day has passed. */
    private void restockIfDue() {
        long now = this.level().getGameTime();
        if (this.offers == null || SalvageStock.due(now, this.lastRestock)) {
            this.offers = AlienTrades.resolve(AlienTrades.SALVAGE_TABLE, 1);
            this.lastRestock = now;
        }
    }

    // --- Merchant (no reputation, no player record) -----------------------------------------------------------

    @Override
    public void setTradingPlayer(Player player) {
        // The offers stay: their uses are the drone's stock, not a per-window list.
        this.tradingPlayer = player;
    }

    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            restockIfDue();
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers newOffers) {
        this.offers = newOffers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.4D, this.getZ(),
                    6, 0.2D, 0.2D, 0.2D, 0.05D);
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_WORK_TOOLSMITH;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.tradingPlayer == player && this.isAlive() && this.distanceToSqr(player) <= 100.0D;
    }

    // --- Persistence (stock only: items, counts and uses; no player field) -------------------------------------

    //? if >=1.21.6 {
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    //?} else {
    /*@Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ValueOutput output = ValueOutput.of(tag, this.registryAccess());
    *///?}
        output.storeNullable("Offers", MerchantOffers.CODEC, this.offers);
        output.putLong("LastRestock", this.lastRestock);
    }

    //? if >=1.21.6 {
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
    //?} else {
    /*@Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ValueInput input = ValueInput.of(tag, this.registryAccess());
    *///?}
        this.offers = input.read("Offers", MerchantOffers.CODEC).orElse(null);
        this.lastRestock = input.getLongOr("LastRestock", SalvageStock.NEVER);
    }

    // --- Sounds --------------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEACON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.3F;
    }
}
