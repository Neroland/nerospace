package za.co.neroland.nerospace.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.Nerospace;

/**
 * C2S: the Configurator GUI sets one face × layer I/O mode on a Universal Pipe. Indices are
 * validated server-side ({@code face} = {@code Direction.get3DDataValue()}, {@code layer} =
 * {@code PipeResourceType} ordinal, {@code mode} = {@code PipeIoMode} ordinal).
 */
public record SetPipeModePayload(BlockPos pos, int face, int layer, int mode) implements CustomPacketPayload {

    public static final Type<SetPipeModePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Nerospace.MODID, "set_pipe_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetPipeModePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetPipeModePayload::pos,
                    ByteBufCodecs.VAR_INT, SetPipeModePayload::face,
                    ByteBufCodecs.VAR_INT, SetPipeModePayload::layer,
                    ByteBufCodecs.VAR_INT, SetPipeModePayload::mode,
                    (pos, face, layer, mode) -> new SetPipeModePayload(pos, face, layer, mode));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
