package za.co.neroland.nerospace.suit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import za.co.neroland.nerolandcore.energy.EnergyBuffer;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideConfigured;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.machine.MachineSideConfig;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/** Charges the worn chest of each player standing on the pad, from a Core energy input. */
public class SuitDockBlockEntity extends BlockEntity implements SideConfigured {

    private final EnergyBuffer energy = new EnergyBuffer(50_000, 50_000, 0, this::setChanged);
    private final SideConfigComponent sideConfig = new SideConfigComponent(SideConfig.builder()
            .channel(Channel.ENERGY)
            .defaultPreset(SidePreset.ALL_INPUT)
            .build(), this).withEnergy(this::energy);

    public SuitDockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUIT_DOCK.get(), pos, state);
    }

    public EnergyBuffer energy() {
        return this.energy;
    }

    public void serverTick() {
        if (!(this.level instanceof ServerLevel level)) {
            return;
        }
        this.sideConfig.serverTick(level, this.worldPosition, MachineSideConfig.TRANSFER_RATE);
        AABB box = new AABB(this.worldPosition).inflate(0.5, 1.0, 0.5);
        int rate = NerospaceConfig.suitDockChargeRate();
        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            if (this.energy.getRaw() <= 0) {
                break;
            }
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!(chest.getItem() instanceof SuitPieceItem piece) || piece.piece() != SuitPiece.CHEST) {
                continue;
            }
            int budget = Math.min(rate, this.energy.getRaw());
            long moved = new SuitEnergy.View(chest).insert(budget, false);
            if (moved > 0) {
                this.energy.consume((int) moved);
            }
        }
    }

    @Override
    public SideConfigComponent sideConfig() {
        return this.sideConfig;
    }

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
        output.putInt("Energy", this.energy.getRaw());
        this.sideConfig.save(output);
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
        this.energy.setRaw(input.getIntOr("Energy", 0));
        this.sideConfig.load(input);
    }
}
