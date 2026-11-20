package za.co.neroland.nerospace.world.planet.europa.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.PhosphorRules;

/**
 * Europium Lamp: a decorative red lamp. Using it with an empty hand switches it on or off ({@code LIT}); lit it gives
 * a light of 14, unlit none. Register with {@code lightLevel(EuropiumLampBlock::light)}. Work happens on use only.
 */
public class EuropiumLampBlock extends Block {

    public static final MapCodec<EuropiumLampBlock> CODEC = BlockCodecs.simple(EuropiumLampBlock::new);

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    @SuppressWarnings("this-escape")
    public EuropiumLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    protected MapCodec<? extends EuropiumLampBlock> codec() {
        return CODEC;
    }

    /** Light for a state, for the registered properties. */
    public static int light(BlockState state) {
        return PhosphorRules.light(state.getValue(LIT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide()) {
            boolean lit = PhosphorRules.toggle(state.getValue(LIT));
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, lit ? 1.2F : 0.9F);
        }
        return InteractionResult.SUCCESS;
    }
}
