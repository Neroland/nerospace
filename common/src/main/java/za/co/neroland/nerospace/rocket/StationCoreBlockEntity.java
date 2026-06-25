package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * The Station Core — the anchor placed at a founded station's centre. Holds the station's slot id +
 * name; breaking it unregisters the station and pops a charter named after it (re-foundable
 * elsewhere). Only obtainable by founding — there is no crafting recipe and the block has no loot
 * table.
 *
 * <p>Cross-loader port: vanilla value-IO + {@code Containers}/{@code DataComponents}; identical to the
 * standalone mod.</p>
 */
public class StationCoreBlockEntity extends BlockEntity {

    /** −1 until {@link #bindStation} — a core placed outside the founding flow anchors nothing. */
    private int slot = -1;
    private String stationName = "";

    public StationCoreBlockEntity(BlockPos pos, BlockState state) {
        super(java.util.Objects.requireNonNull(ModBlockEntities.STATION_CORE.get()), pos, state);
    }

    /** Binds this core to its founded station (called by the founding flow / tests). */
    public void bindStation(int slot, String name) {
        this.slot = slot;
        this.stationName = java.util.Objects.requireNonNull(name);
        setChanged();
    }

    public int stationSlot() {
        return this.slot;
    }

    public String stationName() {
        return this.stationName;
    }

    public boolean isBound() {
        return this.slot >= 0;
    }

    public int comparatorSignal() {
        return isBound() ? 15 : 0;
    }

    /**
     * Breaking the core unregisters its station and pops a charter named after it. The platform simply
     * remains as scrap in the void; the slot is never reused (see {@link StationRegistry}).
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (!(this.level instanceof ServerLevel serverLevel) || !isBound()) {
            return;
        }
        StationRegistry.StationEntry removed =
                StationRegistry.get(serverLevel.getServer()).unregister(this.slot);
        ItemStack charter = new ItemStack(java.util.Objects.requireNonNull(ModItems.STATION_CHARTER.get()));
        String name = removed != null ? removed.name() : this.stationName;
        if (name != null && !name.isBlank()) {
            setComponent(charter, DataComponents.CUSTOM_NAME, Component.literal(name));
        }
        Containers.dropItemStack(serverLevel,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, charter);
        this.slot = -1;
    }

    private static <T extends @Nullable Object> void setComponent(
            ItemStack stack, DataComponentType<T> type, T value) {
        stack.set(java.util.Objects.requireNonNull(type), value);
    }

    // --- Persistence ---------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Slot", this.slot);
        output.putString("StationName", java.util.Objects.requireNonNull(this.stationName));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.slot = input.getIntOr("Slot", -1);
        this.stationName = input.getStringOr("StationName", "");
    }
}
