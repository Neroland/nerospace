package za.co.neroland.nerospace.machine;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.meteor.MeteorMaterials;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;
import za.co.neroland.nerolandcore.sideconfig.SideConfigured;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.menu.NerosiumGrinderMenu;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.rocket.StationRegistry;

/**
 * Nerosium Grinder — grid-powered processing machine. Input slot + a four-slot output buffer + an
 * energy buffer fed by pipes (insert-only); grinds inputs into dust over time. The output buffer
 * gives downstream extraction headroom, and the grinder <b>pauses when the buffer cannot hold the
 * full result</b> — it never drops items into the world. Exercises the item (in/out) and energy
 * seams, a ticker, and the menu/screen seam together.
 */
public class NerosiumGrinderBlockEntity extends BlockEntity
        implements WorldlyContainer, MenuProvider, SideConfigured {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_START = 1;
    public static final int OUTPUT_COUNT = 4;
    public static final int SIZE = OUTPUT_START + OUTPUT_COUNT; // 0 = input, 1..4 = output buffer
    public static final int CAPACITY = 20_000;
    public static final int MAX_INSERT = 500;
    public static final int ENERGY_PER_TICK = 20;
    public static final int MAX_PROGRESS = 200;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, MAX_INSERT, 0, this::setChanged);
    private int progress;

    /**
     * A completed meteor grind whose output is waiting for room in the buffer. The input is not
     * consumed until this is placed, so a blocked grinder holds — it never drops or destroys items.
     * Transient: rerolled after a reload if still blocked (harmless — nothing was consumed).
     */
    private List<ItemStack> pendingOutput = List.of();

    /**
     * Universal side configuration (Neroland Core): ITEM (input slot in / four-slot output buffer out)
     * + ENERGY (grid power in). PROCESSOR preset — material in on every face but the bottom, power in;
     * energy IO/PUSH forbidden (the grinder only consumes power). Composed, not inherited.
     */
    private final SideConfigComponent sideConfig =
            new SideConfigComponent(buildSideConfig(), this)
                    .withEnergy(this::getEnergy)
                    .withItems(() -> this);

    private static SideConfig buildSideConfig() {
        return SideConfig.builder()
                .channel(Channel.ITEM, SlotGroup.of("input", INPUT_SLOT),
                        SlotGroup.of("output", OUTPUT_START, OUTPUT_START + 1, OUTPUT_START + 2, OUTPUT_START + 3))
                .channel(Channel.ENERGY)
                .allow(Channel.ENERGY, za.co.neroland.nerolandcore.sideconfig.SideMode.OUTPUT, false)
                .allow(Channel.ENERGY, za.co.neroland.nerolandcore.sideconfig.SideMode.IO, false)
                .allow(Channel.ENERGY, za.co.neroland.nerolandcore.sideconfig.SideMode.PUSH, false)
                .defaultPreset(SidePreset.PROCESSOR)
                .build();
    }

    @Override
    public SideConfigComponent sideConfig() {
        return this.sideConfig;
    }

    /**
     * The last player to open this grinder's menu — the "operator" whose progression gates and current
     * planet drive the random meteor-block grind through Core's Meteor Material Registry. Transient gameplay
     * state (UUID only, never logged or persisted; POPIA/GDPR): it resets on world reload, after which the
     * grinder falls back to the in-station owner, then degrades to a no-op until a player opens it again.
     */
    @Nullable
    private UUID operator;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> NerospaceConfig.scaleInterval(MAX_PROGRESS, NerospaceConfig.machineSpeedMultiplier());
                case 2 -> energy.getRaw();
                case 3 -> CAPACITY;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public NerosiumGrinderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEROSIUM_GRINDER.get(), pos, state);
    }

    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        // Optional auto-eject / auto-input (default off per face — safe no-op until a player enables it).
        this.sideConfig.serverTick(level, pos, MachineSideConfig.TRANSFER_RATE);
        ItemStack input = this.items.get(INPUT_SLOT);
        boolean changed = input.is(ModItems.METEOR_ROCK_ITEM.get())
                ? tickMeteor(level, pos)
                : tickRecipe(input);
        if (changed) {
            this.setChanged();
        }
    }

    /** Fixed in-code recipes (ores/ingots → nerosium dust). @return whether internal state changed. */
    private boolean tickRecipe(ItemStack input) {
        ItemStack result = GrinderRecipes.getResult(input);
        int energyPerTick = NerospaceConfig.scale(ENERGY_PER_TICK, NerospaceConfig.fuelCostMultiplier());
        // Pause (don't advance, don't drop) unless the whole result fits the output buffer.
        boolean canWork = !result.isEmpty() && canAcceptAll(result) && this.energy.getAmount() >= energyPerTick;
        if (canWork) {
            this.progress++;
            this.energy.consume(energyPerTick);
            if (this.progress >= NerospaceConfig.scaleInterval(MAX_PROGRESS, NerospaceConfig.machineSpeedMultiplier())) {
                this.items.get(INPUT_SLOT).shrink(1);
                insertAll(List.of(result.copy()));
                this.progress = 0;
            }
            return true;
        } else if (this.progress != 0) {
            this.progress = 0;
            return true;
        }
        return false;
    }

    /**
     * The random meteor-block path: grinds a meteor rock into a Core-resolved primary dust (plus an
     * occasional exotic), weighted by the operator's progression gates and current planet via Neroland
     * Core's Meteor Material Registry. Distinct from {@link #tickRecipe} — the output is not known until the
     * grind completes, so it is resolved live (server-side).
     *
     * <p>When the buffer cannot hold the full resolved result, the grind <b>holds</b>: the result is kept
     * pending and the meteor rock is not consumed until there is room. Nothing is ever dropped into the
     * world or destroyed.
     *
     * @return whether internal state changed.
     */
    private boolean tickMeteor(Level level, BlockPos pos) {
        // A finished grind waiting for buffer space: place it as soon as it fits; consume input only then.
        if (!this.pendingOutput.isEmpty()) {
            ItemStack in = this.items.get(INPUT_SLOT);
            if (!in.is(ModItems.METEOR_ROCK_ITEM.get())) {
                // Input was removed while we waited — abandon the pending grind (nothing was consumed).
                this.pendingOutput = List.of();
                this.progress = 0;
                return true;
            }
            if (canAcceptAll(this.pendingOutput)) {
                in.shrink(1);
                insertAll(this.pendingOutput);
                this.pendingOutput = List.of();
                this.progress = 0;
                return true;
            }
            return false; // still blocked — hold, drop nothing
        }

        int energyPerTick = NerospaceConfig.scale(ENERGY_PER_TICK, NerospaceConfig.fuelCostMultiplier());
        ServerPlayer op = resolveOperator(level, pos);
        boolean canWork = op != null && hasAnyOutputRoom() && this.energy.getAmount() >= energyPerTick;
        if (canWork) {
            this.progress++;
            this.energy.consume(energyPerTick);
            if (this.progress >= NerospaceConfig.scaleInterval(MAX_PROGRESS, NerospaceConfig.machineSpeedMultiplier())) {
                List<Item> produced = MeteorMaterials.resolve(op, level.getRandom());
                if (produced.isEmpty()) {
                    this.progress = 0; // no eligible material — no-op, keep the meteor rock
                    return true;
                }
                List<ItemStack> stacks = produced.stream().map(ItemStack::new).toList();
                if (canAcceptAll(stacks)) {
                    this.items.get(INPUT_SLOT).shrink(1);
                    insertAll(stacks);
                    this.progress = 0;
                } else {
                    // Buffer can't take the full result yet — hold it pending; don't consume input.
                    this.pendingOutput = stacks;
                }
            }
            return true;
        } else if (this.progress != 0) {
            this.progress = 0;
            return true;
        }
        return false;
    }

    /**
     * Resolves the operator whose context drives the meteor grind: the last player to open this menu if
     * online, otherwise the owner of the enclosing station if online, otherwise {@code null}. Keyed by UUID
     * only — never logged (POPIA/GDPR).
     */
    @Nullable
    private ServerPlayer resolveOperator(Level level, BlockPos pos) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return null;
        }
        if (this.operator != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(this.operator);
            if (player != null) {
                return player;
            }
        }
        UUID owner = StationRegistry.get(server).ownerAt(level.dimension(), pos);
        return owner == null ? null : server.getPlayerList().getPlayer(owner);
    }

    // --- output buffer (slots OUTPUT_START .. OUTPUT_START+OUTPUT_COUNT-1) ----

    /** Whether any output slot can take at least one more item (so the grind may make progress). */
    private boolean hasAnyOutputRoom() {
        for (int i = 0; i < OUTPUT_COUNT; i++) {
            ItemStack slot = this.items.get(OUTPUT_START + i);
            if (slot.isEmpty() || slot.getCount() < slot.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /** Simulate placing every result across the buffer (merge into matching, then fill empties). */
    private boolean canAcceptAll(List<ItemStack> results) {
        // Working copies of the buffer's contents so we never mutate during the dry run.
        ItemStack[] slot = new ItemStack[OUTPUT_COUNT];
        for (int i = 0; i < OUTPUT_COUNT; i++) {
            slot[i] = this.items.get(OUTPUT_START + i).copy();
        }
        for (ItemStack result : results) {
            if (result.isEmpty()) {
                continue;
            }
            int remaining = result.getCount();
            for (int i = 0; i < OUTPUT_COUNT && remaining > 0; i++) {
                if (!slot[i].isEmpty() && ItemStack.isSameItemSameComponents(slot[i], result)) {
                    int space = slot[i].getMaxStackSize() - slot[i].getCount();
                    int put = Math.min(space, remaining);
                    slot[i].grow(put);
                    remaining -= put;
                }
            }
            for (int i = 0; i < OUTPUT_COUNT && remaining > 0; i++) {
                if (slot[i].isEmpty()) {
                    int put = Math.min(result.getMaxStackSize(), remaining);
                    slot[i] = result.copyWithCount(put);
                    remaining -= put;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean canAcceptAll(ItemStack result) {
        return canAcceptAll(List.of(result));
    }

    /** Place every result across the buffer. Caller must have checked {@link #canAcceptAll} first. */
    private void insertAll(List<ItemStack> results) {
        for (ItemStack result : results) {
            ItemStack remaining = result.copy();
            for (int i = 0; i < OUTPUT_COUNT && !remaining.isEmpty(); i++) {
                ItemStack slot = this.items.get(OUTPUT_START + i);
                if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, remaining)) {
                    int put = Math.min(slot.getMaxStackSize() - slot.getCount(), remaining.getCount());
                    if (put > 0) {
                        slot.grow(put);
                        remaining.shrink(put);
                    }
                }
            }
            for (int i = 0; i < OUTPUT_COUNT && !remaining.isEmpty(); i++) {
                if (this.items.get(OUTPUT_START + i).isEmpty()) {
                    int put = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                    this.items.set(OUTPUT_START + i, remaining.copyWithCount(put));
                    remaining.shrink(put);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Input", ItemStack.OPTIONAL_CODEC, this.items.get(INPUT_SLOT));
        for (int i = 0; i < OUTPUT_COUNT; i++) {
            output.store("Output" + i, ItemStack.OPTIONAL_CODEC, this.items.get(OUTPUT_START + i));
        }
        output.putInt("Progress", this.progress);
        output.putInt("Energy", this.energy.getRaw());
        this.sideConfig.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items.set(INPUT_SLOT, input.read("Input", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        for (int i = 0; i < OUTPUT_COUNT; i++) {
            java.util.Optional<ItemStack> read = input.read("Output" + i, ItemStack.OPTIONAL_CODEC);
            if (read.isEmpty() && i == 0) {
                // Back-compat: the pre-buffer grinder stored a single "Output"; fold it into the first slot.
                read = input.read("Output", ItemStack.OPTIONAL_CODEC);
            }
            this.items.set(OUTPUT_START + i, read.orElse(ItemStack.EMPTY));
        }
        this.progress = input.getIntOr("Progress", 0);
        this.energy.setRaw(input.getIntOr("Energy", 0));
        this.sideConfig.load(input);
    }

    // --- MenuProvider ---------------------------------------------------------
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.nerosium_grinder");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // Remember the operator so an in-progress meteor grind has gate/planet context (UUID only).
        this.operator = player.getUUID();
        return new NerosiumGrinderMenu(containerId, playerInventory, this, this.data);
    }

    /**
     * Whether {@code stack} is a valid grinder input: either a fixed in-code recipe input or a meteor rock
     * (the random Meteor Material Registry path). Shared by the menu slot and the hopper/pipe faces so all
     * three agree on what may enter the input slot.
     */
    public static boolean isGrindableInput(ItemStack stack) {
        return !GrinderRecipes.getResult(stack).isEmpty() || stack.is(ModItems.METEOR_ROCK_ITEM.get());
    }

    private static boolean isOutputSlot(int slot) {
        return slot >= OUTPUT_START && slot < OUTPUT_START + OUTPUT_COUNT;
    }

    // --- WorldlyContainer: per-face routing via the side config ---------------
    // The exposed slots and insert/extract permissions follow the side config (input slot accepts
    // grindable items on INPUT faces, the output buffer is taken from OUTPUT faces); the recipe/slot guard
    // is kept as an extra gate so a face can never push a non-grindable item — or anything — into outputs.
    @Override
    public int[] getSlotsForFace(Direction side) {
        return this.sideConfig.itemSlotsForFace(side);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != null && this.sideConfig.canInsertItem(slot, side)
                && slot == INPUT_SLOT && isGrindableInput(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isOutputSlot(slot) && this.sideConfig.canExtractItem(slot, side);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT_SLOT && isGrindableInput(stack);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack r = ContainerHelper.removeItem(this.items, slot, amount);
        if (!r.isEmpty()) {
            this.setChanged();
        }
        return r;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }
}
