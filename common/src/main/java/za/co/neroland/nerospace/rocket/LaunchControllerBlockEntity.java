package za.co.neroland.nerospace.rocket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.fluid.FluidTank;
import za.co.neroland.nerospace.fluid.ModFluids;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.GasTank;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.menu.LaunchControllerMenu;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModBlocks;

/**
 * Brains of the {@link LaunchControllerBlock}: holds the three building-material slots (Launch Pads,
 * Station Wall, Launch Gantry), a target tier, and the logic to lay out the real pad formation in
 * front of the controller. Building is additive — picking a higher tier only places the blocks the
 * formation is still missing (displaced pad-system blocks are dropped back to the world, so a
 * Tier-3→4 upgrade returns the Station Wall ring it replaces).
 */
public class LaunchControllerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_PAD = 0;
    public static final int SLOT_WALL = 1;
    public static final int SLOT_GANTRY = 2;
    private static final int SLOTS = 3;
    /** How far ahead of the controller the pad centre sits (clears the wall ring off the controller). */
    private static final int PAD_OFFSET = 3;

    /** Tank capacities + per-tick transfer rates into the docked rocket. */
    private static final int FUEL_CAP = 16_000;
    private static final int OXY_CAP = 8_000;
    private static final int POWER_CAP = 20_000;
    private static final int FUEL_RATE = 80;
    private static final int OXY_RATE = 40;
    private static final int POWER_RATE = 200;

    private int targetTier = 1;
    private boolean hologram;
    private final SimpleContainer inputs = new SimpleContainer(SLOTS);

    /** Resource hub: fuel + oxygen + power, fed by pipes/cables and pumped into the rocket on the pad. */
    private final FluidTank fuelTank = new FluidTank(FUEL_CAP, this::setChanged);
    private final GasTank oxygenTank = new GasTank(OXY_CAP, this::setChanged);
    private final EnergyBuffer energy = new EnergyBuffer(POWER_CAP, 1_000, 1_000, this::setChanged);

    private long cacheTick = -1L;
    private final int[] needed = new int[3]; // [pad, wall, gantry]
    private boolean canBuild;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            refresh();
            return switch (index) {
                case 0 -> targetTier;
                case 1 -> needed[0];
                case 2 -> needed[1];
                case 3 -> needed[2];
                case 4 -> canBuild ? 1 : 0;
                case 5 -> hologram ? 1 : 0;
                case 6 -> (int) fuelTank.getAmount();
                case 7 -> FUEL_CAP;
                case 8 -> (int) oxygenTank.getAmount();
                case 9 -> OXY_CAP;
                case 10 -> energy.getRaw();
                case 11 -> POWER_CAP;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Read-only from the client.
        }

        @Override
        public int getCount() {
            return 12;
        }
    };

    public LaunchControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH_CONTROLLER.get(), pos, state);
    }

    public Container getInputs() {
        return this.inputs;
    }

    public ContainerData getData() {
        return this.data;
    }

    public int getTargetTier() {
        return this.targetTier;
    }

    /** Set the target pad tier (1–4); server-side, from the menu. */
    public void setTargetTier(int tier) {
        this.targetTier = Math.max(1, Math.min(4, tier));
        this.cacheTick = -1L;
        markChangedAndSync();
    }

    public boolean isHologram() {
        return this.hologram;
    }

    /** Toggle the holographic pad preview (server-side, synced to the client renderer). */
    public void toggleHologram() {
        this.hologram = !this.hologram;
        markChangedAndSync();
    }

    private void markChangedAndSync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Whether an item belongs in {@code slot} (pad / wall / gantry). */
    public boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_PAD -> stack.is(ModBlocks.ROCKET_LAUNCH_PAD.get().asItem());
            case SLOT_WALL -> stack.is(ModBlocks.STATION_WALL.get().asItem());
            case SLOT_GANTRY -> stack.is(ModBlocks.LAUNCH_GANTRY.get().asItem());
            default -> false;
        };
    }

    // --- Pad layout ----------------------------------------------------------

    /** The block at depth {@code d} (along the facing) and width {@code w} (clockwise), at the pad's Y. */
    private BlockPos cell(Direction facing, Direction right, int d, int w) {
        return getBlockPos().relative(facing, d).relative(right, w);
    }

    /** The target formation for {@code tier}: every position that should hold a given pad block. */
    private Map<BlockPos, Block> targets(int tier) {
        Map<BlockPos, Block> map = new LinkedHashMap<>();
        Direction facing = getBlockState().getValue(LaunchControllerBlock.FACING);
        Direction right = facing.getClockWise();
        Block pad = ModBlocks.ROCKET_LAUNCH_PAD.get();
        int n = tier <= 1 ? 1 : (tier <= 3 ? 3 : 5);
        int r = (n - 1) / 2;
        for (int dd = -r; dd <= r; dd++) {
            for (int ww = -r; ww <= r; ww++) {
                map.put(cell(facing, right, PAD_OFFSET + dd, ww), pad);
            }
        }
        if (tier == 3) { // Station Wall ring around the 3x3
            Block wall = ModBlocks.STATION_WALL.get();
            for (int dd = -2; dd <= 2; dd++) {
                for (int ww = -2; ww <= 2; ww++) {
                    if (Math.abs(dd) == 2 || Math.abs(ww) == 2) {
                        map.put(cell(facing, right, PAD_OFFSET + dd, ww), wall);
                    }
                }
            }
        }
        if (tier >= 4) { // Launch Gantry on the 5x5 border ring
            map.put(cell(facing, right, PAD_OFFSET + 3, 0), ModBlocks.LAUNCH_GANTRY.get());
        }
        return map;
    }

    /**
     * Ghost cells for the holographic preview: each still-missing target position, as an offset from the
     * controller ({@code [dx, dy, dz, argb]}) coloured by block type. Empty when the hologram is off.
     * Client-safe (reads the synced block states around the controller).
     */
    public List<int[]> previewCells() {
        if (!this.hologram || this.level == null) {
            return List.of();
        }
        List<int[]> cells = new ArrayList<>();
        BlockPos base = getBlockPos();
        for (Map.Entry<BlockPos, Block> entry : targets(this.targetTier).entrySet()) {
            BlockState current = this.level.getBlockState(entry.getKey());
            if (current.is(entry.getValue())) {
                continue; // already placed — no ghost needed
            }
            Block block = entry.getValue();
            int color = block == ModBlocks.STATION_WALL.get() ? 0xFF3CC8E6
                    : (block == ModBlocks.LAUNCH_GANTRY.get() ? 0xFFE0506A : 0xFF54D46A);
            BlockPos rel = entry.getKey().subtract(base);
            cells.add(new int[] {rel.getX(), rel.getY(), rel.getZ(), color});
        }
        return cells;
    }

    private int slotFor(Block block) {
        if (block == ModBlocks.STATION_WALL.get()) {
            return SLOT_WALL;
        }
        if (block == ModBlocks.LAUNCH_GANTRY.get()) {
            return SLOT_GANTRY;
        }
        return SLOT_PAD;
    }

    private boolean isPadPart(BlockState state) {
        return state.is(ModBlocks.ROCKET_LAUNCH_PAD.get())
                || state.is(ModBlocks.STATION_WALL.get())
                || state.is(ModBlocks.LAUNCH_GANTRY.get());
    }

    private boolean canPlaceAt(BlockState current) {
        return current.isAir() || current.canBeReplaced() || isPadPart(current);
    }

    /** Recompute the per-type "needed" counts + buildability (memoised per game tick, server-side). */
    private void refresh() {
        if (this.level == null || this.level.isClientSide() || this.level.getGameTime() == this.cacheTick) {
            return;
        }
        this.cacheTick = this.level.getGameTime();
        this.needed[0] = 0;
        this.needed[1] = 0;
        this.needed[2] = 0;
        for (Map.Entry<BlockPos, Block> entry : targets(this.targetTier).entrySet()) {
            BlockState current = this.level.getBlockState(entry.getKey());
            if (!current.is(entry.getValue()) && canPlaceAt(current)) {
                this.needed[slotFor(entry.getValue())]++;
            }
        }
        this.canBuild = this.needed[0] <= this.inputs.getItem(SLOT_PAD).getCount()
                && this.needed[1] <= this.inputs.getItem(SLOT_WALL).getCount()
                && this.needed[2] <= this.inputs.getItem(SLOT_GANTRY).getCount()
                && (this.needed[0] + this.needed[1] + this.needed[2]) > 0;
    }

    /** Places every still-missing block of the target formation, consuming from the slots. Server-side. */
    public void build(Player player) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        int placed = 0;
        for (Map.Entry<BlockPos, Block> entry : targets(this.targetTier).entrySet()) {
            BlockPos pos = entry.getKey();
            Block block = entry.getValue();
            BlockState current = this.level.getBlockState(pos);
            if (current.is(block) || !canPlaceAt(current)) {
                continue;
            }
            int slot = slotFor(block);
            if (this.inputs.getItem(slot).isEmpty()) {
                continue;
            }
            if (isPadPart(current) && !current.isAir()) {
                // Return the displaced pad-system block instead of destroying it.
                Containers.dropItemStack(this.level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        new ItemStack(current.getBlock().asItem()));
            }
            this.level.setBlockAndUpdate(pos, block.defaultBlockState());
            this.inputs.removeItem(slot, 1);
            placed++;
        }
        this.cacheTick = -1L;
        markChangedAndSync();
        if (placed > 0) {
            player.sendSystemMessage(Component.translatable("block.nerospace.launch_controller.built", placed, this.targetTier));
        } else {
            player.sendSystemMessage(Component.translatable("block.nerospace.launch_controller.nothing"));
        }
    }

    // --- Resource hub (fed by pipes/cables; pumped into the docked rocket) ---

    public NerospaceFluidStorage getTank() {
        return this.fuelTank;
    }

    public NerospaceGasStorage getGas() {
        return this.oxygenTank;
    }

    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    /** Server tick: top up the rocket docked on the pad in front with fuel, oxygen and power. */
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        Direction facing = state.getValue(LaunchControllerBlock.FACING);
        RocketEntity rocket = LaunchPadMultiblock.dockedRocket(level, pos.relative(facing, PAD_OFFSET));
        if (rocket == null) {
            return;
        }
        int fuel = (int) this.fuelTank.drain(FUEL_RATE, false);
        if (fuel > 0) {
            int overflow = rocket.addFuel(fuel);
            if (overflow > 0) {
                this.fuelTank.fill(ModFluids.ROCKET_FUEL.get(), overflow, false);
            }
        }
        int oxy = (int) this.oxygenTank.drain(OXY_RATE, false);
        if (oxy > 0) {
            int overflow = rocket.addOxygen(oxy);
            if (overflow > 0) {
                this.oxygenTank.fill(GasResource.OXYGEN, overflow, false);
            }
        }
        int power = (int) this.energy.extract(POWER_RATE, true);
        if (power > 0) {
            int used = power - rocket.addPower(power);
            if (used > 0) {
                this.energy.extract(used, false);
            }
        }
    }

    // --- MenuProvider --------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nerospace.launch_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LaunchControllerMenu(containerId, playerInventory, this.inputs, this.data, this);
    }

    // --- Persistence ---------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("TargetTier", this.targetTier);
        output.putBoolean("Hologram", this.hologram);
        output.store("Pad", ItemStack.OPTIONAL_CODEC, this.inputs.getItem(SLOT_PAD));
        output.store("Wall", ItemStack.OPTIONAL_CODEC, this.inputs.getItem(SLOT_WALL));
        output.store("Gantry", ItemStack.OPTIONAL_CODEC, this.inputs.getItem(SLOT_GANTRY));
        output.putString("FuelFluid", BuiltInRegistries.FLUID.getKey(this.fuelTank.getRawFluid()).toString());
        output.putInt("FuelAmount", this.fuelTank.getRawAmount());
        output.putString("Gas", this.oxygenTank.getRawGas().getSerializedName());
        output.putInt("GasAmount", this.oxygenTank.getRawAmount());
        output.putInt("Power", this.energy.getRaw());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.targetTier = Math.max(1, Math.min(4, input.getIntOr("TargetTier", 1)));
        this.hologram = input.getBooleanOr("Hologram", false);
        this.inputs.setItem(SLOT_PAD, input.read("Pad", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.inputs.setItem(SLOT_WALL, input.read("Wall", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.inputs.setItem(SLOT_GANTRY, input.read("Gantry", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(input.getStringOr("FuelFluid", "minecraft:empty")));
        this.fuelTank.setRaw(fluid, input.getIntOr("FuelAmount", 0));
        this.oxygenTank.setRaw(GasResource.byName(input.getStringOr("Gas", "empty")), input.getIntOr("GasAmount", 0));
        this.energy.setRaw(input.getIntOr("Power", 0));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
