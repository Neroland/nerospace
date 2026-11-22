package za.co.neroland.nerospace.block;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.suit.SuitIds;
import za.co.neroland.nerospace.suit.SuitPiece;
import za.co.neroland.nerospace.suit.SuitProtection;
import za.co.neroland.nerospace.suit.SuitView;

/** Suit-piece gates for contact hazards. Sealed = any suit piece; heat = a piece that carries the Thermal weave. */
public final class BlockSuits {

    private BlockSuits() {
    }

    public static boolean sealedBoots(Entity entity) {
        return worn(entity, EquipmentSlot.FEET, SuitPiece.BOOTS);
    }

    public static boolean heatBoots(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        SuitProtection.Slot slot = SuitView.slot(living.getItemBySlot(EquipmentSlot.FEET));
        if (!slot.suit() || slot.breached() || SuitView.piece(living.getItemBySlot(EquipmentSlot.FEET)) != SuitPiece.BOOTS) {
            return false;
        }
        if (slot.legacyId() != null) {
            return slot.legacyId().contains("heat");
        }
        return slot.loadout() != null && SuitIds.THERMAL_WEAVE.equals(slot.loadout().weave());
    }

    public static boolean sealedHelmet(Entity entity) {
        return worn(entity, EquipmentSlot.HEAD, SuitPiece.HELMET);
    }

    private static boolean worn(Entity entity, EquipmentSlot slot, SuitPiece piece) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        ItemStack stack = living.getItemBySlot(slot);
        SuitProtection.Slot view = SuitView.slot(stack);
        return view.suit() && !view.breached() && SuitView.piece(stack) == piece;
    }
}
