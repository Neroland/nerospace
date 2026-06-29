package za.co.neroland.nerospace.machine;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
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

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.menu.NerosiumGrinderMenu;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.rocket.StationRegistry;

/**
 * Nerosium Grinder — grid-powered processing machine. Input slot + output slot + an energy buffer
 * fed by pipes (insert-only); grinds inputs into dust over time. Exercises the item (in/out) and
 * energy seams, a ticker, and the menu/screen seam together.
 */
public class NerosiumGrinderBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SIZE = 2;
    public static final int CAPACITY = 20_000;
    public static final int MAX_INSERT = 500;
    public static final int ENERGY_PER_TICK = 20;
    public static final int MAX_PROGRESS = 200;
    private static final int[] SLOTS = {INPUT_SLOT, OUTPUT_SLOT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, MAX_INSERT, 0, this::setChanged);
    private int progress;

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
        boolean canWork = !result.isEmpty() && canInsertOutput(result) && this.energy.getAmount() >= energyPerTick;
        if (canWork) {
            this.progress++;
            this.energy.consume(energyPerTick);
            if (this.progress >= NerospaceConfig.scaleInterval(MAX_PROGRESS, NerospaceConfig.machineSpeedMultiplier())) {
                craft(result);
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
     * grind completes, so it is resolved live (server-side) in {@link #grindMeteor}.
     *
     * <p>Needs a {@link ServerPlayer} for gate/planet context: we use the grinder's operator (the last
     * player to open its menu), falling back to the owner of the station the grinder sits in. With no
     * resolvable player — or no eligible material — the grind degrades gracefully to a no-op (energy and
     * the meteor rock are preserved), so an unattended grinder never silently destroys input.
     *
     * @return whether internal state changed.
     */
    private boolean tickMeteor(Level level, BlockPos pos) {
        int energyPerTick = NerospaceConfig.scale(ENERGY_PER_TICK, NerospaceConfig.fuelCostMultiplier());
        ServerPlayer op = resolveOperator(level, pos);
        boolean canWork = op != null && hasOutputRoom() && this.energy.getAmount() >= energyPerTick;
        if (canWork) {
            this.progress++;
            this.energy.consume(energyPerTick);
            if (this.progress >= NerospaceConfig.scaleInterval(MAX_PROGRESS, NerospaceConfig.machineSpeedMultiplier())) {
                grindMeteor(level, pos, op);
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

    /** Whether the output slot can take at least one more item (empty, or below max stack). */
    private boolean hasOutputRoom() {
        ItemStack output = this.items.get(OUTPUT_SLOT);
        return output.isEmpty() || output.getCount() < output.getMaxStackSize();
    }

    /**
     * Completes one meteor grind: consumes a meteor rock and yields Core's resolved item(s). The first
     * item merges into the output slot when compatible; anything that does not fit (a mismatched item, a
     * full slot, or the bonus exotic) pops out above the grinder rather than being lost. An empty
     * resolution (no eligible material) is a no-op — the meteor rock is left intact.
     */
    private void grindMeteor(Level level, BlockPos pos, ServerPlayer operator) {
        List<Item> produced = MeteorMaterials.resolve(operator, level.getRandom());
        if (produced.isEmpty()) {
            return;
        }
        this.items.get(INPUT_SLOT).shrink(1);
        for (Item item : produced) {
            insertOrPop(level, pos, new ItemStack(item));
        }
    }

    /** Merge {@code stack} into the output slot if it fits, else drop it above the grinder. */
    private void insertOrPop(Level level, BlockPos pos, ItemStack stack) {
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_SLOT, stack);
            return;
        }
        if (ItemStack.isSameItemSameComponents(output, stack)
                && output.getCount() + stack.getCount() <= output.getMaxStackSize()) {
            output.grow(stack.getCount());
            return;
        }
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
    }

    private void craft(ItemStack result) {
        this.items.get(INPUT_SLOT).shrink(1);
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_SLOT, result.copy());
        } else {
            output.grow(result.getCount());
        }
    }

    private boolean canInsertOutput(ItemStack result) {
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Input", ItemStack.OPTIONAL_CODEC, this.items.get(INPUT_SLOT));
        output.store("Output", ItemStack.OPTIONAL_CODEC, this.items.get(OUTPUT_SLOT));
        output.putInt("Progress", this.progress);
        output.putInt("Energy", this.energy.getRaw());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items.set(INPUT_SLOT, input.read("Input", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.items.set(OUTPUT_SLOT, input.read("Output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.progress = input.getIntOr("Progress", 0);
        this.energy.setRaw(input.getIntOr("Energy", 0));
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

    // --- WorldlyContainer: input in (grindable), output out -------------------
    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT_SLOT && isGrindableInput(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT;
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
