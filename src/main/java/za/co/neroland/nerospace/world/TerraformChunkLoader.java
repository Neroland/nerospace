package za.co.neroland.nerospace.world;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import za.co.neroland.nerospace.Nerospace;

/**
 * Opt-in active terraforming (terraform design §2.3). A {@link TicketController} the Terraformer uses to
 * force-load a bounded set of chunks around its working frontier so conversion continues while the
 * player is away. Off by default ({@code terraformForceLoadChunks}) and bounded by
 * {@code terraformMaxForcedChunks} — force-loading is the classic TPS/footprint footgun.
 */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class TerraformChunkLoader {

    public static final TicketController CONTROLLER = new TicketController(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "terraformer"));

    private TerraformChunkLoader() {
    }

    @SubscribeEvent
    public static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }
}
