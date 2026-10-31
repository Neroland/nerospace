package za.co.neroland.nerospace.client.compat;

// Minecraft 1.21.1 only (empty on every other line): the Minecraft.getItemModelResolver() look-alike that fills a
// client.compat ItemStackRenderState (the build rewrites the getter to INSTANCE on that line; see stonecutter.gradle).

//? if <1.21.2 {
/*import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ItemModelResolver {

    public static final ItemModelResolver INSTANCE = new ItemModelResolver();

    private ItemModelResolver() {
    }

    public void updateForTopItem(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context,
            boolean leftHand, Level level, LivingEntity entity, int seed) {
        state.stack = stack.copy();
        state.context = context;
        state.level = level;
        state.seed = seed;
    }
}
*///?}
