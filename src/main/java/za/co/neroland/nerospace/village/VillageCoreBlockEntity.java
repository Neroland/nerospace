package za.co.neroland.nerospace.village;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import za.co.neroland.nerospace.entity.AlienVillager;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.village.VillageBuildings.Placement;
import za.co.neroland.nerospace.village.VillageBuildings.Type;

/**
 * Village Core controller (ALIEN_VILLAGERS_DESIGN.md §4). Phase 3 made it claimable; Phase 4 makes it
 * the teach-and-grow engine: it holds a nerosteel stockpile, and when the owner asks it to build the
 * next catalogue building — gated by their reputation with the nearby villagers and by stockpiled
 * materials — it raises that building block-by-block over real time.
 */
public class VillageCoreBlockEntity extends BlockEntity {

    private static final int BUILD_INTERVAL = 5;   // server ticks between placed blocks
    private static final double SCAN_RADIUS = 32.0; // villagers within this drive village reputation
    /** Plot offsets (dx,dz) from the core for successive buildings. */
    private static final int[][] PLOT_OFFSETS = {{8, 0}, {-8, 0}, {0, 8}, {0, -8}, {8, 8}, {-8, -8}};

    private UUID owner;
    private String ownerName = "";

    private int stockpile;
    private int builtCount;

    // Active build job (null type = idle).
    private Type jobType;
    private int progress;
    private int plotX;
    private int plotY;
    private int plotZ;
    private int buildTick;
    private transient List<Placement> jobPlacements;

    public VillageCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VILLAGE_CORE.get(), pos, state);
    }

    // --- Claiming (Phase 3) ---------------------------------------------------

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
        setChanged();
    }

    // --- Teach-and-grow (Phase 4) ---------------------------------------------

    /** Right-click with nerosteel: feed the construction stockpile. */
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

    /** Bare right-click by the owner: report progress, or teach + start the next building. */
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

    /** Server tick (driven by the block's ticker): advance an active build job, one block at a time. */
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (this.jobType == null) {
            return;
        }
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

    /** The player's standing with this village = the highest reputation tier among nearby villagers. */
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

    private static String label(Type t) {
        return switch (t) {
            case HUT -> "Hut";
            case WORKSHOP -> "Workshop";
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
        this.jobPlacements = null;
    }
}
