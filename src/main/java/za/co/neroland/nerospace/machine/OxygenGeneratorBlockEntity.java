package za.co.neroland.nerospace.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Oxygen Generator (Phase 8c/8d): a machine that projects a breathable bubble while powered. It runs
 * on an internal energy buffer (exposed via {@code Capabilities.Energy.BLOCK}) that is replenished by
 * <b>burning fuel</b> — coal, charcoal, a blaze rod, or a rocket fuel canister placed in its single
 * fuel slot (right-click to insert; hopper-fed for automation). While burning it charges the buffer
 * and stays active; once the fuel and buffer are spent it goes idle and the bubble collapses.
 * {@code GreenxertzAtmosphere} treats the area within {@code oxygenBubbleRadius} of an active
 * generator as oxygenated.
 */
public class OxygenGeneratorBlockEntity extends BlockEntity {

    public static final int ENERGY_CAPACITY = 10_000;
    public static final int ENERGY_MAX_INSERT = 500;
    public static final int GENERATE_PER_TICK = 20;
    public static final int RUN_PER_TICK = 10;
    /** Minimum stored energy for the generator to count as active. */
    public static final int ACTIVE_THRESHOLD = RUN_PER_TICK;

    private final GeneratorEnergy energy = new GeneratorEnergy();
    /** Single fuel slot (right-click or hopper-fed). */
    private final SimpleContainer fuelSlot = new SimpleContainer(1);
    /** Remaining burn ticks of the current fuel item. */
    private int burnTime;

    public OxygenGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_GENERATOR.get(), pos, state);
    }

    /** Exposed to {@code RegisterCapabilitiesEvent} for {@code Capabilities.Energy.BLOCK}. */
    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    /** The fuel slot (for the block's interaction and future item automation). */
    public SimpleContainer getFuelSlot() {
        return this.fuelSlot;
    }

    /** Whether the generator is currently powering a breathable bubble. */
    public boolean isActive() {
        return this.energy.getAmountAsInt() >= ACTIVE_THRESHOLD;
    }

    /** Burn value (ticks of charging) for an accepted fuel item, or 0 if not a fuel. */
    public static int fuelValue(ItemStack stack) {
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            return 1_600;
        }
        if (stack.is(Items.COAL_BLOCK)) {
            return 16_000;
        }
        if (stack.is(Items.BLAZE_ROD)) {
            return 2_400;
        }
        if (stack.is(ModItems.ROCKET_FUEL_CANISTER.get())) {
            return 4_000;
        }
        return 0;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        if (this.burnTime > 0) {
            this.burnTime--;
            this.energy.generate(GENERATE_PER_TICK);
        } else {
            // Start the next fuel item if one is available.
            ItemStack fuel = this.fuelSlot.getItem(0);
            int value = fuelValue(fuel);
            if (value > 0 && this.energy.getAmountAsInt() < ENERGY_CAPACITY) {
                this.burnTime = value;
                fuel.shrink(1);
                setChanged();
            }
        }

        // Running cost: a little energy each tick keeps the bubble up.
        if (this.energy.getAmountAsInt() >= RUN_PER_TICK) {
            this.energy.consume(RUN_PER_TICK);
        }
    }

    // --- Persistence (Value I/O) -------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.serialize(output.child("Energy"));
        output.putInt("BurnTime", this.burnTime);
        output.store("Fuel", ItemStack.OPTIONAL_CODEC, this.fuelSlot.getItem(0));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.deserialize(input.childOrEmpty("Energy"));
        this.burnTime = input.getIntOr("BurnTime", 0);
        this.fuelSlot.setItem(0, input.read("Fuel", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    /** Internal energy buffer: receives power but does not allow extraction by others. */
    private final class GeneratorEnergy extends SimpleEnergyHandler {
        private GeneratorEnergy() {
            super(ENERGY_CAPACITY, ENERGY_MAX_INSERT, 0);
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            OxygenGeneratorBlockEntity.this.setChanged();
        }

        void generate(int amount) {
            int current = getAmountAsInt();
            int next = Math.min(getCapacityAsInt(), current + amount);
            if (next != current) {
                set(next);
            }
        }

        void consume(int amount) {
            set(Math.max(0, getAmountAsInt() - amount));
        }
    }
}
