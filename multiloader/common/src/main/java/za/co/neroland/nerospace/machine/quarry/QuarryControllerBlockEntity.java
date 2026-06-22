package za.co.neroland.nerospace.machine.quarry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.energy.EnergyBuffer;
import za.co.neroland.nerospace.energy.NerospaceEnergyStorage;
import za.co.neroland.nerospace.fluid.FluidTank;
import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.module.MachineModules;
import za.co.neroland.nerospace.module.UpgradeModuleItem;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * The quarry controller: the single block that runs the dig. Once landmarks mark a region, it builds a
 * frame ring and excavates the rectangle layer-by-layer from the landmark plane down to the world floor.
 * Mined items buffer internally and auto-eject to adjacent storage; source fluids are sucked into a
 * fluid buffer (drained by pipes). Mining pauses (never loses items) when the buffers fill or power runs
 * out. Throughput scales with supplied power up to the tier's ceiling × the planet's speed factor.
 *
 * <p><b>Cross-loader port note.</b> The root binds the buffers to the NeoForge transfer API, uses a
 * NeoForge chunk-ticket controller, and supports upgrade modules. The multiloader rebuilds the buffers
 * on the shared {@link EnergyBuffer}/{@link FluidTank} and a vanilla {@link WorldlyContainer} (frame in,
 * output out); force-loads via vanilla {@link ServerLevel#setChunkForced}; and <b>defers the modules</b>
 * (speed/energy = 1.0, no Silk/Fortune, no module slots) and the moving drill-head BER. {@code Tuning}
 * values are inlined. Fluids leave via pipe extraction (no auto-eject); items auto-eject to adjacent
 * vanilla containers.</p>
 */
public class QuarryControllerBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int OUTPUT_SLOTS = 12;
    public static final int FRAME_SLOT = 0;
    private static final int OUTPUT_START = 1;
    public static final int ENERGY_MAX_INSERT = 10_000;
    public static final int DATA_COUNT = 7;
    private static final int SCAN_BUDGET_PER_TICK = 4096;

    // Inlined Tuning base values.
    private static final int ENERGY_BUFFER = 200_000;
    private static final int FLUID_CAPACITY = 16_000;
    private static final int ENERGY_PER_BLOCK = 40;
    private static final int MINE_INTERVAL = 8;

    public enum State {
        IDLE, BUILDING_FRAME, MINING, DONE, PAUSED
    }

    private final MinerTier tier;
    private final int moduleSlots;
    private final int containerSize;

    private final EnergyBuffer energy = new EnergyBuffer(ENERGY_BUFFER, ENERGY_MAX_INSERT, 0, this::setChanged);
    private final FluidTank fluidBuffer = new FluidTank(FLUID_CAPACITY, this::setChanged);
    /** Frame casing at index 0, mined output at indices {@code [1, OUTPUT_SLOTS]}. */
    private final NonNullList<ItemStack> items = NonNullList.withSize(1 + OUTPUT_SLOTS, ItemStack.EMPTY);
    private final MachineModules modules;
    private final OutputFilter filter = OutputFilter.KEEP_ALL;

    private State state = State.IDLE;
    private String pauseReason = "";
    @Nullable
    private QuarryRegion region;
    private int frameIndex;
    private int currentY;
    private int cursor;
    private final Set<Integer> skippedColumns = new HashSet<>();
    private final Set<Long> forcedChunks = new HashSet<>();
    private transient int frameTotal = -1;

    /** How often (server ticks) the controller pushes a render snapshot (region/state/cursor) to clients. */
    private static final int RENDER_SYNC_INTERVAL = 10;
    /** Client-only: smoothed drill-head world position for the gantry/drill renderer (eased toward the cell). */
    public double dispX;
    public double dispY;
    public double dispZ;
    public boolean dispInit;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) energy.getAmount();
                case 1 -> (int) energy.getCapacity();
                case 2 -> state.ordinal();
                case 3 -> (int) fluidBuffer.getAmount();
                case 4 -> (int) fluidBuffer.getCapacity();
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
            // server-authoritative
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    @SuppressWarnings("this-escape") // setChanged callback only invoked after construction
    public QuarryControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.QUARRY_CONTROLLER.get(), pos, blockState);
        this.tier = blockState.getBlock() instanceof QuarryControllerBlock controller
                ? controller.tier() : MinerTier.TIER_1;
        this.moduleSlots = this.tier.moduleSlots();
        this.containerSize = 1 + this.moduleSlots + OUTPUT_SLOTS; // frame + modules + output
        this.modules = new MachineModules(this.moduleSlots, this::setChanged);
    }

    public int moduleSlots() {
        return this.moduleSlots;
    }

    // --- Capability accessors ---------------------------------------------------

    public NerospaceEnergyStorage getEnergy() {
        return this.energy;
    }

    public NerospaceFluidStorage getTank() {
        return this.fluidBuffer;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    public MinerTier tier() {
        return this.tier;
    }

    // --- Renderer accessors (gantry + drill-head BER reads the synced dig state) -------------

    @Nullable
    public QuarryRegion renderRegion() {
        return this.region;
    }

    public State renderState() {
        return this.state;
    }

    public int renderCurrentY() {
        return this.currentY;
    }

    public int renderCursor() {
        return this.cursor;
    }

    // --- Ticking ----------------------------------------------------------------

    public void tick(Level level, BlockPos pos, BlockState blockState) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        switch (this.state) {
            case IDLE -> tryActivate(serverLevel, pos);
            case BUILDING_FRAME -> buildFrame(serverLevel);
            case MINING -> {
                if (serverLevel.getGameTime() % miningInterval(serverLevel) == 0L) {
                    mine(serverLevel);
                }
            }
            case PAUSED -> resume(serverLevel, pos);
            case DONE -> { }
            default -> { }
        }
        if (this.region != null) {
            autoEject(serverLevel, pos);
        }
        // Push a throttled render snapshot (region/state/cursor ride the BE update tag) so the gantry +
        // drill-head BER can draw and track the dig while the quarry is working.
        if (this.state != State.IDLE && serverLevel.getGameTime() % RENDER_SYNC_INTERVAL == 0L) {
            serverLevel.sendBlockUpdated(pos, blockState, blockState, Block.UPDATE_CLIENTS);
        }
    }

    // --- Client sync (render state rides the block-entity update packet) ---------------------

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

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

    private void tryActivate(ServerLevel level, BlockPos pos) {
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
        this.currentY = found.refY();
        this.cursor = 0;
        this.skippedColumns.clear();
        this.state = State.BUILDING_FRAME;
        this.pauseReason = "";
        setChanged();
    }

    private int frameTotal() {
        QuarryRegion rg = this.region;
        if (this.frameTotal < 0 && rg != null) {
            this.frameTotal = rg.framePositions().size();
        }
        return Math.max(0, this.frameTotal);
    }

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
            if (fp.equals(this.worldPosition) || existing.hasBlockEntity()) {
                this.frameIndex++;
                continue;
            }
            ItemStack casing = this.items.get(FRAME_SLOT);
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
            this.currentY = region.refY() - 1;
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
        ItemStack tool = miningTool(level);
        int columns = region.columns();
        boolean changed = false;

        int scanned = 0;
        for (int processed = 0; processed < 1 && scanned < SCAN_BUDGET_PER_TICK; ) {
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

            if (region.isPerimeter(x, z)) {
                this.cursor++;
                continue;
            }
            if (isColumnSkipped(x, z)) {
                this.cursor++;
                continue;
            }

            int cx = x >> 4;
            int cz = z >> 4;
            if (!level.hasChunk(cx, cz)) {
                forceLoad(level, cx, cz);
                changed = true;
                break;
            }

            BlockState state = level.getBlockState(target);
            if (state.isAir() || state.getBlock() instanceof QuarryFrameBlock) {
                this.cursor++;
                continue;
            }
            if (state.hasBlockEntity()) {
                markColumnSkipped(x, z);
                this.cursor++;
                continue;
            }

            FluidState fluidState = state.getFluidState();
            if (state.getBlock() instanceof LiquidBlock && fluidState.isSource()) {
                if (!suckFluid(level, target, fluidState)) {
                    setPaused("fluid_full");
                    changed = true;
                    break;
                }
                this.cursor++;
                changed = true;
                continue;
            }

            if (state.getDestroySpeed(level, target) < 0.0F) {
                this.cursor++;
                continue;
            }

            if (this.energy.getAmount() < energyPerBlock) {
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

    private long miningInterval(ServerLevel level) {
        double planet = PlanetMiningProfile.forDimension(level.dimension()).speedMultiplier();
        double rate = this.tier.baseBlocksPerCycle() * this.modules.speedMultiplier() * planet
                * NerospaceConfig.machineSpeedMultiplier();
        return Math.max(1L, Math.round(MINE_INTERVAL / Math.max(0.01, rate)));
    }

    private int quarryEnergyPerBlock() {
        return Math.max(1, (int) Math.round(ENERGY_PER_BLOCK * this.modules.energyMultiplier()));
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

    /** Try to buffer all kept drops atomically into the output slots; filtered-out drops are voided. */
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
            sim[i] = this.items.get(OUTPUT_START + i).copy();
        }
        for (ItemStack drop : kept) {
            if (!mergeInto(sim, drop)) {
                return false;
            }
        }
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            this.items.set(OUTPUT_START + i, sim[i]);
        }
        return true;
    }

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

    private void spawnDrillFx(ServerLevel level, BlockPos target) {
        double cx = target.getX() + 0.5;
        double cy = target.getY() + 0.5;
        double cz = target.getZ() + 0.5;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                cx, cy, cz, 3, 0.2, 0.2, 0.2, 0.0);
    }

    private boolean suckFluid(ServerLevel level, BlockPos pos, FluidState fluidState) {
        Fluid fluid = fluidState.getType();
        if (this.fluidBuffer.fill(fluid, 1000, true) >= 1000) {
            this.fluidBuffer.fill(fluid, 1000, false);
            level.removeBlock(pos, false);
            return true;
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

    // --- Auto-eject (items → adjacent vanilla containers) -----------------------

    private void autoEject(ServerLevel level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof Container target && !(target instanceof QuarryControllerBlockEntity)) {
                ejectInto(target);
            }
        }
    }

    /** Push output stacks into a neighbour container (best-effort merge). */
    private void ejectInto(Container target) {
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack stack = this.items.get(OUTPUT_START + i);
            if (stack.isEmpty()) {
                continue;
            }
            for (int t = 0; t < target.getContainerSize() && !stack.isEmpty(); t++) {
                if (!target.canPlaceItem(t, stack)) {
                    continue;
                }
                ItemStack dest = target.getItem(t);
                if (dest.isEmpty()) {
                    target.setItem(t, stack.copy());
                    stack.setCount(0);
                } else if (ItemStack.isSameItemSameComponents(dest, stack)) {
                    int room = Math.min(dest.getMaxStackSize(), target.getMaxStackSize()) - dest.getCount();
                    int moved = Math.min(room, stack.getCount());
                    if (moved > 0) {
                        dest.grow(moved);
                        stack.shrink(moved);
                    }
                }
            }
            this.items.set(OUTPUT_START + i, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
        target.setChanged();
        setChanged();
    }

    // --- Chunk loading (vanilla force-load; one chunk pinned at a time) ----------

    private void forceLoad(ServerLevel level, int cx, int cz) {
        long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
        Iterator<Long> it = this.forcedChunks.iterator();
        while (it.hasNext()) {
            long k = it.next();
            if (k != key) {
                level.setChunkForced((int) (k >> 32), (int) (k & 0xFFFFFFFFL), false);
                it.remove();
            }
        }
        if (this.forcedChunks.add(key)) {
            level.setChunkForced(cx, cz, true);
        }
    }

    private void releaseForcedChunks(ServerLevel level) {
        for (long key : this.forcedChunks) {
            level.setChunkForced((int) (key >> 32), (int) (key & 0xFFFFFFFFL), false);
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
        }
        super.setRemoved();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level instanceof ServerLevel serverLevel) {
            removeFrame(serverLevel);
        }
    }

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
        output.putInt("Energy", this.energy.getRaw());
        output.putString("Fluid", BuiltInRegistries.FLUID.getKey(this.fluidBuffer.getRawFluid()).toString());
        output.putInt("FluidAmount", this.fluidBuffer.getRawAmount());
        output.store("Frame", ItemStack.OPTIONAL_CODEC, this.items.get(FRAME_SLOT));
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            output.store("Out" + i, ItemStack.OPTIONAL_CODEC, this.items.get(OUTPUT_START + i));
        }
        this.modules.save(output);
        output.putString("MinerState", this.state.name());
        output.putString("PauseReason", this.pauseReason);
        output.putInt("FrameIndex", this.frameIndex);
        output.putInt("CurrentY", this.currentY);
        output.putInt("Cursor", this.cursor);
        QuarryRegion region = this.region;
        output.putBoolean("HasRegion", region != null);
        if (region != null) {
            region.save(output.child("Region"));
        }
        int[] skipped = this.skippedColumns.stream().mapToInt(Integer::intValue).toArray();
        output.putInt("SkipCount", skipped.length);
        for (int i = 0; i < skipped.length; i++) {
            output.putInt("Skip" + i, skipped[i]);
        }
        long[] chunks = this.forcedChunks.stream().mapToLong(Long::longValue).toArray();
        output.putInt("ChunkCount", chunks.length);
        for (int i = 0; i < chunks.length; i++) {
            output.putLong("Chunk" + i, chunks[i]);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.setRaw(input.getIntOr("Energy", 0));
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(input.getStringOr("Fluid", "minecraft:empty")));
        this.fluidBuffer.setRaw(fluid, input.getIntOr("FluidAmount", 0));
        this.items.set(FRAME_SLOT, input.read("Frame", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            this.items.set(OUTPUT_START + i, input.read("Out" + i, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
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
        this.forcedChunks.clear();
        int chunkCount = input.getIntOr("ChunkCount", 0);
        for (int i = 0; i < chunkCount; i++) {
            this.forcedChunks.add(input.getLongOr("Chunk" + i, 0L));
        }
    }

    private static State parseState(String name) {
        try {
            return State.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return State.IDLE;
        }
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

    // --- WorldlyContainer (combined view: [0]=frame, [1..M]=modules, [M+1..]=output) ----
    // Frame + output live in `items` (index 0 + 1..OUTPUT_SLOTS); modules live in `modules`.

    private NonNullList<ItemStack> routeList(int slot) {
        return (slot >= OUTPUT_START && slot <= this.moduleSlots) ? this.modules.items() : this.items;
    }

    private int routeIndex(int slot) {
        if (slot == FRAME_SLOT) {
            return 0;
        }
        if (slot <= this.moduleSlots) {
            return slot - 1;                 // modules: 0..moduleSlots-1
        }
        return slot - this.moduleSlots;      // output: items[1..OUTPUT_SLOTS]
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] slots = new int[this.containerSize];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot > this.moduleSlots; // output slots only
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == FRAME_SLOT) {
            return stack.is(ModItems.FRAME_CASING.get());
        }
        if (slot <= this.moduleSlots) {
            return UpgradeModuleItem.isModule(stack);
        }
        return false; // output slots: take only
    }

    @Override
    public int getContainerSize() {
        return this.containerSize;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        for (ItemStack stack : this.modules.items()) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return routeList(slot).get(routeIndex(slot));
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack r = ContainerHelper.removeItem(routeList(slot), routeIndex(slot), amount);
        if (!r.isEmpty()) {
            this.setChanged();
        }
        return r;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(routeList(slot), routeIndex(slot));
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        routeList(slot).set(routeIndex(slot), stack);
        this.setChanged();
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
        this.items.clear();
        this.modules.items().clear();
    }
}
