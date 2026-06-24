package za.co.neroland.nerospace.machine.quarry;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import za.co.neroland.nerospace.Nerospace;

/**
 * The quarry's chunk {@link TicketController}. An actively-mining quarry force-loads the (bounded)
 * chunks its region spans so a layer-by-layer dig continues even when the player walks its edges out
 * of view; tickets are released when the quarry finishes, pauses or is removed.
 */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class QuarryChunkLoader {

    public static final TicketController CONTROLLER = new TicketController(
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "quarry"));

    private QuarryChunkLoader() {
    }

    @SubscribeEvent
    public static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }
}
