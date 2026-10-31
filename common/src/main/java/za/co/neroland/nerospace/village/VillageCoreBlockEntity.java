package za.co.neroland.nerospace.village;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.entity.AlienVillager;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.village.VillageBuildings.Placement;
import za.co.neroland.nerospace.village.VillageBuildings.Quest;
import za.co.neroland.nerospace.village.VillageBuildings.Type;

/**
 * Village Core controller (ALIEN_VILLAGERS_DESIGN.md §4). Claimable; a teach-and-grow engine (feed
 * Nerosteel, then teach the village to raise the next building); and a functional hub — completed
 * buildings produce goods the owner collects, the village posts fetch quests for reputation, and
 * (config-gated) hostile raids test the settlement at night.
 *
 * <p>Cross-loader port: the root's interactive controller, ported verbatim onto the shared
 * persistence/entity APIs. Two adaptations for cross-version safety: the raid gate reads
 * {@link NerospaceConfig#alienRaidsEnabled()} (the properties config seam, not the NeoForge
 * {@code ModConfigSpec}), and the after-dark check uses the long-standing vanilla
 * {@link Level#getSkyDarken()} instead of {@code isBrightOutside()} (the de-obf day/night helpers
 * diverge 26.1.2↔26.2).</p>
 */
public class VillageCoreBlockEntity extends BlockEntity {

    private static final int BUILD_INTERVAL = 5;
    private static final int PRODUCE_INTERVAL = 1200;  // ~1 min between yields
    private static final int RAID_INTERVAL = 2400;     // ~2 min between raid checks
    private static final double SCAN_RADIUS = 32.0;
    private static final double RAID_RANGE = 48.0;
    private static final int OUTPUT_CAP = 64;
    /** getSkyDarken() ≥ this ⇒ night/dusk (0 = noon, ~11 = full dark); raids only fire after dark. */
    private static final int DARK_THRESHOLD = 4;
    private static final int[][] PLOT_OFFSETS = {{8, 0}, {-8, 0}, {0, 8}, {0, -8}, {8, 8}, {-8, -8}};

    /** Owner UUID only; the 1.6.0 display name is no longer stored (see {@link VillageCoreOwnership}). */
    private final VillageCoreOwnership ownership = new VillageCoreOwnership();

    private int stockpile;
    private int builtCount;

    private Type jobType;
    private int progress;
    private int plotX;
    private int plotY;
    private int plotZ;
    private int buildTick;
    private transient List<Placement> jobPlacements;

    // Production output, quest, timers.
    private int outBread;
    private int outIngot;
    private int produceTick;
    private int raidTick;
    private int questOrdinal = -1;

