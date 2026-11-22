package za.co.neroland.nerospace.suit;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.minecraft.world.item.component.TooltipDisplay;

/**
 * One of the four Pathfinder pieces. Armour values live on the stack (rewritten when the frame changes) so a
 * single item id carries every frame. The piece never breaks: damage stops one point short of the virtual max
 * and that point is the breached state.
 */
//? if >=1.21.5 {
public class SuitPieceItem extends Item {
//?} else if >=1.21.2 {
/*public class SuitPieceItem extends net.minecraft.world.item.ArmorItem {
*///?} else {
/*public class SuitPieceItem extends net.minecraft.world.item.ArmorItem {
*///?}

    private final SuitPiece piece;

    //? if >=1.21.5 {
    public SuitPieceItem(Properties properties, SuitPiece piece) {
        super(properties);
        this.piece = piece;
    }
    //?} else if >=1.21.2 {
    /*public SuitPieceItem(net.minecraft.world.item.equipment.ArmorMaterial material,
            net.minecraft.world.item.equipment.ArmorType type, Properties properties, SuitPiece piece) {
        super(material, type, properties);
        this.piece = piece;
    }
    *///?} else {
    /*public SuitPieceItem(net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> material,
            net.minecraft.world.item.ArmorItem.Type type, Properties properties, SuitPiece piece) {
        super(material, type, properties);
        this.piece = piece;
    }
    *///?}

    public SuitPiece piece() {
        return this.piece;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getDamageValue() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int max = SuitDurability.max(this.piece, SuitStacks.loadout(stack).frameEnum());
        int damage = Math.min(stack.getDamageValue(), max);
        return Math.round(13.0F - damage * 13.0F / max);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        SuitLoadout loadout = SuitStacks.loadout(stack);
        tooltip.accept(Component.translatable("item.nerospace.suit.frame." + loadout.frameEnum().name().toLowerCase()));
        if (loadout.hasWeave()) {
            tooltip.accept(Component.translatable("item.nerospace.suit.weave", name(loadout.weave())));
        }
        for (String lining : loadout.linings()) {
            tooltip.accept(Component.translatable("item.nerospace.suit.lining", name(lining)));
        }
        int index = 0;
        for (String module : loadout.modules()) {
            SuitSlotRules.Rule rule = SuitSlotRules.rule(module, this.piece);
            String label = rule == null ? Component.translatable("item.nerospace.suit.unknown_module").getString()
                    : name(module).getString();
            tooltip.accept(Component.translatable(loadout.toggleOn(index) ? "item.nerospace.suit.module_on"
                    : "item.nerospace.suit.module_off", label));
            index++;
        }
        if (this.piece == SuitPiece.CHEST) {
            SuitCoreState core = SuitStacks.core(stack);
            int cap = SuitEnergyMath.capacity(loadout.frameEnum(), loadout.moduleCount(SuitIds.POWER_CELL), 1.0);
            tooltip.accept(Component.translatable("item.nerospace.suit.energy", core.energy(), cap));
        }
        if (SuitDurability.breached(stack.getDamageValue(), SuitDurability.max(this.piece, loadout.frameEnum()))) {
            tooltip.accept(Component.translatable("item.nerospace.suit.breached"));
        }
    }

    private static Component name(String id) {
        String path = id.startsWith("nerospace:") ? id.substring("nerospace:".length()) : id;
        return Component.translatable("item.nerospace." + path);
    }
}
