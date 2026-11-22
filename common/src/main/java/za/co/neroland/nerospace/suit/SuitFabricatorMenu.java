package za.co.neroland.nerospace.suit;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModMenuTypes;

/**
 * Two slots: the piece being worked, and the module, weave, lining or frame plating to apply. Buttons install
 * and remove. A legacy piece converts when it is set into the piece slot. All of that happens on the server.
 */
public class SuitFabricatorMenu extends AbstractContainerMenu {

    private final SuitFabricatorBlockEntity fabricator;
    private final SimpleContainer craft = new SimpleContainer(2);
    private final ContainerData data;

    public SuitFabricatorMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public SuitFabricatorMenu(int id, Inventory inventory, SuitFabricatorBlockEntity fabricator) {
        super(ModMenuTypes.SUIT_FABRICATOR.get(), id);
        this.fabricator = fabricator;
        this.data = new SimpleContainerData(1);
        this.addSlot(new Slot(this.craft, 0, 26, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SuitView.isSuitStack(stack) || LegacySuitConversion.isLegacyId(
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            }

            @Override
            public void set(ItemStack stack) {
                super.set(SuitTicker.convert(stack));
            }
        });
        this.addSlot(new Slot(this.craft, 1, 62, 35));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        this.addDataSlots(this.data);
    }

    public int energyPerMille() {
        if (this.fabricator == null) {
            return this.data.get(0);
        }
        int cap = Math.max(1, this.fabricator.energy().getRaw() == 0 ? 1 : 100_000);
        return (int) Math.min(1000, this.fabricator.energy().getAmount() * 1000 / cap);
    }

    @Override
    public void broadcastChanges() {
        if (this.fabricator != null) {
            ItemStack piece = this.craft.getItem(0);
            if (piece.getItem() instanceof SuitPieceItem suit && suit.piece() == SuitPiece.CHEST) {
                this.fabricator.charge(piece);
            }
            this.data.set(0, (int) Math.min(1000, this.fabricator.energy().getAmount() * 1000 / 100_000));
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide()) {
            return false;
        }
        ItemStack piece = this.craft.getItem(0);
        if (!(piece.getItem() instanceof SuitPieceItem suit)) {
            return false;
        }
        SuitLoadout loadout = SuitStacks.loadout(piece);
        if (id == 0) {
            return install(suit.piece(), piece, loadout);
        }
        if (id == 1) {
            var removed = SuitTransfers.remove(loadout, SuitSlotKind.MODULE, loadout.modules().size() - 1);
            if (removed.applied()) {
                give(player, removed, SuitSlotKind.MODULE, loadout);
                SuitStacks.setLoadout(piece, removed.loadout());
            }
            return removed.applied();
        }
        if (id == 2) {
            var removed = SuitTransfers.remove(loadout, SuitSlotKind.WEAVE, 0);
            if (removed.applied()) {
                SuitStacks.setLoadout(piece, removed.loadout());
            }
            return removed.applied();
        }
        return false;
    }

    private boolean install(SuitPiece part, ItemStack piece, SuitLoadout loadout) {
        ItemStack token = this.craft.getItem(1);
        if (token.isEmpty()) {
            return false;
        }
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(token.getItem()).toString();
        if (id.equals(SuitIds.PLATING_MK2) || id.equals(SuitIds.PLATING_MK3) || id.equals(SuitIds.PLATING_MK4)) {
            SuitFrame next = id.equals(SuitIds.PLATING_MK2) ? SuitFrame.MK2
                    : id.equals(SuitIds.PLATING_MK3) ? SuitFrame.MK3 : SuitFrame.MK4;
            if (loadout.frameEnum().ordinal() + 1 != next.ordinal()) {
                return false;
            }
            SuitStacks.setLoadout(piece, loadout.withFrame(next));
            token.shrink(1);
            return true;
        }
        var moved = SuitTransfers.install(loadout, part, id, token.getCount());
        if (!moved.applied()) {
            return false;
        }
        SuitStacks.setLoadout(piece, moved.loadout());
        token.shrink(1);
        return true;
    }

    private static void give(Player player, SuitTransfers.Move removed, SuitSlotKind kind, SuitLoadout before) {
        String id = kind == SuitSlotKind.MODULE ? before.modules().get(before.modules().size() - 1) : before.weave();
        int colon = id.indexOf(':');
        var item = colon < 0 ? java.util.Optional.<net.minecraft.world.item.Item>empty()
                : net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(id.substring(0, colon),
                                id.substring(colon + 1)));
        item.ifPresent(value -> {
            //? if >=26.3 {
            /*player.getInventory().placeItemBackInInventory(new ItemStack(value), net.minecraft.util.Prediction.SERVER_ONLY);
            *///?} else {
            player.getInventory().placeItemBackInInventory(new ItemStack(value));
            //?}
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 2) {
            if (!this.moveItemStackTo(stack, 2, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, 2, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.fabricator == null || player.distanceToSqr(
                net.minecraft.world.phys.Vec3.atCenterOf(this.fabricator.getBlockPos())) < 64;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            this.clearContainer(player, this.craft);
        }
    }
}
