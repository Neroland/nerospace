package za.co.neroland.nerospace.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.menu.TerraformMonitorMenu;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.world.TerraformManager;

/**
 * Terraform Monitor (DEEPER_TERRAFORM_DESIGN.md §6): the readout block. Finds the nearest registered
 * Terraformer within {@link #LINK_RANGE} via {@link TerraformManager} (cheap — the SavedData already
 * indexes every machine), shows its stage radii / hydration / stall reason, and reports the LOCAL
 * column's effective stage on a comparator (0/5/10/15) so ranches can automate on "the land turned
 * Living".
 */
public class TerraformMonitorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int DATA_COUNT = 7;
    /** How far (blocks, horizontal) the Monitor searches for a Terraformer to read. */
    public static final int LINK_RANGE = 32;
    /** Ticks between readout refreshes (display only — nothing gameplay-critical). */
    private static final int REFRESH_INTERVAL_TICKS = 20;

    private transient boolean linked;
    private transient int rootedRadius;
    private transient int hydrationRadius;
    private transient int lifeRadius;
    private transient int hydration;
    private transient boolean stalled;
    private transient int localStage;

    /**
     * Synced to the menu: [0]=linked [1]=rootedRadius [2]=hydrationRadius [3]=lifeRadius
     * [4]=hydration [5]=stalled [6]=localStage.
     */
    private final @org.jspecify.annotations.NonNull ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> linked ? 1 : 0;
                case 1 -> rootedRadius;
                case 2 -> hydrationRadius;
                case 3 -> lifeRadius;
                case 4 -> hydration;
                case 5 -> stalled ? 1 : 0;
                case 6 -> localStage;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> linked = value != 0;
                case 1 -> rootedRadius = value;
                case 2 -> hydrationRadius = value;
                case 3 -> lifeRadius = value;
                case 4 -> hydration = value;
                case 5 -> stalled = value != 0;
                case 6 -> localStage = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public TerraformMonitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TERRAFORM_MONITOR.get(), pos, state);
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    /** Comparator: the LOCAL column's effective stage, scaled to redstone (0/5/10/15). */
    public int comparatorSignal() {
        return this.localStage * 5;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)
                || serverLevel.getGameTime() % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }
        refresh(serverLevel, pos);
    }

    /** One readout refresh (public so the gametest can drive it without waiting on the interval). */
    public void refresh(ServerLevel level, BlockPos pos) {
        int oldStage = this.localStage;
        BlockPos checkedPos = NerospaceCommon.requireNonNull(pos);
        this.localStage = TerraformConversion.effectiveStage(level.getChunkAt(checkedPos));

        BlockPos nearest = nearestTerraformer(level, pos);
        this.linked = nearest != null;
        if (nearest != null) {
            TerraformManager manager = TerraformManager.get(level);
            this.rootedRadius = manager.stageRadius(nearest, 1);
            this.hydrationRadius = manager.stageRadius(nearest, 2);
            this.lifeRadius = manager.stageRadius(nearest, 3);
            // Live hydration/stall come from the machine itself when its chunk is loaded.
            if (level.isLoaded(nearest)
                    && level.getBlockEntity(nearest) instanceof TerraformerBlockEntity terraformer) {
                this.hydration = terraformer.getHydration();
                this.stalled = terraformer.getDataAccess().get(8) != 0;
            } else {
                this.hydration = 0;
                this.stalled = false;
            }
        } else {
            this.rootedRadius = this.hydrationRadius = this.lifeRadius = this.hydration = 0;
            this.stalled = false;
        }

        if (oldStage != this.localStage) {
            level.updateNeighbourForOutputSignal(checkedPos, getBlockState().getBlock());
        }
    }

    @Nullable
    private BlockPos nearestTerraformer(ServerLevel level, BlockPos pos) {
        BlockPos[] best = {null};
        long[] bestDist = {(long) LINK_RANGE * LINK_RANGE};
        TerraformManager.get(level).forEachMachine((center, r1, r2, r3) -> {
            long dx = center.getX() - pos.getX();
            long dz = center.getZ() - pos.getZ();
            long d = dx * dx + dz * dz;
            if (d <= bestDist[0]) {
                bestDist[0] = d;
                best[0] = center;
            }
        });
        return best[0];
    }

    // --- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerospace.terraform_monitor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @org.jspecify.annotations.NonNull Inventory playerInventory, Player player) {
        return new TerraformMonitorMenu(containerId, playerInventory, this, this.dataAccess);
    }

    /** Menu range check (no Container interface — the Monitor has no inventory). */
    public boolean stillValid(Player player) {
        Level currentLevel = this.level;
        if (currentLevel == null || currentLevel.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0;
    }
}
