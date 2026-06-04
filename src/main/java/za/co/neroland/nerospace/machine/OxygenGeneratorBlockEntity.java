package za.co.neroland.nerospace.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.resource.ResourceStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.GasStacksResourceHandler;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.world.OxygenFieldManager;

/**
 * Oxygen Generator (gas-layer integration): an electrolysis machine. It takes <b>grid power</b>
 * (energy capability, insert-only — feed it with Universal Pipes from a generator/battery) and turns
 * it into <b>Oxygen gas</b> stored in an internal tank (exposed via the gas capability, so pipes can
 * bottle it into Gas Tanks or carry it to other rooms). The breathable field around the machine is
 * fed FROM the tank: while it holds oxygen the {@code OxygenFieldManager} treats this position as a
 * source and the tank drains slowly; out of gas (or power) the bubble collapses.
 */
public class OxygenGeneratorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int ENERGY_CAPACITY = 10_000;
    public static final int ENERGY_MAX_INSERT = 500;

    /** Internal oxygen tank size (mB). */
    public static final int O2_CAPACITY = 8_000;
    /** Oxygen produced per tick at full power (mB). */
    public static final int MAKE_MB_PER_TICK = 5;
    /** Energy cost per mB of oxygen produced. */
    public static final int FE_PER_MB = 2;
    /** Oxygen consumed per tick to keep the local breathable field alive (mB). */
    public static final int EMIT_MB_PER_TICK = 2;

    private final GeneratorEnergy energy = new GeneratorEnergy();
    private final O2Tank tank = new O2Tank();

    /** Synced to the open menu: [0]=energy, [1]=energy cap, [2]=oxygen, [3]=oxygen cap. */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getAmountAsInt();
                case 1 -> energy.getCapacityAsInt();
                case 2 -> tank.getAmountAsInt(0);
                case 3 -> O2_CAPACITY;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public OxygenGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_GENERATOR.get(), pos, state);
    }

    /** Exposed via {@code Capabilities.Energy.BLOCK} (insert-only — grid powered). */
    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    /** Exposed via the mod's gas capability so pipes can carry the oxygen away. */
    public ResourceHandler<GasResource> getGasHandler() {
        return this.tank;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    /** Whether the generator is currently feeding a breathable bubble. */
    public boolean isActive() {
        return this.tank.getAmountAsInt(0) >= EMIT_MB_PER_TICK;
    }

    /** Comparator output: 0 (empty tank) .. 15 (full oxygen tank). */
    public int comparatorSignal() {
        int stored = this.tank.getAmountAsInt(0);
        return stored <= 0 ? 0 : 1 + (int) (stored / (double) O2_CAPACITY * 14.0D);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        // Electrolysis: grid power -> oxygen, throttled by available energy and tank room.
        int room = O2_CAPACITY - this.tank.getAmountAsInt(0);
        int make = Math.min(MAKE_MB_PER_TICK, Math.min(room, this.energy.getAmountAsInt() / FE_PER_MB));
        if (make > 0) {
            this.energy.consume(make * FE_PER_MB);
            this.tank.fill(make);
        }

        // The breathable field drinks from the tank.
        if (isActive()) {
            this.tank.drain(EMIT_MB_PER_TICK);
        }

        // Feed the oxygen field: register/forget this position as a source by active state. The field
        // diffusion (not a raw radius bubble) decides the breathable volume — see OxygenFieldManager.
        if (level instanceof ServerLevel serverLevel) {
            OxygenFieldManager manager = OxygenFieldManager.get(serverLevel);
            if (isActive()) {
                manager.addSource(pos);
            } else {
                manager.removeSource(pos);
            }
        }
    }

    @Override
    public void setRemoved() {
        if (this.level instanceof ServerLevel serverLevel) {
            OxygenFieldManager.get(serverLevel).removeSource(this.worldPosition);
        }
        super.setRemoved();
    }

    // --- Persistence (Value I/O) -------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.serialize(output.child("Energy"));
        this.tank.serialize(output.child("Oxygen"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.deserialize(input.childOrEmpty("Energy"));
        this.tank.deserialize(input.childOrEmpty("Oxygen"));
    }

    // --- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.oxygen_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OxygenGeneratorMenu(containerId, playerInventory, this.dataAccess,
                net.minecraft.world.inventory.ContainerLevelAccess.create(this.level, this.worldPosition));
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

        void consume(int amount) {
            set(Math.max(0, getAmountAsInt() - amount));
        }
    }

    /** The internal oxygen tank: produced here, drained by the field and by pipes. */
    private final class O2Tank extends GasStacksResourceHandler {
        private O2Tank() {
            super(1, O2_CAPACITY);
        }

        @Override
        public boolean isValid(int index, GasResource resource) {
            return resource.isEmpty() || resource == GasResource.OXYGEN;
        }

        @Override
        protected void onContentsChanged(int index, ResourceStack<GasResource> oldStack) {
            OxygenGeneratorBlockEntity.this.setChanged();
        }

        void fill(int amount) {
            int next = Math.min(O2_CAPACITY, getAmountAsInt(0) + amount);
            set(0, GasResource.OXYGEN, next);
        }

        void drain(int amount) {
            int next = Math.max(0, getAmountAsInt(0) - amount);
            set(0, next == 0 ? GasResource.EMPTY : GasResource.OXYGEN, next);
        }
    }
}
