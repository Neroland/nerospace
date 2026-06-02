package za.co.neroland.nerospace.machine;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.ModTags;
import za.co.neroland.nerospace.world.TerraformChunkLoader;

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

    public static final int FUEL_SLOT = 0;
    public static final int UPGRADE_SLOT = 1;
    public static final int SIZE = 2;

    public static final int ENERGY_CAPACITY = 100_000;
    public static final int ENERGY_MAX_INSERT = 2_000;
    public static final int GENERATE_PER_TICK = 40;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final TerraformerEnergy energy = new TerraformerEnergy();
    private final ItemStacksResourceHandler fuelHandler = new ItemStacksResourceHandler(this.items) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return index == FUEL_SLOT && OxygenGeneratorBlockEntity.fuelValue(resource.toStack(1)) > 0;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack oldStack) {
            TerraformerBlockEntity.this.setChanged();
        }
    };

    private int burnTime;
    private int maxBurnTime;
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

    /** Synced to the menu: [0]=energy [1]=capacity [2]=burnTime [3]=maxBurnTime [4]=tier [5]=radius. */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getAmountAsInt();
                case 1 -> energy.getCapacityAsInt();
                case 2 -> burnTime;
                case 3 -> maxBurnTime;
                case 4 -> tier;
                case 5 -> radius;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 2 -> burnTime = value;
                case 3 -> maxBurnTime = value;
                case 4 -> tier = value;
                case 5 -> radius = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public TerraformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TERRAFORMER.get(), pos, state);
    }

    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    public ResourceHandler<ItemResource> getFuelHandler() {
        return this.fuelHandler;
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
            case 3 -> 24;
            case 2 -> 10;
            default -> 4;
        };
        return Math.min(base, Config.TERRAFORM_MAX_COLUMNS_PER_TICK.get());
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Fuel → energy (shared chassis with the Oxygen Generator).
        if (this.burnTime > 0) {
            this.burnTime--;
            this.energy.generate(GENERATE_PER_TICK);
        } else {
            ItemStack fuel = this.items.get(FUEL_SLOT);
            int value = OxygenGeneratorBlockEntity.fuelValue(fuel);
            if (value > 0 && this.energy.getAmountAsInt() < ENERGY_CAPACITY) {
                this.burnTime = value;
                this.maxBurnTime = value;
                fuel.shrink(1);
                setChanged();
            } else if (this.maxBurnTime != 0) {
                this.maxBurnTime = 0;
            }
        }

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

        if (serverLevel.getGameTime() % Config.TERRAFORM_WORK_INTERVAL_TICKS.get() == 0) {
            work(serverLevel, pos);
        }
    }

    private void work(ServerLevel level, BlockPos center) {
        int cost = Config.TERRAFORM_ENERGY_PER_BLOCK.get();
        int budget = budgetPerCycle();
        boolean changed = false;

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
            convertColumn(level, center.getX() + off[0], center.getZ() + off[1]);
            this.energy.consume(cost);
            changed = true;
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

    /** Convert one surface column (idempotent). @return true if the chunk was loaded and processed. */
    private boolean convertColumn(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, 0, z);
        if (!level.hasChunkAt(pos)) {
            maybeForceLoad(level, x, z);
            return false; // lazy: skip unloaded columns (terraform design §2.3)
        }
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        BlockPos top = new BlockPos(x, surfaceY - 1, z);
        BlockState topState = level.getBlockState(top);

        if (topState.is(ModTags.Blocks.TERRAFORM_TO_GRASS) && !topState.is(Blocks.GRASS_BLOCK)) {
            level.setBlock(top, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int d = 1; d <= 3; d++) {
                BlockPos below = new BlockPos(x, surfaceY - 1 - d, z);
                BlockState bs = level.getBlockState(below);
                if (bs.is(ModTags.Blocks.TERRAFORM_TO_DIRT)) {
                    level.setBlock(below, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            frontierFx(level, x, surfaceY, z);
        }

        // Atmosphere payoff (§3.4): flag the chunk permanently breathable at/above the surface.
        LevelChunk chunk = level.getChunkAt(top);
        if (!Boolean.TRUE.equals(chunk.getData(za.co.neroland.nerospace.registry.ModAttachments.TERRAFORMED))) {
            chunk.setData(za.co.neroland.nerospace.registry.ModAttachments.TERRAFORMED, Boolean.TRUE);
            chunk.markUnsaved();
        }

        scatterPlant(level, x, surfaceY, z);
        seedResource(level, x, surfaceY, z);
        return true;
    }

    /** Sparse grass/flower/sapling scatter on freshly grassed ground (terraform design §2.2). */
    private void scatterPlant(ServerLevel level, int x, int surfaceY, int z) {
        if (!Config.TERRAFORM_PLANTS_ENABLED.get()) {
            return;
        }
        RandomSource rnd = level.getRandom();
        if (rnd.nextDouble() >= Config.TERRAFORM_PLANT_CHANCE.get()) {
            return;
        }
        BlockPos ground = new BlockPos(x, surfaceY - 1, z);
        BlockPos above = new BlockPos(x, surfaceY, z);
        if (!level.getBlockState(ground).is(Blocks.GRASS_BLOCK) || !level.getBlockState(above).isAir()) {
            return;
        }
        double roll = rnd.nextDouble();
        Block plant;
        if (roll < 0.06D) {
            plant = Blocks.OAK_SAPLING;
        } else if (roll < 0.30D) {
            plant = switch (rnd.nextInt(4)) {
                case 0 -> Blocks.POPPY;
                case 1 -> Blocks.DANDELION;
                case 2 -> Blocks.CORNFLOWER;
                default -> Blocks.AZURE_BLUET;
            };
        } else {
            plant = Blocks.SHORT_GRASS;
        }
        level.setBlock(above, plant.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Tier-3 low-rate ore seeding into the converted subsurface (terraform design §2.2 / §T3). */
    private void seedResource(ServerLevel level, int x, int surfaceY, int z) {
        if (this.tier < 3 || !Config.TERRAFORM_RESOURCES_ENABLED.get()) {
            return;
        }
        RandomSource rnd = level.getRandom();
        if (rnd.nextDouble() >= Config.TERRAFORM_RESOURCE_CHANCE.get()) {
            return;
        }
        Block ore = TerraformResources.pickOre(rnd);
        if (ore == null) {
            return;
        }
        int y = surfaceY - 4 - rnd.nextInt(8);
        BlockPos orePos = new BlockPos(x, y, z);
        if (level.hasChunkAt(orePos) && level.getBlockState(orePos).is(ModTags.Blocks.TERRAFORM_TO_DIRT)) {
            level.setBlock(orePos, ore.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Sparse green frontier dust + a soft soil sound as a shell converts (terraform design §2.4). */
    private void frontierFx(ServerLevel level, int x, int surfaceY, int z) {
        RandomSource rnd = level.getRandom();
        if (rnd.nextFloat() < 0.10F) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    x + 0.5D, surfaceY + 0.1D, z + 0.5D, 2, 0.3D, 0.2D, 0.3D, 0.0D);
        }
        if (rnd.nextFloat() < 0.02F) {
            level.playSound(null, x + 0.5D, surfaceY, z + 0.5D,
                    SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.3F, 0.8F + rnd.nextFloat() * 0.3F);
        }
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
        super.setRemoved();
    }

    // --- Persistence --------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.serialize(output.child("Energy"));
        output.putInt("BurnTime", this.burnTime);
        output.putInt("MaxBurnTime", this.maxBurnTime);
        output.putInt("Tier", this.tier);
        output.putInt("Radius", this.radius);
        output.putInt("Cursor", this.cursor);
        output.store("Fuel", ItemStack.OPTIONAL_CODEC, this.items.get(FUEL_SLOT));
        output.store("Upgrade", ItemStack.OPTIONAL_CODEC, this.items.get(UPGRADE_SLOT));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.deserialize(input.childOrEmpty("Energy"));
        this.burnTime = input.getIntOr("BurnTime", 0);
        this.maxBurnTime = input.getIntOr("MaxBurnTime", 0);
        this.tier = Math.max(1, input.getIntOr("Tier", 1));
        this.radius = input.getIntOr("Radius", 0);
        this.cursor = input.getIntOr("Cursor", 0);
        this.ringFor = -1;
        this.items.set(FUEL_SLOT, input.read("Fuel", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.items.set(UPGRADE_SLOT, input.read("Upgrade", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
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
        return this.items.get(FUEL_SLOT).isEmpty() && this.items.get(UPGRADE_SLOT).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(this.items, slot, amount);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = ContainerHelper.takeItem(this.items, slot);
        setChanged();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stack.limitSize(Math.min(this.getMaxStackSize(), stack.getMaxStackSize()));
        this.items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == FUEL_SLOT) {
            return OxygenGeneratorBlockEntity.fuelValue(stack) > 0;
        }
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
        this.items.clear();
        setChanged();
    }

    private final class TerraformerEnergy extends SimpleEnergyHandler {
        private TerraformerEnergy() {
            super(ENERGY_CAPACITY, ENERGY_MAX_INSERT, 0);
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            TerraformerBlockEntity.this.setChanged();
        }

        void generate(int amount) {
            int current = getAmountAsInt();
            int next = Math.min(getCapacityAsInt(), current + amount);
            if (next != current) {
                set(next);
            }
        }

        void consume(int amount) {
            set(Math.max(0, getAmountAsInt() - amount));
        }
    }
}
