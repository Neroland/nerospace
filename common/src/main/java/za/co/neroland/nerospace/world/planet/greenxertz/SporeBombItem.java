package za.co.neroland.nerospace.world.planet.greenxertz;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import za.co.neroland.nerospace.block.SporeBurstBlock;

/**
 * Spore Bomb (crafted from 4 Spore Pods + 1 gunpowder): lobbed up to {@link #RANGE} blocks, it bursts into exactly the
 * Spore Pod's cloud (r 2.5, 100 ticks, Nausea I 8 s, sealed helmets protect) where it lands. It reuses
 * {@link SporeBurstBlock#burst} by planting a pod in the free cell at the impact and bursting it at once, so the
 * cloud, its gate and its lifetime are the shared ones. It never replaces a block: no free cell, no burst, and the
 * bomb is kept.
 */
public class SporeBombItem extends Item {

    public static final double RANGE = 16.0;

    public SporeBombItem(Properties properties) {
        super(properties);
    }

    //? if >=1.21.2 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //?} else {
    /*// Before 1.21.2 Item.use returns the held stack with the result.
    @Override
    public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(Level level,
            Player player, InteractionHand hand) {
        InteractionResult result = useResult(level, player, hand);
        return new net.minecraft.world.InteractionResultHolder<>(result, player.getItemInHand(hand));
    }

    private InteractionResult useResult(Level level, Player player, InteractionHand hand) {
    *///?}
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos target = impactCell(server, player);
        if (target == null) {
            return InteractionResult.FAIL;
        }
        server.setBlock(target, GreenxertzContent.SPORE_POD.get().defaultBlockState(), 2);
        SporeBurstBlock.burst(server, target);
        server.playSound(null, target, SoundEvents.SPORE_BLOSSOM_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    /** The air cell where the bomb lands: in front of the hit face, or at full range in open air. */
    private static BlockPos impactCell(ServerLevel level, Player player) {
        HitResult hit = player.pick(RANGE, 1.0F, false);
        BlockPos cell;
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            cell = block.getBlockPos().relative(block.getDirection());
        } else {
            cell = BlockPos.containing(hit.getLocation());
        }
        return level.isLoaded(cell) && level.getBlockState(cell).isAir() ? cell : null;
    }
}
