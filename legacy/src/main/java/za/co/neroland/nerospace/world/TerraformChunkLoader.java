package za.co.neroland.nerospace.world;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.event.level.ChunkEvent;

import za.co.neroland.nerospace.Nerospace;

/**
 * Terraform chunk handling (terraform design §2.3).
 *
 * <p>{@link #CONTROLLER} is the opt-in active-terraforming {@link TicketController}: the Terraformer can
 * force-load a bounded set of chunks around its frontier so conversion continues while the player is
 * away. Off by default ({@code terraformForceLoadChunks}), bounded by {@code terraformMaxForcedChunks}.</p>
 *
 * <p>{@link #onChunkLoad} is the lazy catch-up: the live frontier skips columns whose chunk is unloaded,
 * so when such a chunk loads later we convert any column inside an active Terraformer's radius (via
 * {@link TerraformManager}). This finishes terraforming a planet as the player explores it.</p>
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

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            TerraformManager.get(level).onChunkLoaded(level, chunk);
        }
    }
}
