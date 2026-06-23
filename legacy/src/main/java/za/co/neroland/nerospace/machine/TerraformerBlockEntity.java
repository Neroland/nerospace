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
import za.co.neroland.nerospace.Tuning;
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

    /** Number of synced menu data slots (see {@link #dataAccess}). */
    public static final int DATA_COUNT = 9;

    /** Machine tier (1..3): more columns per cycle, and Tier 3 unlocks ore seeding. */
    private int tier = 1;
    /** Expanding frontier (terraform design §2.1): horizontal radius + a within-ring column cursor. */
    private int radius;
    private int cursor;
    /**
     * Trailing stage frontiers (DEEPER_TERRAFORM_DESIGN.md §2.1), each the same ring/cursor mechanism.
     * Invariant: {@code lifeRadius <= hydrationRadius <= radius}. All default 0 on legacy saves, so an
     * old machine simply starts sweeping the deeper stages over its existing stage-1 ground.
     */
    private int hydrationRadius;
    private int hydrationCursor;
    private int lifeRadius;
    private int lifeCursor;
    /** Buffered hydration units (melted glacite, fed by an adjacent Hydration Module — §3.1). */
    private int hydration;
    /** Transient: stage 2 wants water but the hydration buffer ran dry (GUI/Monitor stall reason). */
    private transient boolean hydrationStalled;

    /** Transient per-stage caches of the current ring's column offsets (recomputed on load). */
    private final transient List<int[]>[] rings = newRingCache();
    private final transient int[] ringsFor = {-1, -1, -1};
    /** Chunks this machine has force-loaded (only when active terraforming is enabled). */
    private final transient LongOpenHashSet forcedChunks = new LongOpenHashSet();

    @SuppressWarnings("unchecked")
    private static List<int[]>[] newRingCache() {
        return (List<int[]>[]) new List<?>[3];
    }

    /**
     * Synced to the menu: [0]=energy [1]=capacity [2]=tier [3]=radius [4]=hydration
     * [5]=hydrationCap [6]=hydrationRadius [7]=lifeRadius [8]=hydrationStalled.
     */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getAmountAsInt();
                case 1 -> energy.getCapacityAsInt();
                case 2 -> tier;
                case 3 -> radius;
                case 4 -> hydration;
                case 5 -> Tuning.TERRAFORM_HYDRATION_CAP;
                case 6 -> hydrationRadius;
                case 7 -> lifeRadius;
                case 8 -> hydrationStalled ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 2 -> tier = value;
                case 3 -> radius = value;
                case 4 -> hydration = value;
                case 6 -> hydrationRadius = value;
                case 7 -> lifeRadius = value;
                case 8 -> hydrationStalled = value != 0;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
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
        return this.energy.getAmountAsInt() >= Tuning.terraformEnergyPerBlock();
    }

    public int comparatorSignal() {
        int cap = this.energy.getCapacityAsInt();
        int stored = this.energy.getAmountAsInt();
        return (cap <= 0 || stored <= 0) ? 0 : 1 + (int) (stored / (double) cap * 14.0D);
    }

    /** Stage-1 columns converted per work cycle, by tier (capped for TPS). */
    private int budgetPerCycle() {
        int base = switch (this.tier) {
            case 3 -> 6;
            case 2 -> 3;
            default -> 1;
        };
        return Math.min(base, Config.TERRAFORM_MAX_COLUMNS_PER_TICK.get());
    }

    /**
     * Per-stage column budget for one work cycle (DEEPER_TERRAFORM_DESIGN.md §2.1): stage 1 keeps the
     * full tier budget (breathable ground never waits), the trailing stages get a guaranteed-but-
     * smaller share so they always crawl forward yet never overtake. The summed work stays well under
     * the global {@code terraformMaxColumnsPerTick} cap.
     */
    private int stageBudget(int stage) {
        int base = budgetPerCycle();
        return switch (stage) {
            case 2 -> Math.max(1, base / 2);
            case 3 -> Math.max(1, base / 4);
            default -> base;
        };
    }

    /** Per-column energy cost of a stage (stage 2 ×2, stage 3 ×4 — §2.1). */
    private static int stageCost(int stage) {
        return switch (stage) {
            case 2 -> Tuning.terraformStage2EnergyPerBlock();
            case 3 -> Tuning.terraformStage3EnergyPerBlock();
            default -> Tuning.terraformEnergyPerBlock();
        };
    }

    /**
     * The stage-2 water table (§3.2): one below the machine's own base, derived (not persisted) so it
     * is stable across saves. Basins below it fill flush with the ground the machine stands on.
     */
    public int waterTableY() {
        return this.worldPosition.getY() - 1;
    }

    /** Buffered hydration units (melted glacite). */
    public int getHydration() {
        return this.hydration;
    }

    /**
     * Accepts melted glacite from an adjacent Hydration Module (§3.1).
     *
     * @return units actually accepted (0 when the buffer is full)
     */
    public int acceptHydration(int units) {
        int accepted = Math.min(units, Tuning.TERRAFORM_HYDRATION_CAP - this.hydration);
        if (accepted > 0) {
            this.hydration += accepted;
            setChanged();
        }
        return accepted;
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

        // Redstone switch (gallery/UX): wired machines only sweep while powered (MachineRedstone).
        if (serverLevel.getGameTime() % Tuning.terraformWorkIntervalTicks() == 0
                && MachineRedstone.allowsRun(level, pos)) {
            work(serverLevel, pos);
        }
    }

    private void work(ServerLevel level, BlockPos center) {
        boolean changed = false;
        Set<LevelChunk> biomeChanged = new HashSet<>();

        // Outermost first (DEEPER_TERRAFORM_DESIGN.md §2.1): stage 1 keeps priority so breathable
        // ground never waits on water; the trailing frontiers then take their smaller shares.
        this.hydrationStalled = false;
        changed |= workStage(level, center, 1, biomeChanged);
        changed |= workStage(level, center, 2, biomeChanged);
        changed |= workStage(level, center, 3, biomeChanged);

        // Publish our reach so the chunk-load handler can catch up unloaded columns later.
        TerraformManager.get(level).update(this.worldPosition,
                this.radius, this.hydrationRadius, this.lifeRadius, this.tier);

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
            Nerospace.LOGGER.info("[terraform] dim={} center={} tier={} radii={}/{}/{} hydration={}",
                    level.dimension(), center, this.tier,
                    this.radius, this.hydrationRadius, this.lifeRadius, this.hydration);
        }
    }

    /**
     * Runs one stage's frontier for this cycle: up to {@link #stageBudget} columns, each costing
     * {@link #stageCost} energy. A trailing frontier only advances while strictly inside its
     * predecessor's radius; a stage-2 column that needs water it can't pay for stalls in place.
     *
     * @return whether any persisted state changed
     */
    private boolean workStage(ServerLevel level, BlockPos center, int stage, Set<LevelChunk> biomeChanged) {
        int cost = stageCost(stage);
        int budget = stageBudget(stage);
        boolean changed = false;

        for (int processed = 0; processed < budget; processed++) {
            if (this.energy.getAmountAsInt() < cost) {
                break;
            }
            int stageRadius = switch (stage) {
                case 2 -> this.hydrationRadius;
                case 3 -> this.lifeRadius;
                default -> this.radius;
            };
            // Trailing frontiers stay strictly inside the predecessor (its current ring is mid-work).
            if (stage == 2 && stageRadius >= this.radius) {
                break;
            }
            if (stage == 3 && stageRadius >= this.hydrationRadius) {
                break;
            }

            List<int[]> r = ring(stage, stageRadius);
            int stageCursor = switch (stage) {
                case 2 -> this.hydrationCursor;
                case 3 -> this.lifeCursor;
                default -> this.cursor;
            };
            if (stageCursor >= r.size()) {
                stageRadius++;          // grow outward — no cap (terraform design §2.1)
                stageCursor = 0;
                setStageFrontier(stage, stageRadius, stageCursor);
                changed = true;
                continue;
            }

            int[] off = r.get(stageCursor);
            int x = center.getX() + off[0];
            int z = center.getZ() + off[1];
            if (!level.hasChunk(x >> 4, z >> 4)) {
                // Lazy: leave unloaded columns for the chunk-load catch-up (TerraformManager). Optionally
                // force-load if the player opted in. No energy spent on a skipped column.
                maybeForceLoad(level, x, z);
                setStageFrontier(stage, stageRadius, stageCursor + 1);
                changed = true;
                continue;
            }

            switch (stage) {
                case 2 -> {
                    boolean done = TerraformConversion.hydrateColumn(level, x, z, waterTableY(),
                            this::drawHydration);
                    if (!done) {
                        // Out of glacite mid-column: stall without advancing — the GUI says why.
                        this.hydrationStalled = true;
                        return changed;
                    }
                }
                case 3 -> TerraformConversion.vivifyColumn(level, x, z, biomeChanged);
                default -> TerraformConversion.convertColumn(level, x, z, this.tier, biomeChanged);
            }
            this.energy.consume(cost);
            setStageFrontier(stage, stageRadius, stageCursor + 1);
            changed = true;
        }
        return changed;
    }

    /** {@link TerraformConversion.HydrationSink} for the live frontier: pays from the buffer. */
    private int drawHydration(int units) {
        int granted = Math.min(units, this.hydration);
        if (granted > 0) {
            this.hydration -= granted;
            setChanged();
        }
        return granted;
    }

    private void setStageFrontier(int stage, int newRadius, int newCursor) {
        switch (stage) {
            case 2 -> {
                this.hydrationRadius = newRadius;
                this.hydrationCursor = newCursor;
            }
            case 3 -> {
                this.lifeRadius = newRadius;
                this.lifeCursor = newCursor;
            }
            default -> {
                this.radius = newRadius;
                this.cursor = newCursor;
            }
        }
    }

    /** The cached ring offsets for {@code stage} at {@code r} (cache invalidates on radius change). */
    private List<int[]> ring(int stage, int r) {
        int slot = stage - 1;
        if (this.rings[slot] != null && this.ringsFor[slot] == r) {
            return this.rings[slot];
        }
        List<int[]> out = ringOffsets(r);
        this.rings[slot] = out;
        this.ringsFor[slot] = r;
        return out;
    }

    /** Column offsets of the circular shell {@code [r, r+1)} (the original frontier geometry). */
    static List<int[]> ringOffsets(int r) {
        List<int[]> out = new ArrayList<>();
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
        // Deeper terraforming (DEEPER_TERRAFORM_DESIGN.md §9): additive keys only — a legacy save
        // simply lacks them and loads with the trailing frontiers at 0.
        output.putInt("HydrationRadius", this.hydrationRadius);
        output.putInt("HydrationCursor", this.hydrationCursor);
        output.putInt("LifeRadius", this.lifeRadius);
        output.putInt("LifeCursor", this.lifeCursor);
        output.putInt("Hydration", this.hydration);
        output.store("Upgrade", ItemStack.OPTIONAL_CODEC, this.upgradeHandler.getStack(UPGRADE_SLOT));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.deserialize(input.childOrEmpty("Energy"));
        this.tier = Math.max(1, input.getIntOr("Tier", 1));
        this.radius = input.getIntOr("Radius", 0);
        this.cursor = input.getIntOr("Cursor", 0);
        this.hydrationRadius = input.getIntOr("HydrationRadius", 0);
        this.hydrationCursor = input.getIntOr("HydrationCursor", 0);
        this.lifeRadius = input.getIntOr("LifeRadius", 0);
        this.lifeCursor = input.getIntOr("LifeCursor", 0);
        this.hydration = input.getIntOr("Hydration", 0);
        this.ringsFor[0] = this.ringsFor[1] = this.ringsFor[2] = -1;
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
            super(Tuning.terraformerBuffer(), ENERGY_MAX_INSERT, 0);
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
