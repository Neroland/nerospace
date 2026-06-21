package za.co.neroland.nerospace.gear;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.registry.ModItems;

/**
 * Shared ability logic for the exclusive Artificer gear (ALIEN_VILLAGERS_DESIGN.md §6.1). Loader-agnostic
 * predicates the per-loader event hooks call; this is the cross-loader stand-in for the root's
 * {@code gear/AlienGearEvents} (a NeoForge {@code @EventBusSubscriber}). Each loader binds its own
 * fall-damage event and defers the decision here:
 * <ul>
 *   <li>NeoForge — {@code LivingFallEvent.setDamageMultiplier(0)} when {@link #negatesFall} is true;</li>
 *   <li>Fabric — {@code ServerLivingEntityEvents.ALLOW_DAMAGE} cancels a {@code FALL} source when it is.</li>
 * </ul>
 */
public final class AlienGearAbilities {

    private AlienGearAbilities() {
    }

    /** Grav Striders: while carried anywhere in the inventory, alien grav-tech cushions the wearer's fall. */
    public static boolean negatesFall(Entity entity) {
        return entity instanceof Player player
                && player.getInventory().hasAnyMatching((ItemStack s) -> s.is(ModItems.GRAV_STRIDERS.get()));
    }
}
