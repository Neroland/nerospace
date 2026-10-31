package za.co.neroland.nerospace.compat.mc;

/**
 * Version seam for block {@code useItemOn} results. 1.21.2 folded {@code ItemInteractionResult} into
 * {@link net.minecraft.world.InteractionResult} (with the new {@code TRY_WITH_EMPTY_HAND}). On Minecraft 1.21.1 the
 * blocks keep their 26.x body in a private helper that returns an {@code InteractionResult}, and the real override
 * converts it here. {@code null} stands for {@code TRY_WITH_EMPTY_HAND} on 1.21.1 (the build rewrites the constant
 * and {@code super.useItemOn(..)} calls to this class there; see legacyRenames in stonecutter.gradle).
 * On 1.21.2 and later this class is empty.
 */
public final class UseCompat {

    private UseCompat() {
    }

    //? if <1.21.2 {
    /*// "Try the empty-hand interaction" (PASS_TO_DEFAULT_BLOCK_INTERACTION) inside the 1.21.1 useItemOn helpers.
    public static final net.minecraft.world.InteractionResult TRY_WITH_EMPTY_HAND = null;

    // A 26.x-style useItemOn result as the 1.21.1 ItemInteractionResult.
    public static net.minecraft.world.ItemInteractionResult toItem(net.minecraft.world.InteractionResult result) {
        if (result == null) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return switch (result) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> net.minecraft.world.ItemInteractionResult.SUCCESS;
            case CONSUME, CONSUME_PARTIAL -> net.minecraft.world.ItemInteractionResult.CONSUME;
            case FAIL -> net.minecraft.world.ItemInteractionResult.FAIL;
            case PASS -> net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        };
    }

    // A 1.21.1 super.useItemOn(..) result as the 26.x-style result the helpers return.
    public static net.minecraft.world.InteractionResult fromItem(net.minecraft.world.ItemInteractionResult result) {
        return switch (result) {
            case SUCCESS -> net.minecraft.world.InteractionResult.SUCCESS;
            case CONSUME -> net.minecraft.world.InteractionResult.CONSUME;
            case CONSUME_PARTIAL -> net.minecraft.world.InteractionResult.CONSUME_PARTIAL;
            case FAIL -> net.minecraft.world.InteractionResult.FAIL;
            case SKIP_DEFAULT_BLOCK_INTERACTION -> net.minecraft.world.InteractionResult.PASS;
            case PASS_TO_DEFAULT_BLOCK_INTERACTION -> TRY_WITH_EMPTY_HAND;
        };
    }
    *///?}
}
