package za.co.neroland.nerospace.gear;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Ability hooks for the exclusive Artificer gear (ALIEN_VILLAGERS_DESIGN.md §6.1). Grav Striders:
 * while carried, alien grav-tech cushions the wearer — fall damage is negated.
 */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class AlienGearEvents {

    private AlienGearEvents() {
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player
                && player.getInventory().hasAnyMatching(s -> s.is(ModItems.GRAV_STRIDERS.get()))) {
            event.setDamageMultiplier(0.0F);
        }
    }
}
