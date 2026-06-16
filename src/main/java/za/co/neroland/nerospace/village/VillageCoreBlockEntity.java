package za.co.neroland.nerospace.village;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Backing data for the {@link VillageCoreBlock}. Phase 3: records the claiming player (UUID + name
 * for display). Later phases extend this into the per-village reputation map, plot registry and
 * construction queue (ALIEN_VILLAGERS_DESIGN.md §4.1).
 */
public class VillageCoreBlockEntity extends BlockEntity {

    private UUID owner;
    private String ownerName = "";

    public VillageCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VILLAGE_CORE.get(), pos, state);
    }

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

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Owner", this.owner == null ? "" : this.owner.toString());
        output.putString("OwnerName", this.ownerName);
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
    }
}
