package za.co.neroland.nerospace.machine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.world.TerraformChunkLoader;
import za.co.neroland.nerospace.world.TerraformManager;

/**
 * Terraformer machine (terraform design §2). On the same chassis as the Oxygen Generator — an internal
 * energy buffer fed by burning fuel — it slowly converts a dead planet into livable ground by advancing
 * an expanding circular frontier from its own column outward. Conversion is idempotent (re-running a
 * ring is a no-op), so only {@code radius + cursor} need persisting; the radius is uncapped and energy
 * is the real throttle (cost per block). Higher tiers convert more columns per cycle.
 *
 * <p>Each converted column turns its exposed surface into grass + dirt, flags the chunk permanently
 * breathable ({@code ModAttachments.TERRAFORMED} — §3.4) and, on Tier 3, may seed ore (§2.2/§T3).</p>
 */
public class TerraformerBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int UPGRADE_SLOT = 0;
    public static final int SIZE = 1;

    public static final int ENERGY_CAPACITY = 100_000;
    public static final int ENERGY_MAX_INSERT = 2_000;

    private final TerraformerEnergy energy = new TerraformerEnergy();
    /**
     * The authoritative upgrade slot ({@link MachineItemHandler}): the capability surface AND the
     * backing store of the Container/GUI/tick — never a parallel copy (see that class's javadoc).
     */
    @SuppressWarnings("this-escape") // setChanged callback, invoked only after construction
    private final MachineItemHandler upgradeHandler = new MachineItemHandler(SIZE, this::setChanged,
            (index, resource) -> {
                ItemStack stack = resource.toStack(1);
                return stack.is(ModItems.NEROSTEEL_INGOT.get()) || stack.is(ModItems.CINDRITE.get());
            });

    /** Machine tier (1..3): more columns per cycle, and Tier 3 unlocks ore seeding. */
    private int tier = 1;
    /** Expanding frontier (terraform design §2.1): horizontal radius + a within-ring column cursor. */
    private int radius;
    private int cursor;

    /** Transient cache of the current ring's column offsets (recomputed from {@code radius} on load). */
    private transient List<int[]> ring;
    private transient int ringFor = -1;
    /** Chunks this machine has force-loaded (only when active terraforming is enabled). */
    private final transient LongOpenHashSet forcedChunks = new LongOpenHashSet();

    /** Synced to the menu: [0]=energy [1]=capacity [2]=tier [3]=radius. */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getAmountAsInt();
                case 1 -> energy.getCapacityAsInt();
                case 2 -> tier;
                case 3 -> radius;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 2 -> tier = value;
                case 3 -> radius = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public TerraformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TERRAFORMER.get(), pos, state);
    }

    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    public ResourceHandler<ItemResource> getUpgradeHandler() {
        return this.upgradeHandler;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    public int getTier() {
        return this.tier;
    }

    public boolean isActive() {
        return this.energy.getAmountAsInt() >= Config.TERRAFORM_ENERGY_PER_BLOCK.get();
    }

    public int comparatorSignal() {
        int cap = this.energy.getCapacityAsInt();
        int stored = this.energy.getAmountAsInt();
        return (cap <= 0 || stored <= 0) ? 0 : 1 + (int) (stored / (double) cap * 14.0D);
    }

    /** Columns converted per work cycle, by tier (capped for TPS). */
    private int budgetPerCycle() {
        int base = switch (this.tier) {
            case 3 -> 6;
            case 2 -> 3;
            default -> 1;
        };
        return Math.min(base, Config.TERRAFORM_MAX_COLUMNS_PER_TICK.get());
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Grid-only power: the buffer is filled exclusively through the energy capability (pipes).

        // Auto-consume tier upgrades: a Nerosteel Ingot → T2, a Cindrite → T3.
        ItemStack upgrade = this.upgradeHandler.getStack(UPGRADE_SLOT);
        if (!upgrade.isEmpty()) {
            if (this.tier < 2 && upgrade.is(ModItems.NEROSTEEL_INGOT.get())) {
                upgrade.shrink(1);
                this.tier = 2;
                setChanged();
            } else if (this.tier < 3 && upgrade.is(ModItems.CINDRITE.get())) {
                upgrade.shrink(1);
                this.tier = 3;
                setChanged();
            }
        }

        if (serverLevel.getGameTime() % Config.TERRAFORM_WORK_INTERVAL_TICKS.get() == 0) {
            work(serverLevel, pos);
        }
    }

    private void work(ServerLevel level, BlockPos center) {
        int cost = Config.TERRAFORM_ENERGY_PER_BLOCK.get();
        int budget = budgetPerCycle();
        boolean changed = false;
        Set<LevelChunk> biomeChanged = new HashSet<>();
        BlockPos.MutableBlockPos col = new BlockPos.MutableBlockPos();

        for (int processed = 0; processed < budget; processed++) {
            if (this.energy.getAmountAsInt() < cost) {
                break;
            }
            List<int[]> r = ring();
            if (this.cursor >= r.size()) {
                this.radius++;          // grow outward — no cap (terraform design §2.1)
                this.cursor = 0;
                this.ringFor = -1;
                r = ring();
                changed = true;
            }
            int[] off = r.get(this.cursor++);
            int x = center.getX() + off[0];
            int z = center.getZ() + off[1];
            col.set(x, 0, z);
            if (!level.hasChunk(col.getX() >> 4, col.getZ() >> 4)) {
                // Lazy: leave unloaded columns for the chunk-load catch-up (TerraformManager). Optionally
                // force-load if the player opted in. No energy spent on a skipped column.
                maybeForceLoad(level, x, z);
                continue;
            }
            TerraformConversion.convertColumn(level, x, z, this.tier, biomeChanged);
            this.energy.consume(cost);
            changed = true;
        }

        // Publish our reach so the chunk-load handler can catch up unloaded columns later.
        TerraformManager.get(level).update(this.worldPosition, this.radius, this.tier);

        // Resync any chunks whose biome changed so clients recolour the terraformed ground.
        if (!biomeChanged.isEmpty()) {
            ClientboundChunksBiomesPacket packet =
                    ClientboundChunksBiomesPacket.forChunks(new ArrayList<>(biomeChanged));
            for (ServerPlayer player : level.players()) {
                player.connection.send(packet);
            }
        }

        if (changed) {
            setChanged();
        }
        if (Config.OXYGEN_DEBUG_LOG.get() && level.getGameTime() % 100 == 0) {
            Nerospace.LOGGER.info("[terraform] dim={} center={} tier={} radius={}",
                    level.dimension(), center, this.tier, this.radius);
        }
    }

    private List<int[]> ring() {
        if (this.ring != null && this.ringFor == this.radius) {
            return this.ring;
        }
        List<int[]> out = new ArrayList<>();
        int r = this.radius;
        if (r == 0) {
            out.add(new int[] {0, 0});
        } else {
            long inner = (long) r * r;
            long outer = (long) (r + 1) * (r + 1);
            for (int dx = -r - 1; dx <= r + 1; dx++) {
                for (int dz = -r - 1; dz <= r + 1; dz++) {
                    long d = (long) dx * dx + (long) dz * dz;
                    if (d >= inner && d < outer) {
                        out.add(new int[] {dx, dz});
                    }
                }
            }
        }
        this.ring = out;
        this.ringFor = r;
        return out;
    }

    /** Opt-in active terraforming: force-load an unloaded frontier chunk, bounded by config (§2.3). */
    private void maybeForceLoad(ServerLevel level, int x, int z) {
        if (!Config.TERRAFORM_FORCE_LOAD_CHUNKS.get()
                || this.forcedChunks.size() >= Config.TERRAFORM_MAX_FORCED_CHUNKS.get()) {
            return;
        }
        int cx = x >> 4;
        int cz = z >> 4;
        long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
        if (this.forcedChunks.add(key)) {
            TerraformChunkLoader.CONTROLLER.forceChunk(level, this.worldPosition, cx, cz, true, false);
        }
    }

    private void releaseForcedChunks() {
        if (!(this.level instanceof ServerLevel serverLevel) || this.forcedChunks.isEmpty()) {
            return;
        }
        LongIterator it = this.forcedChunks.iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            int cx = (int) (key >> 32);
            int cz = (int) key;
            TerraformChunkLoader.CONTROLLER.forceChunk(serverLevel, this.worldPosition, cx, cz, false, false);
        }
        this.forcedChunks.clear();
    }

    @Override
    public void setRemoved() {
        releaseForcedChunks();
        if (this.level instanceof ServerLevel serverLevel) {
            TerraformManager.get(serverLevel).remove(this.worldPosition);
        }
        super.setRemoved();
    }

    // --- Persistence --------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.serialize(output.child("Energy"));
        output.putInt("Tier", this.tier);
        output.putInt("Radius", this.radius);
        output.putInt("Cursor", this.cursor);
        output.store("Upgrade", ItemStack.OPTIONAL_CODEC, this.upgradeHandler.getStack(UPGRADE_SLOT));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.deserialize(input.childOrEmpty("Energy"));
        this.tier = Math.max(1, input.getIntOr("Tier", 1));
        this.radius = input.getIntOr("Radius", 0);
        this.cursor = input.getIntOr("Cursor", 0);
        this.ringFor = -1;
        this.upgradeHandler.setStack(UPGRADE_SLOT, input.read("Upgrade", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    // --- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.terraformer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TerraformerMenu(containerId, playerInventory, this, this.dataAccess);
    }

    // --- Container ----------------------------------------------------------

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.upgradeHandler.isStoreEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.upgradeHandler.getStack(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return this.upgradeHandler.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.upgradeHandler.takeStack(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stack.limitSize(Math.min(this.getMaxStackSize(), stack.getMaxStackSize()));
        this.upgradeHandler.setStack(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(ModItems.NEROSTEEL_INGOT.get()) || stack.is(ModItems.CINDRITE.get());
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        this.upgradeHandler.clearStore();
    }

    private final class TerraformerEnergy extends SimpleEnergyHandler {
        private TerraformerEnergy() {
            super(ENERGY_CAPACITY, ENERGY_MAX_INSERT, 0);
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            TerraformerBlockEntity.this.setChanged();
        }

        void consume(int amount) {
            set(Math.max(0, getAmountAsInt() - amount));
        }
    }
}
