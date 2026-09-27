package za.co.neroland.nerospace.route;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.menu.MenuOpener;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.rocket.LaunchPadMultiblock;
import za.co.neroland.nerospace.rocket.RocketEntity;
import za.co.neroland.nerospace.rocket.RocketLaunchPadBlock;
import za.co.neroland.nerospace.rocket.RocketTier;

/**
 * The uncrewed cargo rocket. It reuses everything about {@link RocketEntity} that matters — the fuel tank
 * an adjacent Fuel Tank / Launch Controller / the Cargo Pad pumps into, the tier-2 hull and livery, the
 * ascent animation — and removes the crew: nobody can board it, it has no flight console, and it never
 * decides to launch. The Cargo Pad it stands on validates and records the flight, then calls
 * {@link #liftOff()}; when the ascent counter completes the entity simply vanishes, because the flight is
 * a server-side timer that materialises a fresh rocket on the destination pad.
 *
 * <p>Right-clicking it with a fuel bucket/canister fuels it (inherited); an empty hand opens the Cargo Pad
 * GUI underneath. Breaking it returns the Cargo Rocket item (and any fuel container in its intake).</p>
 */
public class CargoRocketEntity extends RocketEntity {

    /** Fuel this hull carries — enough for a full pad to the heaviest planet and back. */
    public static final int FUEL_CAPACITY = 12_000;

    public CargoRocketEntity(EntityType<? extends CargoRocketEntity> type, Level level) {
        super(type, level);
    }

    /** Spawn helper: a cargo rocket standing on the plate surface of the pad block at {@code pad}. */
    public static CargoRocketEntity standOn(Level level, BlockPos pad) {
        CargoRocketEntity rocket = new CargoRocketEntity(ModEntities.CARGO_ROCKET.get(), level);
        rocket.setPos(pad.getX() + 0.5D, pad.getY() + RocketLaunchPadBlock.SURFACE_HEIGHT, pad.getZ() + 0.5D);
        rocket.setTier(RocketTier.TIER_2);
        return rocket;
    }

    @Override
    public float visualScale() {
        return 2.2F;
    }

    /** Uncrewed: nobody boards a cargo rocket. */
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    /** The pad drives launches; the inherited readiness (and the crewed Launch button) never lights up. */
    @Override
    public boolean isLaunchReady() {
        return false;
    }

    @Override
    public boolean canLaunch() {
        return false;
    }

    /** A bigger tank than the tier-2 hull it borrows its looks from. */
    @Override
    public int fuelCapacity() {
        return FUEL_CAPACITY;
    }

    /** Burns {@code mb} from the tank (server). @return what was actually removed. */
    public int burnFuel(int mb) {
        return this.drainFuelRaw(mb);
    }

    /** Called by the Cargo Pad once the flight is recorded: play the ascent, then vanish. */
    public void liftOff() {
        if (!level().isClientSide() && !isLaunching()) {
            beginAscent();
        }
    }

    @Override
    protected void finishAscent() {
        // The flight record already exists; the hull is expended here and rebuilt on arrival.
        dropFuelInput();
        this.discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitLocation) {
        if (isLaunching()) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.ROCKET_FUEL_BUCKET.get()) || held.is(ModItems.ROCKET_FUEL_CANISTER.get())) {
            return super.interact(player, hand, hitLocation);
        }
        if (!level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            CargoPadBlockEntity pad = padBelow();
            return pad == null ? InteractionResult.CONSUME : MenuOpener.openOrConsume(serverPlayer, pad);
        }
        return InteractionResult.SUCCESS;
    }

    /** The Cargo Pad block entity in the pad cluster this rocket stands on, or {@code null}. */
    @Nullable
    public CargoPadBlockEntity padBelow() {
        BlockPos feet = this.blockPosition();
        BlockPos origin = level().getBlockState(feet).getBlock() instanceof RocketLaunchPadBlock ? feet : feet.below();
        for (BlockPos pad : LaunchPadMultiblock.connectedPads(level(), origin)) {
            if (level().getBlockEntity(pad) instanceof CargoPadBlockEntity cargoPad) {
                return cargoPad;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.CARGO_ROCKET.get());
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource damageSource, float amount) {
        if (this.isRemoved() || isLaunching()) {
            return false;
        }
        if (damageSource.getEntity() instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                this.spawnAtLocation(level, new ItemStack(ModItems.CARGO_ROCKET.get()));
            }
            dropFuelInput();
            this.discard();
            return true;
        }
        return false;
    }
}
