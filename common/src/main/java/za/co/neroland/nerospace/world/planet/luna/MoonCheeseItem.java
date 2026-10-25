package za.co.neroland.nerospace.world.planet.luna;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** The cheese item: food plus its one line of tooltip. */
public class MoonCheeseItem extends Item {

    public static final String TOOLTIP_KEY = "item.nerospace.moon_cheese.tooltip";

    public MoonCheeseItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable(TOOLTIP_KEY).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
    }
}
