package za.co.neroland.nerospace.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Server → client nearest-meteor snapshot for the Meteor Tracker (meteor-events design §6). Pushed
 * only to players holding a tracker. {@code present} false means "no tracked meteor" (the readout
 * idles); otherwise the packed position + {@link za.co.neroland.nerospace.meteor.MeteorSite} state
 * let the client draw direction, distance and incoming/landed status.
 *
 * <p>Cross-loader port note: the multiloader's first networking payload — registered (with its
 * client handler {@code ClientMeteorTracker::accept}) in {@code ModNetwork.init()}; both loaders'
 * networking seams pick it up from the {@code ModNetwork.clientbound()} list automatically.</p>
 */
public record MeteorSyncPayload(boolean present, long pos, int state) implements CustomPacketPayload {

    public static final Type<MeteorSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "meteor_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MeteorSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MeteorSyncPayload::present,
            ByteBufCodecs.VAR_LONG, MeteorSyncPayload::pos,
            ByteBufCodecs.VAR_INT, MeteorSyncPayload::state,
            MeteorSyncPayload::new);

    public static final MeteorSyncPayload ABSENT = new MeteorSyncPayload(false, 0L, 0);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
