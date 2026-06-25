package za.co.neroland.nerospace.machine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
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

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.menu.TerraformerMenu;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.world.TerraformManager;

/**
 * Terraformer machine (terraform design §2). An internal energy buffer (grid-fed) slowly converts a
 * dead planet into livable ground by advancing an expanding circular frontier from its own column
 * outward, in three trailing stages (Rooted → Hydrated → Living). Conversion is idempotent, so only
 * {@code radius + cursor} per stage need persisting; energy is the throttle. Higher tiers convert more
 * columns per cycle and unlock ore seeding.
 *
 * <p>Cross-loader port note: rebuilt on the shared {@link EnergyBuffer} + a vanilla
 * {@link WorldlyContainer} upgrade slot (the root used the NeoForge transfer API + {@code
 * MachineItemHandler}). Tuning/Config values are inlined. Opt-in active force-loading is deferred — the
 * {@link TerraformManager} chunk-load catch-up converts unloaded columns when they load instead.</p>
 */
public class TerraformerBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int UPGRADE_SLOT = 0;
    public static final int SIZE = 1;
    public static final int DATA_COUNT = 9;

    // --- Inlined Tuning/Config base values (config seam deferred) ---
    public static final int ENERGY_BUFFER = 100_000;
    public static final int ENERGY_MAX_INSERT = 2_000;
    private static final int ENERGY_PER_BLOCK = 12;       // stage 1
    private static final int STAGE2_ENERGY_PER_BLOCK = 24; // ×2
    private static final int STAGE3_ENERGY_PER_BLOCK = 48; // ×4
    private static final int WORK_INTERVAL_TICKS = 8;
    private static final int HYDRATION_CAP = 1_024;
    private static final int MAX_COLUMNS_PER_TICK = 48;
    /** Opt-in (config) force-load window: a (2R+1)² chunk square centred on the machine (R=2 → 25 chunks). */
    private static final int FORCE_LOAD_RADIUS = 2;

    private static final int @org.jspecify.annotations.NonNull[] SLOTS = {UPGRADE_SLOT};

    private final @org.jspecify.annotations.NonNull NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final EnergyBuffer energy = new EnergyBuffer(ENERGY_BUFFER, ENERGY_MAX_INSERT, 0, this::setChanged);

    /** Machine tier (1..3): more columns per cycle, and Tier 3 unlocks ore seeding. */
    private int tier = 1;
    /** Expanding stage-1 frontier: horizontal radius + within-ring column cursor. */
    private int radius;
    private int cursor;
    /** Trailing stage frontiers (invariant: {@code lifeRadius <= hydrationRadius <= radius}). */
    private int hydrationRadius;
    private int hydrationCursor;
    private int lifeRadius;
    private int lifeCursor;
    /** Buffered hydration units (melted glacite, fed by an adjacent Hydration Module — §3.1). */
    private int hydration;
    /** Transient: stage 2 wants water but the hydration buffer ran dry (GUI/Monitor stall reason). */
    private transient boolean hydrationStalled;

    /** Transient per-stage caches of the current ring's column offsets (recomputed on load). */
    private final transient List<@org.jspecify.annotations.Nullable List<int[]>> rings = newRingCache();
    private final transient int[] ringsFor = {-1, -1, -1};

    /** Chunks we currently keep force-loaded (opt-in, config-gated) — {@link #packChunk} long keys. */
    private final transient Set<Long> forcedChunks = new HashSet<>();

    private static List<@org.jspecify.annotations.Nullable List<int[]>> newRingCache() {
        List<@org.jspecify.annotations.Nullable List<int[]>> cache = new ArrayList<>(3);
        cache.add(null);
        cache.add(null);
        cache.add(null);
        return cache;
    }

    /**
     * Synced to the menu: [0]=energy [1]=capacity [2]=tier [3]=radius [4]=hydration
     * [5]=hydrationCap [6]=hydrationRadius [7]=lifeRadius [8]=hydrationStalled.
     */
    private final @org.jspecify.annotations.NonNull ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) (energy.getRaw() * 1000L / ENERGY_BUFFER); // permille (ContainerData syncs as short)
                case 1 -> 1000;
                case 2 -> tier;
                case 3 -> radius;
                case 4 -> hydration;
                case 5 -> HYDRATION_CAP;
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

    /** Exposed via the mod's energy capability/lookup (insert-only — grid powered). */
    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    public int getTier() {
        return this.tier;
    }

    public boolean isActive() {
        return this.energy.getAmount() >= ENERGY_PER_BLOCK;
    }

    public int comparatorSignal() {
        int stored = this.energy.getRaw();
        return stored <= 0 ? 0 : 1 + (int) (stored / (double) ENERGY_BUFFER * 14.0D);
    }

    /** Stage-1 columns converted per work cycle, by tier (capped for TPS). */
    private int budgetPerCycle() {
        int base = switch (this.tier) {
            case 3 -> 6;
            case 2 -> 3;
            default -> 1;
        };
        return Math.min(base, MAX_COLUMNS_PER_TICK);
    }

    /** Per-stage column budget for one work cycle: trailing stages get smaller guaranteed shares. */
    private int stageBudget(int stage) {
        int base = budgetPerCycle();
        return switch (stage) {
            case 2 -> Math.max(1, base / 2);
            case 3 -> Math.max(1, base / 4);
            default -> base;
        };
    }

    /** Per-column energy cost of a stage (stage 2 ×2, stage 3 ×4). */
    private static int stageCost(int stage) {
        return switch (stage) {
            case 2 -> STAGE2_ENERGY_PER_BLOCK;
            case 3 -> STAGE3_ENERGY_PER_BLOCK;
            default -> ENERGY_PER_BLOCK;
        };
    }

    /** The stage-2 water table: one below the machine's own base, derived (not persisted). */
    public int waterTableY() {
        return this.worldPosition.getY() - 1;
    }

    public int getHydration() {
        return this.hydration;
    }

    /** Accepts melted glacite from an adjacent Hydration Module (§3.1). @return units actually accepted. */
    public int acceptHydration(int units) {
        int accepted = Math.min(units, HYDRATION_CAP - this.hydration);
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

        // Opt-in (config) active force-loading: keep a small window of chunks loaded while running.
        updateForcedChunks(serverLevel, pos);

        // Auto-consume tier upgrades: a Nerosteel Ingot → T2, a Cindrite → T3.
        ItemStack upgrade = this.items.get(UPGRADE_SLOT);
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

        // Redstone switch: wired machines only sweep while powered.
        if (serverLevel.getGameTime() % NerospaceConfig.scaleInterval(
                        WORK_INTERVAL_TICKS, NerospaceConfig.machineSpeedMultiplier()) == 0
                && MachineRedstone.allowsRun(level, pos)) {
            work(serverLevel, pos);
        }
    }

    private void work(ServerLevel level, BlockPos center) {
        boolean changed = false;
        Set<LevelChunk> biomeChanged = new HashSet<>();

        // Outermost first: stage 1 keeps priority so breathable ground never waits on water.
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
    }

    /**
     * Runs one stage's frontier for this cycle: up to {@link #stageBudget} columns, each costing
     * {@link #stageCost} energy. A trailing frontier only advances while strictly inside its
     * predecessor's radius; a stage-2 column that needs water it can't pay for stalls in place.
     */
    private boolean workStage(ServerLevel level, BlockPos center, int stage, Set<LevelChunk> biomeChanged) {
        int cost = NerospaceConfig.scale(stageCost(stage), NerospaceConfig.fuelCostMultiplier());
        int budget = stageBudget(stage);
        boolean changed = false;

        for (int processed = 0; processed < budget; processed++) {
            if (this.energy.getAmount() < cost) {
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
                stageRadius++;          // grow outward — no cap
                stageCursor = 0;
                setStageFrontier(stage, stageRadius, stageCursor);
                changed = true;
                continue;
            }

            int[] off = r.get(stageCursor);
            int x = center.getX() + off[0];
            int z = center.getZ() + off[1];
            if (!level.hasChunk(x >> 4, z >> 4)) {
                // Lazy: leave unloaded columns for the chunk-load catch-up (TerraformManager). Active
                // force-loading is deferred (opt-in, off by default in the root). No energy spent.
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
        List<int[]> cached = this.rings.get(slot);
        if (cached != null && this.ringsFor[slot] == r) {
            return cached;
        }
        List<int[]> out = ringOffsets(r);
        this.rings.set(slot, out);
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

    @Override
    public void setRemoved() {
        if (this.level instanceof ServerLevel serverLevel) {
            TerraformManager.get(serverLevel).remove(this.worldPosition);
            releaseForcedChunks(serverLevel);
        }
        super.setRemoved();
    }

    /**
     * Opt-in active force-loading (config {@code terraformerForceLoadEnabled}): while running (powered),
     * keep a bounded {@link #FORCE_LOAD_RADIUS} window of chunks force-loaded so the frontier keeps
     * converting with no player nearby; release them when the machine idles or the config is off. The
     * window is diffed against {@link #forcedChunks} so a steady state issues no per-tick ticket churn.
     */
    private void updateForcedChunks(ServerLevel level, BlockPos center) {
        boolean want = NerospaceConfig.terraformerForceLoadEnabled() && isActive();
        if (!want) {
            if (!this.forcedChunks.isEmpty()) {
                releaseForcedChunks(level);
            }
            return;
        }

        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        Set<Long> desired = new HashSet<>();
        for (int dx = -FORCE_LOAD_RADIUS; dx <= FORCE_LOAD_RADIUS; dx++) {
            for (int dz = -FORCE_LOAD_RADIUS; dz <= FORCE_LOAD_RADIUS; dz++) {
                desired.add(packChunk(cx + dx, cz + dz));
            }
        }
        if (desired.equals(this.forcedChunks)) {
            return; // steady state — no ticket changes needed this tick
        }

        for (long key : new ArrayList<>(this.forcedChunks)) {
            if (!desired.contains(key)) {
                level.setChunkForced((int) key, (int) (key >> 32), false);
            }
        }
        for (long key : desired) {
            if (!this.forcedChunks.contains(key)) {
                level.setChunkForced((int) key, (int) (key >> 32), true);
            }
        }
        this.forcedChunks.clear();
        this.forcedChunks.addAll(desired);
    }

    /** Releases every chunk we force-loaded (on idle / config-off / removal). */
    private void releaseForcedChunks(ServerLevel level) {
        for (long key : this.forcedChunks) {
            level.setChunkForced((int) key, (int) (key >> 32), false);
        }
        this.forcedChunks.clear();
    }

    /** Packs chunk coords (lower 32 bits = x, upper = z) into a {@link #forcedChunks} key. */
    private static long packChunk(int x, int z) {
        return ((long) z << 32) | (x & 0xFFFF_FFFFL);
    }

    // --- Persistence --------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", this.energy.getRaw());
        output.putInt("Tier", this.tier);
        output.putInt("Radius", this.radius);
        output.putInt("Cursor", this.cursor);
        output.putInt("HydrationRadius", this.hydrationRadius);
        output.putInt("HydrationCursor", this.hydrationCursor);
        output.putInt("LifeRadius", this.lifeRadius);
        output.putInt("LifeCursor", this.lifeCursor);
        output.putInt("Hydration", this.hydration);
        output.store("Upgrade", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC, this.items.get(UPGRADE_SLOT));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.setRaw(input.getIntOr("Energy", 0));
        this.tier = Math.max(1, input.getIntOr("Tier", 1));
        this.radius = input.getIntOr("Radius", 0);
        this.cursor = input.getIntOr("Cursor", 0);
        this.hydrationRadius = input.getIntOr("HydrationRadius", 0);
        this.hydrationCursor = input.getIntOr("HydrationCursor", 0);
        this.lifeRadius = input.getIntOr("LifeRadius", 0);
        this.lifeCursor = input.getIntOr("LifeCursor", 0);
        this.hydration = input.getIntOr("Hydration", 0);
        this.ringsFor[0] = this.ringsFor[1] = this.ringsFor[2] = -1;
        this.items.set(UPGRADE_SLOT, za.co.neroland.nerospace.NerospaceCommon.orElse(
                input.read("Upgrade", za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC), ItemStack.EMPTY));
    }

    // --- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.terraformer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @org.jspecify.annotations.NonNull Inventory playerInventory, Player player) {
        return new TerraformerMenu(containerId, playerInventory, this, this.dataAccess);
    }

    // --- WorldlyContainer: a single tier-upgrade slot -----------------------

    private static boolean slotAccepts(ItemStack stack) {
        return stack.is(ModItems.NEROSTEEL_INGOT.get()) || stack.is(ModItems.CINDRITE.get());
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slotAccepts(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slotAccepts(stack);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return this.items.get(UPGRADE_SLOT).isEmpty();
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
        Level currentLevel = this.level;
        if (currentLevel == null || currentLevel.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }
}
