package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Places a {@link RocketEntity} of a fixed {@link RocketTier} onto a {@link RocketLaunchPadBlock}.
 * Using it anywhere else does nothing, so a launch pad is required to deploy a rocket.
 */
public class RocketItem extends Item {

    private final RocketTier tier;

    public RocketItem(Properties properties, RocketTier tier) {
        super(properties);
        this.tier = tier;
    }

    public RocketTier tier() {
        return this.tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (ReturnSiteBlock.isReturnSite(state)) {
            return deployOnReturnSite(context);
        }

        if (!(state.getBlock() instanceof RocketLaunchPadBlock)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            // Multiblock gating: deploying needs a properly formed 3x3 pad; a Tier 3 rocket
            // additionally needs the pad ringed with Station Wall (the same checks re-run at launch).
            Player player = context.getPlayer();
            java.util.Set<BlockPos> pads = LaunchPadMultiblock.connectedPads(level, pos);
            if (!LaunchPadMultiblock.isFullThreeByThree(pads)) {
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("item.nerospace.rocket.pad_incomplete"));
                }
                return InteractionResult.SUCCESS;
            }
            // Tier 3 needs the Station-Wall ring OR a Heavy Launch Complex (5x5 + gantry).
            if (this.tier == RocketTier.TIER_3
                    && !LaunchPadMultiblock.hasStationWallRing(level, pads)
                    && !LaunchPadMultiblock.isHeavyComplex(level, pads)) {
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("item.nerospace.rocket.pad_ring_required"));
                }
                return InteractionResult.SUCCESS;
            }
            // Tier 4 needs the Heavy Launch Complex specifically (no ring shortcut).
            if (this.tier == RocketTier.TIER_4 && !LaunchPadMultiblock.isHeavyComplex(level, pads)) {
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("item.nerospace.rocket.pad_heavy_required"));
                }
                return InteractionResult.SUCCESS;
            }

            // One rocket per pad: reject a second deploy onto an occupied cluster.
            if (LaunchPadMultiblock.rocketAbove(level, pads) != null) {
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("item.nerospace.rocket.pad_occupied"));
                }
                return InteractionResult.SUCCESS;
            }

            // Centre the rocket on the formed square (the 5x5 when present, else the 3x3) and stand it
            // on the pad's plate surface (the pad is a 3px platform, not a cube).
            BlockPos corner5 = LaunchPadMultiblock.fullSquareCorner(pads, 5);
            BlockPos corner = corner5 != null ? corner5 : LaunchPadMultiblock.fullSquareCorner(pads, 3);
            int size = corner5 != null ? 5 : 3;
            BlockPos centre = corner == null ? pos
                    : new BlockPos(corner.getX() + size / 2, corner.getY(), corner.getZ() + size / 2);
            RocketEntity rocket = new RocketEntity(
                    level, centre.getX() + 0.5D, centre.getY() + RocketLaunchPadBlock.SURFACE_HEIGHT,
                    centre.getZ() + 0.5D, this.tier);
            level.addFreshEntity(rocket);

            ItemStack stack = context.getItemInHand();
            if (player != null && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (player != null) {
                player.sendSystemMessage(Component.translatable("item.nerospace.rocket.deployed"));
            }
        }

        return InteractionResult.SUCCESS;
    }

    private InteractionResult deployOnReturnSite(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide()) {
            Player player = context.getPlayer();
            if (rocketAboveReturnSite(level, pos)) {
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("item.nerospace.rocket.pad_occupied"));
                }
                return InteractionResult.SUCCESS;
            }

            RocketEntity rocket = new RocketEntity(
                    level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, this.tier);
            level.addFreshEntity(rocket);

            ItemStack stack = context.getItemInHand();
            if (player != null && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (player != null) {
                player.sendSystemMessage(Component.translatable("item.nerospace.rocket.deployed"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean rocketAboveReturnSite(Level level, BlockPos pos) {
        AABB box = new AABB(pos.getX(), pos.getY() + 0.1D, pos.getZ(),
                pos.getX() + 1.0D, pos.getY() + 8.0D, pos.getZ() + 1.0D);
        return !level.getEntitiesOfClass(RocketEntity.class, box, rocket -> !rocket.isLaunching()).isEmpty();
    }
}
