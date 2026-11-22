package za.co.neroland.nerospace.suit;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerolandcore.energy.EnergyBuffer;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideConfigured;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.machine.MachineSideConfig;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/** Energy input on every face. The menu edits the piece in its own slots; this block only holds the buffer. */
public class SuitFabricatorBlockEntity extends BlockEntity implements SideConfigured,
        net.minecraft.world.MenuProvider {

    private final EnergyBuffer energy = new EnergyBuffer(100_000, 100_000, 0, this::setChanged);
    private final SideConfigComponent sideConfig = new SideConfigComponent(SideConfig.builder()
            .channel(Channel.ENERGY)
            .defaultPreset(SidePreset.ALL_INPUT)
            .build(), this).withEnergy(this::energy);

    public SuitFabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUIT_FABRICATOR.get(), pos, state);
    }

    public EnergyBuffer energy() {
        return this.energy;
    }

    public void serverTick() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        this.sideConfig.serverTick(this.level, this.worldPosition, MachineSideConfig.TRANSFER_RATE);
    }

    /** Move up to the fabricator rate into a chest piece. Returns the FE moved. */
    public int charge(ItemStack chest) {
        if (!(chest.getItem() instanceof SuitPieceItem piece) || piece.piece() != SuitPiece.CHEST) {
            return 0;
        }
        int budget = Math.min(NerospaceConfig.suitFabricatorChargeRate(), this.energy.getRaw());
        if (budget <= 0) {
            return 0;
        }
        long moved = new SuitEnergy.View(chest).insert(budget, false);
        if (moved > 0) {
            this.energy.consume((int) moved);
        }
        return (int) moved;
    }

    @Override
    public SideConfigComponent sideConfig() {
        return this.sideConfig;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nerospace.suit_fabricator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SuitFabricatorMenu(id, inventory, this);
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
