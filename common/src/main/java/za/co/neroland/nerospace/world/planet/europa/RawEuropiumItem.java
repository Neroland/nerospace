package za.co.neroland.nerospace.world.planet.europa;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Raw Europium: a decorative gem with one line of tooltip. It feeds the lamp and nothing else. */
public class RawEuropiumItem extends Item {

    public static final String TOOLTIP_KEY = "item.nerospace.raw_europium.tooltip";

    public RawEuropiumItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable(TOOLTIP_KEY).withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
    }
}
