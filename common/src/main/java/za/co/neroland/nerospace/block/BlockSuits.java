package za.co.neroland.nerospace.block;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModItems;

/** Suit-piece gates for contact hazards (002 §2.1.7): sealed = any oxygen suit; heat = the heat variant only. */
public final class BlockSuits {

    private BlockSuits() {
    }

    public static boolean sealedBoots(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        ItemStack feet = living.getItemBySlot(EquipmentSlot.FEET);
        return feet.is(ModItems.OXYGEN_SUIT_BOOTS.get()) || feet.is(ModItems.OXYGEN_SUIT_T2_BOOTS.get())
                || feet.is(ModItems.OXYGEN_SUIT_HEAT_BOOTS.get()) || feet.is(ModItems.OXYGEN_SUIT_COLD_BOOTS.get());
    }

    public static boolean heatBoots(Entity entity) {
        return entity instanceof LivingEntity living
                && living.getItemBySlot(EquipmentSlot.FEET).is(ModItems.OXYGEN_SUIT_HEAT_BOOTS.get());
    }

    public static boolean sealedHelmet(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        ItemStack head = living.getItemBySlot(EquipmentSlot.HEAD);
        return head.is(ModItems.OXYGEN_SUIT_HELMET.get()) || head.is(ModItems.OXYGEN_SUIT_T2_HELMET.get())
                || head.is(ModItems.OXYGEN_SUIT_HEAT_HELMET.get()) || head.is(ModItems.OXYGEN_SUIT_COLD_HELMET.get());
    }
}
