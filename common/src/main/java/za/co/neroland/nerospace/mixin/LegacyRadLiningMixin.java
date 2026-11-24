package za.co.neroland.nerospace.mixin;

//? if <1.20.5 {
/*import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import za.co.neroland.nerospace.registry.ModDataComponents;
import za.co.neroland.nerospace.registry.ModItems;

@Mixin(SmithingTransformRecipe.class)
abstract class LegacyRadLiningMixin {
    @Inject(method = "assemble(Lnet/minecraft/world/Container;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private void nerospace$applyLining(Container input, RegistryAccess registries, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack output = cir.getReturnValue();
        if (output.isEmpty() || !input.getItem(0).is(ModItems.RAD_LINING.get())
                || !output.is(input.getItem(1).getItem()) || ModDataComponents.RAD_LINING.has(output)) {
            return;
        }
        ModDataComponents.RAD_LINING.set(output, Unit.INSTANCE);
        var display = output.getOrCreateTagElement("display");
        ListTag lore = display.getList("Lore", Tag.TAG_STRING);
        String description = Component.Serializer.toJson(Component.translatable("item.nerospace.rad_lining.applied")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GOLD).withItalic(false)));
        lore.add(StringTag.valueOf(description));
        display.put("Lore", lore);
    }
}
*///?}
