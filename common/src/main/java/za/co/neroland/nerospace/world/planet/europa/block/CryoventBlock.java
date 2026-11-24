package za.co.neroland.nerospace.world.planet.europa.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.VentTapRules;
import za.co.neroland.nerospace.world.planet.europa.EuropaContent;

/**
 * Cryovent: a surface vent that wells up brine. Used with an empty bucket while {@code CHARGED} it gives one Brine
 * Bucket and loses its charge; a scheduled tick charges it again 600 ticks later. An uncharged vent that has no tick
 * pending (placed by a command, say) is picked up by a random tick, which schedules the same recharge. Cost: one state
 * write per recharge; only uncharged vents take random ticks.
 */
public class CryoventBlock extends Block {

    public static final MapCodec<CryoventBlock> CODEC = BlockCodecs.simple(CryoventBlock::new);

    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");

    @SuppressWarnings("this-escape")
    public CryoventBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(CHARGED, true));
    }

    protected MapCodec<? extends CryoventBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGED);
    }

    //? if >=1.21.2 {
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
    //?} else if >=1.20.5 {
    /*// Before 1.21.2 useItemOn returns an ItemInteractionResult; the 26.x body below runs as a helper.
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return za.co.neroland.nerospace.compat.mc.UseCompat.toItem(
                useItemOnResult(stack, state, level, pos, player, hand, hit));
    }

    private InteractionResult useItemOnResult(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
    *///?} else {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = useItemOnResult(player.getItemInHand(hand), state, level, pos, player, hand, hit);
        return result == null ? InteractionResult.PASS : result;
    }

    private InteractionResult useItemOnResult(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
    *///?}
        if (!VentTapRules.canTap(state.getValue(CHARGED), stack.is(Items.BUCKET))) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(CHARGED, VentTapRules.chargedAfterTap()), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, VentTapRules.RECHARGE_TICKS);
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player,
                    new ItemStack(EuropaContent.BRINE_BUCKET.get(), VentTapRules.BUCKETS_PER_TAP)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (VentTapRules.recharges(state.getValue(CHARGED))) {
            level.setBlock(pos, state.setValue(CHARGED, true), Block.UPDATE_ALL);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return VentTapRules.recharges(state.getValue(CHARGED));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (VentTapRules.recharges(state.getValue(CHARGED)) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, VentTapRules.RECHARGE_TICKS);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(CHARGED) && random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0.0, 0.06, 0.0);
        }
    }
}
