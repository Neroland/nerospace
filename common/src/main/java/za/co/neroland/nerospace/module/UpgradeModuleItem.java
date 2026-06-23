package za.co.neroland.nerospace.module;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * A machine upgrade module card. One {@link UpgradeModuleItem} class backs all module types; each
 * registered item fixes its {@link ModuleType} so the card is identified purely by its item (no data
 * component needed) and is portable across every machine that embeds a {@link MachineModules}.
 */
public class UpgradeModuleItem extends Item {

    private final ModuleType type;

    public UpgradeModuleItem(Properties properties, ModuleType type) {
        super(properties);
        this.type = type;
    }

    public ModuleType moduleType() {
        return this.type;
    }

    /** @return the module type of {@code stack}, or {@code null} if it is not a module card. */
    @Nullable
    public static ModuleType typeOf(ItemStack stack) {
        return stack.getItem() instanceof UpgradeModuleItem module ? module.moduleType() : null;
    }

    public static boolean isModule(ItemStack stack) {
        return stack.getItem() instanceof UpgradeModuleItem;
    }
}
