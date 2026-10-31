package za.co.neroland.nerospace.client.compat;

// Minecraft 1.21.1 only (empty on every other line). 1.21.4 resolves an item's model into an ItemStackRenderState
// during extraction and draws it later. Before it items were drawn straight from the stack; this look-alike (same
// simple name, swapped in by an import rewrite in stonecutter.gradle) remembers what client.compat.ItemModelResolver
// was given and draws it with the 1.21.1 ItemRenderer.

//? if <1.21.2 {
/*import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemStackRenderState {

    ItemStack stack = ItemStack.EMPTY;
    ItemDisplayContext context = ItemDisplayContext.NONE;
    Level level;
    int seed;

    public void clear() {
        this.stack = ItemStack.EMPTY;
        this.level = null;
    }

    public boolean isEmpty() {
        return this.stack.isEmpty();
    }

    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (!this.stack.isEmpty()) {
            Minecraft.getInstance().getItemRenderer().renderStatic(this.stack, this.context, light, overlay, poseStack,
                    buffers, this.level, this.seed);
        }
    }
}
*///?}
