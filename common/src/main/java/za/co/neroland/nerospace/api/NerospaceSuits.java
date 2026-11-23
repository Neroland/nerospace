package za.co.neroland.nerospace.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.world.OxygenManager;

/**
 * Read-only queries about Nerospace suit pieces, for mods that react to radiation shielding.
 *
 * <p><b>Public API — semver-stable.</b> {@link #RADIATION_CAPABLE} ({@code nerospace:suits/radiation_capable})
 * lists the suit pieces that can take a Radiation Lining (the Tier 2, Heat and Cryo suits). A piece counts as
 * lined once it carries the {@code nerospace:rad_lining} data component, which {@link #isRadLined(ItemStack)}
 * checks. Pure reads; no player data is stored or returned.</p>
 */
public final class NerospaceSuits {

    /** Suit pieces that can carry a Radiation Lining. Membership is data ({@code tags/item/suits/radiation_capable.json}). */
    public static final TagKey<Item> RADIATION_CAPABLE = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath("nerospace", "suits/radiation_capable"));

    private NerospaceSuits() {
    }

    /** Whether {@code stack} is a suit piece that can be lined against radiation. */
    public static boolean isRadiationCapable(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(RADIATION_CAPABLE);
    }

    /** Whether {@code stack} carries a Radiation Lining. */
    public static boolean isRadLined(ItemStack stack) {
        return stack != null && OxygenManager.isRadLined(stack);
    }

    /** How many of {@code player}'s four armour slots hold a lined suit piece (0-4). */
    public static int radLinedPieces(Player player) {
        if (player == null) {
            return 0;
        }
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            if (isRadLined(player.getItemBySlot(slot))) {
                count++;
            }
        }
        return count;
    }

    /** Whether all four of {@code player}'s armour pieces are lined (Nerospace's own full-protection rule). */
    public static boolean isFullyRadLined(Player player) {
        return player != null && OxygenManager.radLined(player);
    }

    /** Whether {@code stack} is a Pathfinder piece or a legacy oxygen-suit piece. */
    public static boolean isSuitPiece(ItemStack stack) {
        return za.co.neroland.nerospace.suit.SuitView.isSuitStack(stack);
    }

    /**
     * Frame ordinal. A legacy Tier 1 piece is 0 and every other legacy piece is 1. A stack that is not a suit
     * reads as 0.
     */
    public static int frame(ItemStack stack) {
        za.co.neroland.nerospace.suit.SuitProtection.Slot slot = za.co.neroland.nerospace.suit.SuitView.slot(stack);
        if (!slot.suit()) {
            return 0;
        }
        if (slot.loadout() != null && slot.legacyId() == null) {
            return slot.loadout().frame();
        }
        za.co.neroland.nerospace.suit.LegacySuitConversion.Family family =
                za.co.neroland.nerospace.suit.LegacySuitConversion.family(slot.legacyId());
        return family == za.co.neroland.nerospace.suit.LegacySuitConversion.Family.T1 ? 0 : 1;
    }

    /** Whether a piece's module list contains {@code id}. */
    public static boolean hasModule(ItemStack stack, Identifier id) {
        return stack != null && id != null
                && za.co.neroland.nerospace.suit.SuitStacks.loadout(stack).hasModule(id.toString());
    }

    /** FE stored on the worn chest piece, or 0. */
    public static int wornEnergy(Player player) {
        if (player == null) {
            return 0;
        }
        return za.co.neroland.nerospace.suit.SuitStacks.core(player.getItemBySlot(EquipmentSlot.CHEST)).energy();
    }
}
