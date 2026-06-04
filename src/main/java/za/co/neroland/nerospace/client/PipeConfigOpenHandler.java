package za.co.neroland.nerospace.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.item.ConfiguratorItem;
import za.co.neroland.nerospace.pipe.PipeIoMode;
import za.co.neroland.nerospace.pipe.PipeResourceType;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;

/**
 * CLIENT: sneak-right-clicking a Universal Pipe while holding the Configurator opens the per-face ×
 * per-layer config panel (and swallows the interaction so the in-world cycling doesn't also fire).
 */
@EventBusSubscriber(modid = Nerospace.MODID, value = Dist.CLIENT)
public final class PipeConfigOpenHandler {

    private PipeConfigOpenHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!level.isClientSide() || !event.getEntity().isShiftKeyDown()
                || !(event.getItemStack().getItem() instanceof ConfiguratorItem)
                || !(level.getBlockEntity(event.getPos()) instanceof UniversalPipeBlockEntity pipe)) {
            return;
        }
        event.setCanceled(true);
        PipeIoMode[][] modes = new PipeIoMode[6][PipeResourceType.VALUES.length];
        for (int d = 0; d < 6; d++) {
            for (int t = 0; t < PipeResourceType.VALUES.length; t++) {
                modes[d][t] = pipe.mode(net.minecraft.core.Direction.from3DDataValue(d), PipeResourceType.VALUES[t]);
            }
        }
        Minecraft.getInstance().setScreen(new PipeConfigScreen(event.getPos(), modes));
    }
}
