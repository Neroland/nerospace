package za.co.neroland.nerospace.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.pipe.PipeIoMode;
import za.co.neroland.nerospace.pipe.PipeResourceType;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;

/** Custom payload registration + server handlers. */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class ModPayloads {

    private ModPayloads() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SetPipeModePayload.TYPE, SetPipeModePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handleSetPipeMode(payload, context.player())));
    }

    private static void handleSetPipeMode(SetPipeModePayload payload, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ServerLevel level = serverPlayer.level();
        BlockPos pos = payload.pos();
        // Reach check: ignore packets for far-away pipes.
        if (serverPlayer.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0
                || !(level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe)) {
            return;
        }
        Direction face = Direction.from3DDataValue(Math.floorMod(payload.face(), 6));
        PipeResourceType type = PipeResourceType.VALUES[Math.floorMod(payload.layer(), PipeResourceType.VALUES.length)];
        PipeIoMode mode = PipeIoMode.VALUES[Math.floorMod(payload.mode(), PipeIoMode.VALUES.length)];
        pipe.setMode(face, type, mode);
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
    }
}
