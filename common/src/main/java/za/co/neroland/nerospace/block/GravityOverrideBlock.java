package za.co.neroland.nerospace.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.BlockGravityRules.Kind;
import za.co.neroland.nerospace.block.world.BlockGravityOverrides;

/**
 * A block that registers a gravity override above itself (Gravity Shard Block = {@link Kind#SHARD}, Gravity Plate =
 * {@link Kind#PLATE}; 002 §10.4). Registration happens on placement and is removed when the block is removed. Beyond
 * 64 per chunk (config may lower) the placement is refused: the block pops back as an item with a message.
 */
public class GravityOverrideBlock extends Block {

    public static final MapCodec<GravityOverrideBlock> CODEC = BlockCodecs.simple(p -> new GravityOverrideBlock(p, Kind.SHARD));

    private final Kind kind;

    public GravityOverrideBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    protected MapCodec<? extends GravityOverrideBlock> codec() {
        return CODEC;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            BlockGravityOverrides.get(server).register(pos, this.kind);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel server) {
            // onPlace already tried to register; a full chunk leaves no entry → refuse the placement.
            if (!BlockGravityOverrides.get(server).contains(pos)) {
                level.destroyBlock(pos, true);
                if (placer instanceof Player player) {
                    player.sendOverlayMessage(Component.translatable("block.nerospace.gravity_override.full"));
                }
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockGravityOverrides.get(level).unregister(pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
