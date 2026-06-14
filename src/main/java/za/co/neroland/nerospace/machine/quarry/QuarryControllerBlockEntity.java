package za.co.neroland.nerospace.machine.quarry;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.Tuning;
import za.co.neroland.nerospace.machine.MachineItemHandler;
import za.co.neroland.nerospace.module.MachineModules;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * The quarry controller (MINER_DESIGN): the single block that runs the dig. Once landmarks mark an
 * L-shaped region nearby, it materialises a frame ring (one {@code frame_casing} per open perimeter
 * cell) and then excavates the rectangle layer-by-layer from the landmark plane down to the world
 * floor, like a 3D printer in reverse. Mined items buffer internally and auto-eject to adjacent
 * storage / pipes; source fluids are sucked into a fluid buffer that auto-ejects to adjacent tanks.
 * Mining pauses (never loses items) when the buffers fill or power runs out.
 *
 * <p>Throughput scales with supplied power up to the tier's per-tick ceiling × the modules' speed
 * multiplier × the planet's speed factor. See {@link MinerTier}, {@link MachineModules},
 * {@link PlanetMiningProfile}.</p>
 */
public class QuarryControllerBlockEntity extends BlockEntity implements Container, MenuProvider {

    /** Internal output buffer slot count. */
    public static final int OUTPUT_SLOTS = 12;
    public static final int FRAME_SLOT = 0;
    /** Insert cap on the energy buffer (so feeding more power raises throughput to the tier ceiling). */
    public static final int ENERGY_MAX_INSERT = 10_000;
    public static final int DATA_COUNT = 7;
    /** Max cells examined per tick (mined + skipped) so empty volumes can't spike a single tick. */
    private static final int SCAN_BUDGET_PER_TICK = 4096;

    /** Lifecycle of the dig. */
    public enum State {
        IDLE, BUILDING_FRAME, MINING, DONE, PAUSED
    }

    private final MinerTier tier;
    private final int moduleSlots;
    private final int containerSize;

    private final QuarryEnergy energy = new QuarryEnergy();

    @SuppressWarnings("this-escape")
    private final MachineItemHandler frameHandler = new MachineItemHandler(1, this::setChanged,
            (index, resource) -> resource.toStack(1).is(ModItems.FRAME_CASING.get()));

    /** Output buffer: external insertion is rejected (mining-only); extraction (pipes) is allowed. */
    @SuppressWarnings("this-escape")
    private final MachineItemHandler outputHandler = new MachineItemHandler(OUTPUT_SLOTS, this::setChanged,
            (index, resource) -> false);

    @SuppressWarnings("this-escape")
    private final MachineModules modules;

    @SuppressWarnings("this-escape")
    private final QuarryFluidBuffer fluidBuffer = new QuarryFluidBuffer();

    private final OutputFilter filter = OutputFilter.KEEP_ALL;

