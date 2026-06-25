package za.co.neroland.nerospace.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.NonNull;

/**
 * Cross-loader packet send seam (the counterpart to NeoForge {@code PacketDistributor} /
 * {@code ClientPacketDistributor} and Fabric {@code Server|ClientPlayNetworking.send}). Payload
 * <em>types</em> and handlers are declared once in {@link za.co.neroland.nerospace.network.ModNetwork};
 * each loader registers them and implements this send interface, resolved via {@link Services}.
 */
public interface NetworkPlatform {

    /** Server → one client. */
    void sendToPlayer(@NonNull ServerPlayer player, @NonNull CustomPacketPayload payload);

    /** Client → server (call only on the physical client). */
    void sendToServer(@NonNull CustomPacketPayload payload);
}
