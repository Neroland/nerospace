package za.co.neroland.nerospace.world.planet.europa.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.BreachWarnRules;

/**
 * Shell Ice: the body of Europa's ice shell. A player who mines a block with water or brine within 6 cells along any
 * of the six axes hears a warning crack, higher pitched the closer the liquid is, and sees a short notice. It is a
 * warning only: nothing floods. Cost: 36 block reads per mined block at most.
 */
public class ShellIceBlock extends Block {

    public static final MapCodec<ShellIceBlock> CODEC = BlockCodecs.simple(ShellIceBlock::new);

    public static final String WARNING_KEY = "message.nerospace.europa.breach_warning";

    public ShellIceBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends ShellIceBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server) {
            int distance = BreachWarnRules.nearest(axisDistances(level, pos));
            if (BreachWarnRules.warns(distance)) {
                level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F,
                        BreachWarnRules.pitch(distance));
                server.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        BreachWarnRules.PARTICLES, 0.3, 0.3, 0.3, 0.02);
                player.sendOverlayMessage(Component.translatable(WARNING_KEY));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Distance to the first liquid along each of the six axes (0 = none within range). */
    private static int[] axisDistances(Level level, BlockPos pos) {
        int[] out = new int[6];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (Direction d : Direction.values()) {
            p.set(pos);
            for (int i = 1; i <= BreachWarnRules.RANGE; i++) {
                p.move(d);
                if (EuropaHeat.isLiquid(level.getBlockState(p))) {
                    out[d.get3DDataValue()] = i;
                    break;
                }
            }
        }
        return out;
    }
}
