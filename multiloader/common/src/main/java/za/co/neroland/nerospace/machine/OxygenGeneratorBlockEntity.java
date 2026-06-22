package za.co.neroland.nerospace.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.GasTank;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.menu.OxygenGeneratorMenu;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.world.OxygenFieldManager;

/**
 * Oxygen Generator — a grid-powered electrolyser: each tick it spends energy from its internal buffer
 * to synthesise {@link GasResource#OXYGEN} into its gas tank. Exposes the energy capability (insert
 * only, fed by the pipe network) and the gas capability (extract only, tapped by the pipe network /
 * adjacent gas tanks). It also feeds the world {@link OxygenFieldManager}: while its tank holds oxygen
 * this position is a field source (pressurising sealed rooms / a bubble) and the tank drains slowly.
 */
public class OxygenGeneratorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int ENERGY_CAPACITY = 50_000;
    public static final int GAS_CAPACITY = 8_000;
    public static final int MAX_INSERT = 1_000;
    public static final int MB_PER_TICK = 4;
    public static final int FE_PER_MB = 20;
    /** Oxygen drawn from the tank per tick to keep the breathable field alive (Config emit rate, inlined). */
    public static final int EMIT_MB_PER_TICK = 2;

    private final EnergyBuffer energy = new EnergyBuffer(ENERGY_CAPACITY, MAX_INSERT, 0, this::setChanged);
    private final GasTank gas = new GasTank(GAS_CAPACITY, this::setChanged);

    /** Synced gauge values for the screen: [0]=energy, [1]=energy cap, [2]=oxygen mB, [3]=oxygen cap. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getRaw();
                case 1 -> ENERGY_CAPACITY;
                case 2 -> gas.getRawAmount();
                case 3 -> GAS_CAPACITY;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // server-authoritative; the gauges are read-only
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public OxygenGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_GENERATOR.get(), pos, state);
    }

    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    /** Extract-only view of the gas tank — the pipe/network may pull oxygen out but not push it back. */
    public NerospaceGasStorage getGas() {
        return new NerospaceGasStorage() {
            @Override
            public GasResource getGas() {
                return gas.getGas();
            }

            @Override
            public long getAmount() {
                return gas.getAmount();
            }

            @Override
            public long getCapacity() {
                return gas.getCapacity();
            }

            @Override
            public long fill(GasResource g, long amount, boolean simulate) {
                return 0;
            }

            @Override
            public long drain(long amount, boolean simulate) {
                return gas.drain(amount, simulate);
            }
        };
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        // Electrolyse energy into oxygen gas while powered and there's room in the tank.
        long room = this.gas.getCapacity() - this.gas.getAmount();
        int produce = (int) Math.min(MB_PER_TICK, room);
        if (produce > 0) {
            int cost = produce * FE_PER_MB;
            if (this.energy.getAmount() >= cost) {
                this.energy.consume(cost);
                this.gas.fill(GasResource.OXYGEN, produce, false);
            }
        }
        // Feed the world oxygen field from the tank: while it holds oxygen this position is a field
        // source (the diffusion in OxygenFieldManager decides the breathable volume) and the tank drains
        // slowly; out of gas the source is dropped and the bubble collapses.
        if (level instanceof ServerLevel serverLevel) {
            OxygenFieldManager fieldManager = OxygenFieldManager.get(serverLevel);
            if (this.gas.getGas() == GasResource.OXYGEN && this.gas.getAmount() >= EMIT_MB_PER_TICK) {
                this.gas.drain(EMIT_MB_PER_TICK, false);
                fieldManager.addSource(pos);
            } else {
                fieldManager.removeSource(pos);
            }
        }
    }

    // --- MenuProvider ---------------------------------------------------------
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.oxygen_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OxygenGeneratorMenu(containerId, playerInventory, this.data,
                ContainerLevelAccess.create(this.level, this.worldPosition));
    }

    @Override
    public void setRemoved() {
        if (this.level instanceof ServerLevel serverLevel) {
            OxygenFieldManager.get(serverLevel).removeSource(this.worldPosition);
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", this.energy.getRaw());
        output.putString("Gas", this.gas.getRawGas().getSerializedName());
        output.putInt("GasAmount", this.gas.getRawAmount());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.setRaw(input.getIntOr("Energy", 0));
        this.gas.setRaw(GasResource.byName(input.getStringOr("Gas", "empty")), input.getIntOr("GasAmount", 0));
    }
}
