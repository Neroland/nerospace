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

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.entity.AlienVillager;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.village.VillageBuildings.Placement;
import za.co.neroland.nerospace.village.VillageBuildings.Quest;
import za.co.neroland.nerospace.village.VillageBuildings.Type;

/**
 * Village Core controller (ALIEN_VILLAGERS_DESIGN.md §4). Claimable (Phase 3); a teach-and-grow
 * engine (Phase 4); and — Phase 5 — a functional hub: completed buildings produce goods the owner
 * collects, the village posts fetch quests for reputation, and (config-gated) hostile raids test the
 * settlement at night.
 */
public class VillageCoreBlockEntity extends BlockEntity {

    private static final int BUILD_INTERVAL = 5;
    private static final int PRODUCE_INTERVAL = 1200;  // ~1 min between yields
    private static final int RAID_INTERVAL = 2400;     // ~2 min between raid checks
    private static final double SCAN_RADIUS = 32.0;
    private static final double RAID_RANGE = 48.0;
    private static final int OUTPUT_CAP = 64;
    private static final int[][] PLOT_OFFSETS = {{8, 0}, {-8, 0}, {0, 8}, {0, -8}, {8, 8}, {-8, -8}};

    private UUID owner;
    private String ownerName = "";

    private int stockpile;
    private int builtCount;

    private Type jobType;
    private int progress;
    private int plotX;
    private int plotY;
    private int plotZ;
    private int buildTick;
    private transient List<Placement> jobPlacements;

    // Phase 5 — production output, quest, timers.
    private int outBread;
    private int outIngot;
    private int produceTick;
    private int raidTick;
    private int questOrdinal = -1;

    public VillageCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VILLAGE_CORE.get(), pos, state);
    }

    // --- Claiming -------------------------------------------------------------

    public boolean isClaimed() {
        return this.owner != null;
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(this.owner);
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public void claim(Player player) {
        this.owner = player.getUUID();
        this.ownerName = player.getName().getString();
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
        player.sendSystemMessage(Component.literal("Stockpile: " + this.stockpile + " Nerosteel."));
        setChanged();
    }

    public void onUse(Player player) {
        if (this.jobType != null) {
            int total = placements().size();
            int pct = total == 0 ? 100 : (int) (100.0 * this.progress / total);
            player.sendSystemMessage(Component.literal("Constructing " + label(this.jobType) + "… " + pct + "%"));
            return;
        }
        Type next = Type.byOrdinalOrNull(this.builtCount);
        if (next == null) {
            player.sendSystemMessage(Component.literal("The village is fully built. The aliens are grateful."));
            return;
        }
        int tier = villageTier(player);
        if (tier < next.reqTier) {
            player.sendSystemMessage(Component.literal(
                    "The villagers won't follow your plans yet — reach trust tier " + next.reqTier
                            + " (you are tier " + tier + ")."));
            return;
        }
        if (this.stockpile < next.cost) {
            player.sendSystemMessage(Component.literal(
                    "Teaching the " + label(next) + " needs " + next.cost + " Nerosteel in the stockpile (have "
                            + this.stockpile + ")."));
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
        player.sendSystemMessage(Component.literal("Construction begins: " + label(next) + "."));
        setChanged();
    }

    // --- Phase 5: production + quests + raids ---------------------------------

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
            player.sendSystemMessage(Component.literal("Collected " + bread + " bread, " + ingot + " nerosteel."));
        }
        Quest q = Quest.byOrdinalOrNull(this.questOrdinal);
        if (q != null) {
            player.sendSystemMessage(Component.literal(
                    "Village task: bring " + q.count + "x " + itemLabel(q) + " for " + q.reward + " emeralds + trust."));
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
        player.sendSystemMessage(Component.literal("The villagers thank you. (+" + q.reward + " trust)"));
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
        if (!Config.ALIEN_RAIDS_ENABLED.get() || !(level instanceof ServerLevel server)) {
            return;
        }
        if (level.isBrightOutside()) {
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
            player.drop(stack, false);
        }
    }

    private static String label(Type t) {
        return switch (t) {
            case HUT -> "Hut";
            case WORKSHOP -> "Workshop";
        };
    }

    private static String itemLabel(Quest q) {
        return switch (q) {
            case XERTZ_QUARTZ -> "Xertz Quartz";
            case RAW_NEROSTEEL -> "Raw Nerosteel";
            case ALIEN_FRAGMENT -> "Alien Fragment";
        };
    }

    // --- Persistence ----------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Owner", this.owner == null ? "" : this.owner.toString());
        output.putString("OwnerName", this.ownerName);
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

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        String stored = input.getStringOr("Owner", "");
        if (stored.isEmpty()) {
            this.owner = null;
        } else {
            try {
                this.owner = UUID.fromString(stored);
            } catch (IllegalArgumentException ex) {
                this.owner = null;
            }
        }
        this.ownerName = input.getStringOr("OwnerName", "");
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
