package za.co.neroland.nerospace.suit;

import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModDataComponents;
import za.co.neroland.nerospace.village.AlienTrades;

/** Preinstalls the weave a culture's world needs on a Pathfinder piece sold by that culture. */
public final class SuitTrades {

    private SuitTrades() {
    }

    public static void decorate(String cultureId, ItemStack stack) {
        if (cultureId == null || !(stack.getItem() instanceof SuitPieceItem)) {
            return;
        }
        String weave = "";
        boolean rad = false;
        if (AlienTrades.EMBER_KIN.equals(cultureId)) {
            weave = SuitIds.THERMAL_WEAVE;
        } else if (AlienTrades.FROSTBOUND.equals(cultureId) || AlienTrades.BRINEKIN.equals(cultureId)) {
            weave = SuitIds.CRYO_WEAVE;
        } else if (AlienTrades.DUSKBORN.equals(cultureId)) {
            rad = true;
        } else {
            return;
        }
        SuitLoadout loadout = new SuitLoadout(SuitFrame.MK2.ordinal(), weave,
                rad ? java.util.List.of(SuitIds.RAD_LINING) : java.util.List.of(), java.util.List.of(), 0, -1, 0);
        SuitStacks.setLoadout(stack, loadout);
        if (rad) {
            stack.set(ModDataComponents.RAD_LINING.get(), Unit.INSTANCE);
        }
    }
}