    /**
     * Server-side cores currently in a loaded chunk, for the erasure sweep. Weak, runtime-only, never persisted;
     * {@link #setRemoved()} only unregisters (no world access there — it also fires on chunk unload).
     */
    private static final java.util.Set<VillageCoreBlockEntity> LOADED =
            java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>()));

    public VillageCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VILLAGE_CORE.get(), pos, state);
    }

    /**
     * Cores that have just joined a server level and still need their {@link VillageCoreLedger} check (a pending
     * erasure applied, the index row refreshed). Drained on the server thread by {@link #syncLedger}, never inside
     * {@code setLevel} (that runs mid chunk load).
     */
    private static final java.util.Queue<VillageCoreBlockEntity> AWAITING_LEDGER =
            new java.util.concurrent.ConcurrentLinkedQueue<>();

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel) {
            LOADED.add(this);
            AWAITING_LEDGER.add(this);
        }
    }

    @Override
    public void setRemoved() {
        LOADED.remove(this);
        super.setRemoved();
    }

    /** The block was broken (not a chunk unload): its ledger row and any pending clear go with it. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level instanceof ServerLevel server) {
            VillageCoreLedger.get(server.getServer()).remove(loc(server, pos));
        }
    }

    static VillageCoreLedger.CoreLoc loc(Level level, BlockPos pos) {
        return new VillageCoreLedger.CoreLoc(level.dimension().identifier().toString(), pos.asLong());
    }

    /**
     * Server tick (and the start of an erasure or export): every core that joined a level since the last call is
     * checked against the {@link VillageCoreLedger} — a pending erasure for its position clears the owner and is
     * dropped, and the owner index row is refreshed.
     */
    public static void syncLedger(net.minecraft.server.MinecraftServer server) {
        if (AWAITING_LEDGER.isEmpty() || server == null) {
            return;
        }
        VillageCoreLedger ledger = VillageCoreLedger.get(server);
        long today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toEpochDay();
        int cleared = 0;
        VillageCoreBlockEntity core;
        while ((core = AWAITING_LEDGER.poll()) != null) {
            if (core.isRemoved() || !(core.level instanceof ServerLevel lvl) || lvl.getServer() != server) {
                continue;
            }
            if (ledger.onLoaded(loc(lvl, core.worldPosition), core.ownership.owner(), today)) {
                core.ownership.clear();
                core.setChanged();
                cleared++;
            }
        }
        if (cleared > 0) {
            // Count only (POPIA/GDPR).
            NerospaceCommon.LOGGER.info("[Nerospace] Erasure: applied {} pending village core owner clear(s).", cleared);
        }
    }

    /** Locations of every loaded, live core (erasure: which ledger rows the live sweep already reached). */
    public static java.util.Set<VillageCoreLedger.CoreLoc> loadedLocations() {
        java.util.Set<VillageCoreLedger.CoreLoc> out = new java.util.HashSet<>();
        synchronized (LOADED) {
            for (VillageCoreBlockEntity core : LOADED) {
                if (!core.isRemoved() && core.level != null) {
                    out.add(loc(core.level, core.worldPosition));
                }
            }
        }
        return out;
    }

    /** Privacy export: locations of the loaded cores {@code playerId} owns. */
    public static List<VillageCoreLedger.CoreLoc> loadedOwnedBy(UUID playerId) {
        List<VillageCoreLedger.CoreLoc> out = new java.util.ArrayList<>();
        synchronized (LOADED) {
            for (VillageCoreBlockEntity core : LOADED) {
                if (!core.isRemoved() && core.level != null && core.ownership.isOwner(playerId)) {
                    out.add(loc(core.level, core.worldPosition));
                }
            }
        }
        return out;
    }

    /**
     * POPIA/GDPR erasure: clears {@code playerId} as owner of every loaded core.
     *
     * @return how many cores were owned by the player
     */
    public static int forgetOwnerEverywhere(UUID playerId) {
        List<VillageCoreBlockEntity> snapshot;
        synchronized (LOADED) {
            snapshot = new java.util.ArrayList<>(LOADED);
        }
        int cleared = 0;
        for (VillageCoreBlockEntity core : snapshot) {
            if (!core.isRemoved() && core.forgetOwner(playerId)) {
                cleared++;
            }
        }
        return cleared;
    }

    // --- Claiming -------------------------------------------------------------

    public boolean isClaimed() {
        return this.ownership.isClaimed();
    }

    public boolean isOwner(Player player) {
        return this.ownership.isOwner(player.getUUID());
    }

    /** Whether {@code playerId} owns this core (erasure sweep). */
    public boolean isOwnedBy(UUID playerId) {
        return this.ownership.isOwner(playerId);
    }

    /**
     * The owner's name for a chat line, resolved now and never stored: the live name of an online owner, otherwise
     * "another player".
     */
    public Component ownerDisplay(net.minecraft.server.MinecraftServer server) {
        UUID owner = this.ownership.owner();
        Player online = owner == null || server == null ? null : server.getPlayerList().getPlayer(owner);
        return online != null ? online.getName()
                : Component.translatable("message.nerospace.village_core.owned_by_offline");
    }

    /**
     * POPIA/GDPR: clears the owner if it is {@code playerId}; the core keeps its stockpile, buildings and quest.
     *
     * @return whether this core was owned by the player
     */
    public boolean forgetOwner(UUID playerId) {
        if (this.ownership.forget(playerId)) {
            setChanged();
            return true;
        }
        return false;
    }

    public void claim(Player player) {
        this.ownership.claim(player.getUUID());
        if (this.level instanceof ServerLevel server) {
            VillageCoreLedger.get(server.getServer()).record(loc(server, this.worldPosition), player.getUUID());
        }
        if (this.questOrdinal < 0) {
            rollQuest();
        }
        setChanged();
    }

    // --- Teach-and-grow -------------------------------------------------------

    public void deposit(Player player, ItemStack stack) {
        int add = stack.getCount();
        if (add <= 0) {
            return;
        }
        stack.shrink(add);
        this.stockpile += add;
        player.sendSystemMessage(Component.translatable("message.nerospace.village_core.stockpile", this.stockpile));
        setChanged();
    }

    public void onUse(Player player) {
        if (this.jobType != null) {
            int total = placements().size();
            int pct = total == 0 ? 100 : (int) (100.0 * this.progress / total);
            player.sendSystemMessage(Component.translatable("message.nerospace.village_core.constructing", label(this.jobType), pct));
            return;
        }
        Type next = Type.byOrdinalOrNull(this.builtCount);
        if (next == null) {
            player.sendSystemMessage(Component.translatable("message.nerospace.village_core.complete"));
            return;
        }
        int tier = villageTier(player);
        if (tier < next.reqTier) {
            player.sendSystemMessage(Component.translatable(
                    "message.nerospace.village_core.tier_locked", next.reqTier, tier));
            return;
        }
        if (this.stockpile < next.cost) {
            player.sendSystemMessage(Component.translatable(
                    "message.nerospace.village_core.needs_stockpile", label(next), next.cost, this.stockpile));
            return;
        }
        this.stockpile -= next.cost;
        int[] off = PLOT_OFFSETS[Math.min(this.builtCount, PLOT_OFFSETS.length - 1)];
        this.plotX = this.worldPosition.getX() + off[0];
        this.plotZ = this.worldPosition.getZ() + off[1];
        this.plotY = this.level != null
                ? this.level.getHeight(Heightmap.Types.WORLD_SURFACE, this.plotX, this.plotZ)
                : this.worldPosition.getY();
        this.jobType = next;
        this.progress = 0;
        this.buildTick = 0;
        this.jobPlacements = VillageBuildings.build(next);
        player.sendSystemMessage(Component.translatable("message.nerospace.village_core.construction_begins", label(next)));
        setChanged();
    }

    // --- Production + quests + raids ------------------------------------------

    /** Sneak-right-click: collect produced goods and read the current quest. */
    public void collectAndStatus(Player player) {
        int bread = this.outBread;
        int ingot = this.outIngot;
        if (bread > 0) {
            give(player, new ItemStack(Items.BREAD, bread));
        }
        if (ingot > 0) {
            give(player, new ItemStack(ModBlocks.NEROSTEEL_BLOCK.get().asItem(), ingot));
        }
        this.outBread = 0;
        this.outIngot = 0;
        if (bread > 0 || ingot > 0) {
            player.sendSystemMessage(Component.translatable("message.nerospace.village_core.collected", bread, ingot));
        }
        Quest q = Quest.byOrdinalOrNull(this.questOrdinal);
        if (q != null) {
            player.sendSystemMessage(Component.translatable(
                    "message.nerospace.village_core.task", q.count, new ItemStack(q.item()).getHoverName(), q.reward));
        }
        setChanged();
    }

    /** Right-click with the quest item: hand it in for the reward. */
    public boolean tryCompleteQuest(Player player, ItemStack stack) {
        Quest q = Quest.byOrdinalOrNull(this.questOrdinal);
        if (q == null || !stack.is(q.item()) || stack.getCount() < q.count) {
            return false;
        }
        stack.shrink(q.count);
        give(player, new ItemStack(q.rewardItem(), q.reward));
        grantVillageReputation(player, q.reward);
        player.sendSystemMessage(Component.translatable("message.nerospace.village_core.thanks", q.reward));
        rollQuest();
        setChanged();
        return true;
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        // Construction.
        if (this.jobType != null) {
            tickConstruction(level);
        }
        if (!this.isClaimed()) {
            return;
        }
        // Passive production from completed buildings.
        if (++this.produceTick >= PRODUCE_INTERVAL) {
            this.produceTick = 0;
            if (this.builtCount >= 1) {
                this.outBread = Math.min(OUTPUT_CAP, this.outBread + 1);   // Hut → food
            }
            if (this.builtCount >= 2) {
                this.outIngot = Math.min(OUTPUT_CAP, this.outIngot + 1);   // Workshop → nerosteel
            }
            setChanged();
        }
        // Config-gated night raids.
        if (++this.raidTick >= RAID_INTERVAL) {
            this.raidTick = 0;
            maybeRaid(level, pos);
        }
    }

    private void tickConstruction(Level level) {
        List<Placement> list = placements();
        if (this.progress >= list.size()) {
            finishJob();
            return;
        }
        if (++this.buildTick < BUILD_INTERVAL) {
            return;
        }
        this.buildTick = 0;
        Placement p = list.get(this.progress++);
        BlockPos bp = new BlockPos(this.plotX + p.dx(), this.plotY + p.dy(), this.plotZ + p.dz());
        BlockState bs = switch (p.kind()) {
            case WALL, ROOF -> ModBlocks.NEROSTEEL_BLOCK.get().defaultBlockState();
            case LIGHT -> Blocks.GLOWSTONE.defaultBlockState();
            case AIR -> Blocks.AIR.defaultBlockState();
        };
        level.setBlock(bp, bs, 3);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.0);
        }
        setChanged();
        if (this.progress >= list.size()) {
            finishJob();
        }
    }

    private void maybeRaid(Level level, BlockPos pos) {
        if (!NerospaceConfig.alienRaidsEnabled() || !(level instanceof ServerLevel server)) {
            return;
        }
        if (level.getSkyDarken() < DARK_THRESHOLD) {
            return; // raids only after dark
        }
        Player near = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), RAID_RANGE, false);
        if (near == null) {
            return;
        }
        RandomSource rand = level.getRandom();
        int waves = 1 + rand.nextInt(2);
        for (int i = 0; i < waves; i++) {
            int ox = pos.getX() + (rand.nextBoolean() ? 1 : -1) * (10 + rand.nextInt(8));
            int oz = pos.getZ() + (rand.nextBoolean() ? 1 : -1) * (10 + rand.nextInt(8));
            int oy = level.getHeight(Heightmap.Types.WORLD_SURFACE, ox, oz);
            ModEntities.XERTZ_STALKER.get().spawn(server, new BlockPos(ox, oy, oz), EntitySpawnReason.EVENT);
        }
    }

    private void finishJob() {
        this.builtCount++;
        this.jobType = null;
        this.jobPlacements = null;
        this.progress = 0;
        setChanged();
    }

    private List<Placement> placements() {
        if (this.jobPlacements == null && this.jobType != null) {
            this.jobPlacements = VillageBuildings.build(this.jobType);
        }
        return this.jobPlacements == null ? List.of() : this.jobPlacements;
    }

    private int villageTier(Player player) {
        if (this.level == null) {
            return 0;
        }
        AABB box = new AABB(this.worldPosition).inflate(SCAN_RADIUS);
        int max = 0;
        for (AlienVillager v : this.level.getEntitiesOfClass(AlienVillager.class, box)) {
            max = Math.max(max, v.getTier(player));
        }
        return max;
    }

    private void grantVillageReputation(Player player, int amount) {
        if (this.level == null) {
            return;
        }
        AABB box = new AABB(this.worldPosition).inflate(SCAN_RADIUS);
        for (AlienVillager v : this.level.getEntitiesOfClass(AlienVillager.class, box)) {
            v.addReputation(player, amount);
        }
    }

    private void rollQuest() {
        RandomSource rand = this.level != null ? this.level.getRandom() : RandomSource.create();
        this.questOrdinal = rand.nextInt(Quest.values().length);
    }

    private void give(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            //? if >=26.3 {
            /*player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
            *///?} else {
            player.drop(stack, false);
            //?}
        }
    }

    private static Component label(Type t) {
        return Component.translatable(switch (t) {
            case HUT -> "message.nerospace.village_core.building.hut";
            case WORKSHOP -> "message.nerospace.village_core.building.workshop";
        });
    }

    // --- Persistence ----------------------------------------------------------

    //? if >=1.21.6 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
    //?} else {
    /*@Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput output = ValueOutput.of(tag, registries);
    *///?}
        // Owner UUID only. The 1.6.0 "OwnerName" key is never written, so it drops out at the next chunk save.
        this.ownership.write(output::putString);
        output.putInt("Stockpile", this.stockpile);
        output.putInt("BuiltCount", this.builtCount);
        output.putInt("JobType", this.jobType == null ? -1 : this.jobType.ordinal());
        output.putInt("Progress", this.progress);
        output.putInt("PlotX", this.plotX);
        output.putInt("PlotY", this.plotY);
        output.putInt("PlotZ", this.plotZ);
        output.putInt("OutBread", this.outBread);
        output.putInt("OutIngot", this.outIngot);
        output.putInt("Quest", this.questOrdinal);
    }

    //? if >=1.21.6 {
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
    //?} else {
    /*@Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput input = ValueInput.of(tag, registries);
    *///?}
        // "OwnerName" (1.6.0) is ignored. An owner erased earlier this session is dropped on load (POPIA/GDPR).
        this.ownership.read(key -> input.getStringOr(key, ""),
                za.co.neroland.nerospace.data.NerospaceErasure::erasedThisSession);
        this.stockpile = input.getIntOr("Stockpile", 0);
        this.builtCount = input.getIntOr("BuiltCount", 0);
        this.jobType = Type.byOrdinalOrNull(input.getIntOr("JobType", -1));
        this.progress = input.getIntOr("Progress", 0);
        this.plotX = input.getIntOr("PlotX", 0);
        this.plotY = input.getIntOr("PlotY", 0);
        this.plotZ = input.getIntOr("PlotZ", 0);
        this.outBread = input.getIntOr("OutBread", 0);
        this.outIngot = input.getIntOr("OutIngot", 0);
        this.questOrdinal = input.getIntOr("Quest", -1);
        this.jobPlacements = null;
    }
}
