package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
        super(ModBlockEntities.STATION_CORE.get(), pos, state);
    }

    /** Binds this core to its founded station (called by the founding flow / tests). */
    public void bindStation(int slot, String name) {
        this.slot = slot;
        this.stationName = name;
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

    /** Server tick: every {@link za.co.neroland.nerospace.world.station.ObservatoryRules#RECHECK_TICKS} ticks
     * records a 1.x station's {@code CORE_LEGACY} module on its first 2.0 load and keeps the Observatory's
     * telescope in step with the station class. Cheap: one registry lookup per interval. */
    public void serverTick(net.minecraft.world.level.Level level) {
        if (!(level instanceof ServerLevel server) || !isBound()
                || server.getGameTime() % za.co.neroland.nerospace.world.station.ObservatoryRules.RECHECK_TICKS
                        != (this.worldPosition.asLong() & 0x7F)) {
            return;
        }
        StationRegistry registry = StationRegistry.get(server.getServer());
        StationRegistry.StationEntry entry = registry.get(this.slot);
        if (entry == null || !entry.center().equals(this.worldPosition)) {
            return;
        }
        za.co.neroland.nerospace.world.station.StationModules.ensureLegacyCore(server, registry, entry);
        za.co.neroland.nerospace.world.station.StationObservatory.syncTelescope(server, entry);
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
        if (removed != null) {
            // Abandonment removes every module's oxygen source; the hull stays as scrap.
            za.co.neroland.nerospace.menu.StationConsoleMenu.clearOxygen(serverLevel, removed);
        }
        ItemStack charter = new ItemStack(ModItems.STATION_CHARTER.get());
        String name = removed != null ? removed.name() : this.stationName;
        if (name != null && !name.isBlank()) {
            charter.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }
        Containers.dropItemStack(serverLevel,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, charter);
        this.slot = -1;
    }

    // --- Persistence ---------------------------------------------------------------------------

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
        output.putInt("Slot", this.slot);
        output.putString("StationName", this.stationName);
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
        this.slot = input.getIntOr("Slot", -1);
        this.stationName = input.getStringOr("StationName", "");
    }
}
