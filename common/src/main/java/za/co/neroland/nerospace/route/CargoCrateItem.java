package za.co.neroland.nerospace.route;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/**
 * The Cargo Crate: what a cargo flight's remaining manifest becomes when it cannot be delivered (held
 * past the timeout, or cancelled while holding). Nothing is ever deleted — the crate holds the stacks in
 * a vanilla {@code minecraft:container} component (so the tooltip lists them) and right-clicking unpacks
 * it into the player's inventory, spilling whatever does not fit. Not craftable; only flights make them.
 */
public class CargoCrateItem extends Item {

    public CargoCrateItem(Properties properties) {
        super(properties);
    }

    /** Packs {@code items} (copied) into a crate stack; an empty manifest yields {@link ItemStack#EMPTY}. */
    public static ItemStack crate(Item crateItem, List<ItemStack> items) {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack stack : items) {
            if (stack != null && !stack.isEmpty()) {
                copies.add(stack.copy());
            }
        }
        if (copies.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack crate = new ItemStack(crateItem);
        crate.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(copies));
        return crate;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack crate = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemContainerContents contents = crate.get(DataComponents.CONTAINER);
        if (contents == null) {
            return InteractionResult.PASS;
        }
        for (ItemStack stack : contents.nonEmptyItemCopyStream().toList()) {
            if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                //? if >=26.3 {
                /*player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
                *///?} else {
                player.drop(stack, false);
                //?}
            }
        }
        crate.shrink(1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_DROP_CONTENTS,
                SoundSource.PLAYERS, 0.8F, 1.0F);
        return InteractionResult.SUCCESS;
    }
}
