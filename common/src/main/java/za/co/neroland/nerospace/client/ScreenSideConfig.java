package za.co.neroland.nerospace.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.client.SideConfigWidget;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideConfigured;

/**
 * Client-side glue that drops Neroland Core's reusable {@link SideConfigWidget} onto a Nerospace
 * machine screen. Nerospace's machine menus are plain (non-extended) menus that carry no block
 * position, so the widget needs the open machine's {@link BlockPos} from the client side: it is
 * resolved once, when the screen opens, from the block the player interacted with (the crosshair
 * {@link BlockHitResult}), and validated against the client block-entity actually being a
 * {@link SideConfigured} machine. The widget is then built from that block-entity's own
 * {@link SideConfigComponent#config()} (the client BE constructs the same structure as the server),
 * and it requests the authoritative face snapshot from the server on open.
 *
 * <p>If the position cannot be resolved (e.g. a menu opened without a matching block hit), the
 * widget is simply absent and the rest of the screen renders normally — the side-config tab is a
 * non-essential overlay.
 *
 * <p>Holds only a block position and routing state — no player identity, nothing personal
 * (POPIA/GDPR).
 */
public final class ScreenSideConfig {

    /** Anchor: top-right of a 176-wide machine panel (imageWidth - 20, 4). */
    public static final int ANCHOR_X = 176 - 20;
    public static final int ANCHOR_Y = 4;

    @Nullable
    private final SideConfigWidget widget;

    private ScreenSideConfig(@Nullable SideConfigWidget widget) {
        this.widget = widget;
    }

    /**
     * Build the side-config overlay for a screen, resolving the open machine from the crosshair hit.
     *
     * @param typeKey the machine's block id (used for the widget's translation keys), e.g.
     *                {@code "nerospace:nerosium_grinder"}.
     */
    public static ScreenSideConfig create(String typeKey, int anchorX, int anchorY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.hitResult instanceof BlockHitResult blockHit)) {
            return new ScreenSideConfig(null);
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockEntity be = mc.level.getBlockEntity(pos);
        if (!(be instanceof SideConfigured configured)) {
            return new ScreenSideConfig(null);
        }
        SideConfigComponent comp = configured.sideConfig();
        if (comp == null) {
            return new ScreenSideConfig(null);
        }
        SideConfigWidget widget = new SideConfigWidget(pos, comp.config(), typeKey, anchorX, anchorY);
        return new ScreenSideConfig(widget);
    }

    public void render(GuiGraphicsExtractor g, int guiLeft, int guiTop, int mouseX, int mouseY) {
        if (this.widget != null) {
            this.widget.render(g, guiLeft, guiTop, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int guiLeft, int guiTop) {
        return this.widget != null && this.widget.mouseClicked(mouseX, mouseY, button, guiLeft, guiTop);
    }
}
