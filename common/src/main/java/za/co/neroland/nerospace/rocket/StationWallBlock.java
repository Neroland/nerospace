package za.co.neroland.nerospace.rocket;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Station Wall: a plain structural block, with one job beyond decoration. Sixteen of them ringing a 3x3
 * of Launch Pads form a Tier 3 pad, so placing one tells the player when that ring is complete (see
 * {@link LaunchPadFeedback}). Otherwise it behaves exactly like a vanilla {@link Block}.
 */
public class StationWallBlock extends Block {

    public StationWallBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        LaunchPadFeedback.onPartPlaced(level, pos, placer);
    }
}