    private State state = State.IDLE;
    private String pauseReason = "";
    @Nullable
    private QuarryRegion region;
    private int frameIndex;
    private int currentY;
    private int cursor;
    /** Columns (packed relative dx*128+dz) skipped because a tile-entity sits in them. */
    private final IntOpenHashSet skippedColumns = new IntOpenHashSet();
    /** Chunks this quarry force-loads while mining. */
    private final transient LongOpenHashSet forcedChunks = new LongOpenHashSet();
    /** Cached frame-cell count (recomputed lazily). */
    private transient int frameTotal = -1;
    /** Last state pushed to clients (so a change forces a sync). */
    private transient State lastSyncedState = State.IDLE;
    /** Client-side smoothed drill-head position (interpolated by the renderer). */
    public double dispX;
    public double dispY;
    public double dispZ;
    public boolean dispInit;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getAmountAsInt();
                case 1 -> energy.getCapacityAsInt();
                case 2 -> state.ordinal();
                case 3 -> fluidBuffer.getAmountAsInt(0);
                case 4 -> fluidBuffer.getCapacity();
                case 5 -> currentY;
                case 6 -> {
                    QuarryRegion rg = region;
                    yield rg == null ? 0 : rg.refY();
                }
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // server-authoritative; nothing client-settable
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    @SuppressWarnings("this-escape") // setChanged callbacks are only invoked after construction
    public QuarryControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.QUARRY_CONTROLLER.get(), pos, blockState);
        this.tier = blockState.getBlock() instanceof QuarryControllerBlock controller
                ? controller.tier() : MinerTier.TIER_1;
        this.moduleSlots = this.tier.moduleSlots();
        this.modules = new MachineModules(this.moduleSlots, this::setChanged);
        this.containerSize = 1 + this.moduleSlots + OUTPUT_SLOTS;
    }

    // --- Capability accessors ---------------------------------------------------

    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    public ResourceHandler<ItemResource> getOutputHandler() {
        return this.outputHandler;
    }

    public ResourceHandler<ItemResource> getFrameHandler() {
        return this.frameHandler;
    }

    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.fluidBuffer;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    public MinerTier tier() {
        return this.tier;
    }

    // --- Client render accessors (populated on the client via the update tag) ----

    public State renderState() {
        return this.state;
    }

    @Nullable
    public QuarryRegion renderRegion() {
        return this.region;
    }

    /** Linear column index currently being mined (clamped into range for the renderer). */
    public int renderCursor() {
        return this.cursor;
    }

    public int renderCurrentY() {
        return this.currentY;
    }

    /**
     * Creative/gallery helper: adopt an already-built {@code region} (frame assumed placed) and drop
     * straight into a powered MINING state from {@code startY}, so a staged display mines for real.
     */
    public void stageDisplay(QuarryRegion region, int startY) {
        this.region = region;
        this.frameTotal = region.framePositions().size();
        this.frameIndex = this.frameTotal;
        this.currentY = startY;
        this.cursor = 0;
        this.skippedColumns.clear();
        this.state = State.MINING;
        this.pauseReason = "";
        this.energy.fill();
        setChanged();
    }

    // --- Ticking ----------------------------------------------------------------

    public void tick(Level level, BlockPos pos, BlockState blockState) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        State before = this.state;
        switch (this.state) {
            case IDLE -> tryActivate(serverLevel, pos);
            case BUILDING_FRAME -> buildFrame(serverLevel);
            case MINING -> mine(serverLevel);
            case PAUSED -> resume(serverLevel, pos);
            case DONE -> { }
            default -> { }
        }

        // Always try to drain the buffers once we own a region — otherwise a "buffer full" pause
        // could never clear itself (nothing would push items/fluids out while paused).
        if (this.region != null) {
            autoEject(serverLevel, pos);
        }

        // Push a client update on a state change, and periodically while mining so the drill-head
        // renderer can follow the cursor.
        boolean stateChanged = this.state != before || this.state != this.lastSyncedState;
        if (stateChanged || (this.state == State.MINING && serverLevel.getGameTime() % 5L == 0L)) {
            this.lastSyncedState = this.state;
            serverLevel.sendBlockUpdated(pos, blockState, blockState, Block.UPDATE_CLIENTS);
        }
    }

    /** Resume a paused dig: retry the phase that paused, without bouncing the displayed state. */
    private void resume(ServerLevel level, BlockPos pos) {
        if (this.region == null) {
            this.state = State.IDLE;
            tryActivate(level, pos);
            return;
        }
        if (!this.tier.canOperateIn(level.dimension())) {
            this.pauseReason = "wrong_planet";
            return;
        }
        if (this.frameIndex < frameTotal()) {
            this.state = State.BUILDING_FRAME;
            buildFrame(level);
        } else {
            this.state = State.MINING;
            mine(level);
        }
    }

    /** Look for landmarks and, if a valid region exists and the planet allows this tier, start. */
    private void tryActivate(ServerLevel level, BlockPos pos) {
        // Throttle the search.
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        if (!this.tier.canOperateIn(level.dimension())) {
            setPaused("wrong_planet");
            return;
        }
        BlockPos seed = QuarryRegion.findNearbyLandmark(level, pos, this.tier.maxAreaSide());
        if (seed == null) {
            return;
        }
        QuarryRegion found = QuarryRegion.fromLandmarks(level, seed, this.tier.maxAreaSide());
        if (found == null) {
            setPaused("bad_region");
            return;
        }
        this.region = found;
        this.frameTotal = -1;
        consumeLandmarks(level, found);
        this.frameIndex = 0;
        // Stay at the reference plane while building (depth 0); drop to the first layer when mining.
        this.currentY = found.refY();
        this.cursor = 0;
        this.skippedColumns.clear();
        this.state = State.BUILDING_FRAME;
        this.pauseReason = "";
        setChanged();
    }

    /** Total frame cells (cached; recomputed lazily from the region). */
    private int frameTotal() {
        QuarryRegion rg = this.region;
        if (this.frameTotal < 0 && rg != null) {
            this.frameTotal = rg.framePositions().size();
        }
        return Math.max(0, this.frameTotal);
    }

    /** Remove the landmark blocks inside the claimed rectangle at the reference plane. */
    private void consumeLandmarks(ServerLevel level, QuarryRegion region) {
        for (int x = region.minX(); x <= region.maxX(); x++) {
            for (int z = region.minZ(); z <= region.maxZ(); z++) {
                BlockPos lp = new BlockPos(x, region.refY(), z);
                if (level.getBlockState(lp).getBlock() instanceof QuarryLandmarkBlock) {
                    level.removeBlock(lp, false);
                }
            }
        }
    }

    /** Place the frame ring a few cells per tick, consuming a casing per open cell. */
    private void buildFrame(ServerLevel level) {
        QuarryRegion region = this.region;
        if (region == null) {
            this.state = State.IDLE;
            return;
        }
        List<BlockPos> ring = region.framePositions();
        int placedThisTick = 0;
        boolean changed = false;
        while (this.frameIndex < ring.size() && placedThisTick < 8) {
            BlockPos fp = ring.get(this.frameIndex);
            BlockState existing = level.getBlockState(fp);
            if (existing.getBlock() instanceof QuarryFrameBlock) {
                this.frameIndex++;
                continue;
            }
            // Only draw the frame through open cells; terrain-backed perimeter cells need no casing.
            if (!existing.isAir() && !existing.canBeReplaced()) {
                this.frameIndex++;
                continue;
            }
            ItemStack casing = this.frameHandler.getStack(FRAME_SLOT);
            if (casing.isEmpty()) {
                setPaused("need_material");
                return;
            }
            level.setBlock(fp, ModBlocks.QUARRY_FRAME.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            casing.shrink(1);
            placedThisTick++;
            this.frameIndex++;
            changed = true;
        }
        if (this.frameIndex >= ring.size()) {
            this.state = State.MINING;
            this.currentY = region.refY() - 1;   // drop to the first layer below the frame plane
            this.pauseReason = "";
            changed = true;
        }
        if (changed) {
            setChanged();
        }
    }

    private void mine(ServerLevel level) {
        QuarryRegion region = this.region;
        if (region == null) {
            this.state = State.IDLE;
            return;
        }
        int floor = level.getMinY();
        int energyPerBlock = quarryEnergyPerBlock();
        int cap = blocksPerTick(level);
        ItemStack tool = miningTool(level);
        int columns = region.columns();
        boolean changed = false;

        // Skips (air, caves, already-cleared cells) don't cost energy or count toward the mined cap,
        // so bound the cells examined per tick to avoid a spike when sweeping large empty volumes.
        int scanned = 0;
        for (int processed = 0; processed < cap && scanned < SCAN_BUDGET_PER_TICK; ) {
            scanned++;
            if (this.currentY < floor) {
                this.state = State.DONE;
                releaseForcedChunks(level);
                changed = true;
                break;
            }
            if (this.cursor >= columns) {
                this.cursor = 0;
                this.currentY--;
                continue;
            }
            BlockPos target = region.columnPos(this.cursor, this.currentY);
            int x = target.getX();
            int z = target.getZ();

            if (isColumnSkipped(x, z)) {
                this.cursor++;
                continue;
            }

            int cx = x >> 4;
            int cz = z >> 4;
            if (!level.hasChunk(cx, cz)) {
                forceLoad(level, cx, cz);
                changed = true;
                break; // retry next tick once the chunk is in
            }

            BlockState state = level.getBlockState(target);
            if (state.isAir() || state.getBlock() instanceof QuarryFrameBlock) {
                this.cursor++;
                continue;
            }
            // Respect tile-entities: skip the whole column (richer handling is a follow-up).
            if (state.hasBlockEntity()) {
                markColumnSkipped(x, z);
                this.cursor++;
                continue;
            }

            FluidState fluidState = state.getFluidState();
            if (state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock && fluidState.isSource()) {
                // A pure liquid source block: pull it into the fluid buffer or pause if full.
                if (!suckFluid(level, target, fluidState)) {
                    setPaused("fluid_full");
                    changed = true;
                    break;
                }
                this.cursor++;
                changed = true;
                continue;
            }

            // Unbreakable (bedrock, barrier, command blocks): skip without spending energy.
            if (state.getDestroySpeed(level, target) < 0.0F) {
                this.cursor++;
                continue;
            }

            if (this.energy.getAmountAsInt() < energyPerBlock) {
                setPaused("no_power");
                changed = true;
                break;
            }

            List<ItemStack> drops = Block.getDrops(state, level, target, null, null, tool);
            if (!acceptDrops(drops)) {
                setPaused("buffer_full");
                changed = true;
                break;
            }
            level.removeBlock(target, false);
            spawnDrillFx(level, target);
            this.energy.consume(energyPerBlock);
            this.cursor++;
            processed++;
            changed = true;
        }

        if (changed) {
            setChanged();
        }
    }

    /** Per-tick block ceiling = tier base × module speed × planet speed, ≥ 1. */
    private int blocksPerTick(ServerLevel level) {
        double planet = PlanetMiningProfile.forDimension(level.dimension()).speedMultiplier();
        double scaled = this.tier.baseBlocksPerCycle() * this.modules.speedMultiplier() * planet;
        return Math.max(1, (int) Math.round(scaled));
    }

    private int quarryEnergyPerBlock() {
        return Math.max(1, (int) Math.round(Tuning.quarryEnergyPerBlock() * this.modules.energyMultiplier()));
    }

    /** Build the synthetic harvest tool reflecting the Silk-Touch / Fortune modules. */
    private ItemStack miningTool(ServerLevel level) {
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (this.modules.silkTouch()) {
            tool.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        } else {
            int fortune = this.modules.fortuneLevel();
            if (fortune > 0) {
                tool.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), fortune);
            }
        }
        return tool;
    }

    /** Try to buffer all kept drops atomically; filtered-out drops are voided. */
    private boolean acceptDrops(List<ItemStack> drops) {
        List<ItemStack> kept = new ArrayList<>();
        for (ItemStack drop : drops) {
            if (!drop.isEmpty() && this.filter.keep(drop)) {
                kept.add(drop.copy());
            }
        }
        if (kept.isEmpty()) {
            return true;
        }
        ItemStack[] sim = new ItemStack[OUTPUT_SLOTS];
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            sim[i] = this.outputHandler.getStack(i).copy();
        }
        for (ItemStack drop : kept) {
            if (!mergeInto(sim, drop)) {
                return false;
            }
        }
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            this.outputHandler.setStack(i, sim[i]);
        }
        return true;
    }

    /** Merge {@code stack} into the slot array (existing matches first, then empty); true if it all fit. */
    private static boolean mergeInto(ItemStack[] slots, ItemStack stack) {
        for (int i = 0; i < slots.length && !stack.isEmpty(); i++) {
            ItemStack slot = slots[i];
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                int room = Math.min(slot.getMaxStackSize(), 64) - slot.getCount();
                int moved = Math.min(room, stack.getCount());
                if (moved > 0) {
                    slot.grow(moved);
                    stack.shrink(moved);
                }
            }
        }
        for (int i = 0; i < slots.length && !stack.isEmpty(); i++) {
            if (slots[i].isEmpty()) {
                slots[i] = stack.copy();
                stack.setCount(0);
            }
        }
        return stack.isEmpty();
    }

    /** A small spark burst at the drill head, plus a glowing beam back up to the frame plane. */
    private void spawnDrillFx(ServerLevel level, BlockPos target) {
        double cx = target.getX() + 0.5;
        double cy = target.getY() + 0.5;
        double cz = target.getZ() + 0.5;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                cx, cy, cz, 3, 0.2, 0.2, 0.2, 0.0);
        QuarryRegion region = this.region;
        if (region != null) {
            double topY = region.refY() + 0.5;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    cx, (cy + topY) / 2.0, cz, 1, 0.02, (topY - cy) / 4.0, 0.02, 0.0);
        }
    }

    private boolean suckFluid(ServerLevel level, BlockPos pos, FluidState fluidState) {
        FluidResource resource = FluidResource.of(fluidState.getType());
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = this.fluidBuffer.insert(0, resource, 1000, tx);
            if (inserted >= 1000) {
                tx.commit();
                level.removeBlock(pos, false);
                return true;
            }
        }
        return false;
    }

    // --- Skipped-column bookkeeping --------------------------------------------

    private int columnKey(int x, int z) {
        QuarryRegion region = this.region;
        if (region == null) {
            return -1;
        }
        return (x - region.minX()) * 128 + (z - region.minZ());
    }

    private boolean isColumnSkipped(int x, int z) {
        return this.skippedColumns.contains(columnKey(x, z));
    }

    private void markColumnSkipped(int x, int z) {
        this.skippedColumns.add(columnKey(x, z));
    }

    // --- Auto-eject -------------------------------------------------------------

    private void autoEject(ServerLevel level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos np = pos.relative(dir);
            ResourceHandler<ItemResource> itemTarget = net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK
                    .getCapability(level, np, null, null, dir.getOpposite());
            if (itemTarget != null) {
                try (Transaction tx = Transaction.openRoot()) {
                    int moved = ResourceHandlerUtil.move(this.outputHandler, itemTarget, r -> true, 16, tx);
                    if (moved > 0) {
                        tx.commit();
                    }
                }
            }
            ResourceHandler<FluidResource> fluidTarget = net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK
                    .getCapability(level, np, null, null, dir.getOpposite());
            if (fluidTarget != null) {
                try (Transaction tx = Transaction.openRoot()) {
                    int moved = ResourceHandlerUtil.move(this.fluidBuffer, fluidTarget, r -> true, 500, tx);
                    if (moved > 0) {
                        tx.commit();
                    }
                }
            }
        }
    }

    // --- Chunk loading ----------------------------------------------------------

    private void forceLoad(ServerLevel level, int cx, int cz) {
        long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
        if (this.forcedChunks.add(key)) {
            QuarryChunkLoader.CONTROLLER.forceChunk(level, this.worldPosition, cx, cz, true, false);
        }
    }

    private void releaseForcedChunks(ServerLevel level) {
        if (this.forcedChunks.isEmpty()) {
            return;
        }
        LongIterator it = this.forcedChunks.iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            int cx = (int) (key >> 32);
            int cz = (int) key;
            QuarryChunkLoader.CONTROLLER.forceChunk(level, this.worldPosition, cx, cz, false, false);
        }
        this.forcedChunks.clear();
    }

    private void setPaused(String reason) {
        this.state = State.PAUSED;
        this.pauseReason = reason;
        setChanged();
    }

    @Override
    public void setRemoved() {
        if (this.level instanceof ServerLevel serverLevel) {
            releaseForcedChunks(serverLevel);
            removeFrame(serverLevel);
        }
        super.setRemoved();
    }

    /** Tear down the frame ring this controller built. */
    private void removeFrame(ServerLevel level) {
        QuarryRegion region = this.region;
        if (region == null) {
            return;
        }
        for (BlockPos fp : region.framePositions()) {
            if (level.getBlockState(fp).getBlock() instanceof QuarryFrameBlock) {
                level.removeBlock(fp, false);
            }
        }
    }

    // --- Persistence ------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.serialize(output.child("Energy"));
        this.fluidBuffer.serialize(output.child("Fluid"));
        output.store("Frame", ItemStack.OPTIONAL_CODEC, this.frameHandler.getStack(FRAME_SLOT));
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            output.store("Out" + i, ItemStack.OPTIONAL_CODEC, this.outputHandler.getStack(i));
        }
        this.modules.save(output);
        output.putString("MinerState", this.state.name());
        output.putString("PauseReason", this.pauseReason);
        output.putInt("FrameIndex", this.frameIndex);
        output.putInt("CurrentY", this.currentY);
        output.putInt("Cursor", this.cursor);
        QuarryRegion region = this.region;
        if (region != null) {
            output.putBoolean("HasRegion", true);
            region.save(output.child("Region"));
        } else {
            output.putBoolean("HasRegion", false);
        }
        int[] skipped = this.skippedColumns.toIntArray();
        output.putInt("SkipCount", skipped.length);
        for (int i = 0; i < skipped.length; i++) {
            output.putInt("Skip" + i, skipped[i]);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.deserialize(input.childOrEmpty("Energy"));
        this.fluidBuffer.deserialize(input.childOrEmpty("Fluid"));
        this.frameHandler.setStack(FRAME_SLOT, input.read("Frame", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            this.outputHandler.setStack(i, input.read("Out" + i, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        }
        this.modules.load(input);
        this.state = parseState(input.getStringOr("MinerState", State.IDLE.name()));
        this.pauseReason = input.getStringOr("PauseReason", "");
        this.frameIndex = input.getIntOr("FrameIndex", 0);
        this.currentY = input.getIntOr("CurrentY", 0);
        this.cursor = input.getIntOr("Cursor", 0);
        this.region = input.getBooleanOr("HasRegion", false)
                ? QuarryRegion.load(input.childOrEmpty("Region")) : null;
        this.frameTotal = -1;
        this.skippedColumns.clear();
        int skipCount = input.getIntOr("SkipCount", 0);
        for (int i = 0; i < skipCount; i++) {
            this.skippedColumns.add(input.getIntOr("Skip" + i, -1));
        }
    }

    private static State parseState(String name) {
        try {
            return State.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return State.IDLE;
        }
    }

    // --- Client sync (region + state + cursor drive the drill-head renderer) -----

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    // --- MenuProvider -----------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.quarry_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new QuarryMenu(containerId, playerInventory, this, this.dataAccess, this.moduleSlots);
    }

    // --- Container (combined view: [0]=frame, [1..M]=modules, [M+1..]=output) ----

    public int moduleSlots() {
        return this.moduleSlots;
    }

    private MachineItemHandler routeHandler(int slot) {
        if (slot == FRAME_SLOT) {
            return this.frameHandler;
        }
        if (slot <= this.moduleSlots) {
            return this.modules.store();
        }
        return this.outputHandler;
    }

    private int routeIndex(int slot) {
        if (slot == FRAME_SLOT) {
            return 0;
        }
        if (slot <= this.moduleSlots) {
            return slot - 1;
        }
        return slot - 1 - this.moduleSlots;
    }

    @Override
    public int getContainerSize() {
        return this.containerSize;
    }

    @Override
    public boolean isEmpty() {
        return this.frameHandler.isStoreEmpty() && this.modules.store().isStoreEmpty()
                && this.outputHandler.isStoreEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return routeHandler(slot).getStack(routeIndex(slot));
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return routeHandler(slot).removeStack(routeIndex(slot), amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return routeHandler(slot).takeStack(routeIndex(slot));
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stack.limitSize(Math.min(this.getMaxStackSize(), stack.getMaxStackSize()));
        routeHandler(slot).setStack(routeIndex(slot), stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == FRAME_SLOT) {
            return stack.is(ModItems.FRAME_CASING.get());
        }
        if (slot <= this.moduleSlots) {
            return za.co.neroland.nerospace.module.UpgradeModuleItem.isModule(stack);
        }
        return false; // output slots: take only
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
        this.frameHandler.clearStore();
        this.modules.store().clearStore();
        this.outputHandler.clearStore();
    }

    // --- Internal buffers -------------------------------------------------------

    private final class QuarryEnergy extends SimpleEnergyHandler {
        private QuarryEnergy() {
            super(Tuning.quarryBuffer(), ENERGY_MAX_INSERT, 0);
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            QuarryControllerBlockEntity.this.setChanged();
        }

        void consume(int amount) {
            set(Math.max(0, getAmountAsInt() - amount));
        }

        void fill() {
            set(getCapacityAsInt());
        }
    }

    private final class QuarryFluidBuffer extends FluidStacksResourceHandler {
        private QuarryFluidBuffer() {
            super(1, Tuning.quarryFluidCapacity());
        }

        int getCapacity() {
            return Tuning.quarryFluidCapacity();
        }

        @Override
        protected void onContentsChanged(int index, FluidStack oldStack) {
            QuarryControllerBlockEntity.this.setChanged();
        }
    }
}
