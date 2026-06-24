package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import za.co.neroland.nerospace.network.SetPipeModePayload;
import za.co.neroland.nerospace.pipe.PipeIoMode;
import za.co.neroland.nerospace.pipe.PipeResourceType;

/**
 * The Configurator panel for one Universal Pipe: a 6-face × 4-layer grid of mode buttons
 * (Auto → In → Out → Off). Every click applies immediately (sent to the server); Done just closes.
 * Opened by sneak-right-clicking a pipe with the Configurator.
 */
public class PipeConfigScreen extends Screen {

    private static final int CELL_W = 56;
    private static final int CELL_H = 20;
    private static final int GAP = 4;
    private static final int FACE_COL_W = 64;

    private final BlockPos pos;
    private final PipeIoMode[][] modes;

    public PipeConfigScreen(BlockPos pos, PipeIoMode[][] modes) {
        super(Component.translatable("screen.nerospace.pipe_config"));
        this.pos = pos;
        this.modes = modes;
    }

    @Override
    protected void init() {
        int gridW = FACE_COL_W + 4 * (CELL_W + GAP);
        int left = (this.width - gridW) / 2;
        int top = (this.height - (7 * (CELL_H + GAP) + 28)) / 2;

        addRenderableWidget(new StringWidget(left, top, gridW, 12, this.title, this.font));

        int headerY = top + 20;
        for (int t = 0; t < PipeResourceType.VALUES.length; t++) {
            PipeResourceType type = PipeResourceType.VALUES[t];
            addRenderableWidget(new StringWidget(left + FACE_COL_W + t * (CELL_W + GAP), headerY, CELL_W, 12,
                    type.label().copy().withStyle(s -> s.withColor(type.color() & 0xFFFFFF)), this.font));
        }

        for (int d = 0; d < 6; d++) {
            int rowY = headerY + 16 + d * (CELL_H + GAP);
            Direction face = Direction.from3DDataValue(d);
            addRenderableWidget(new StringWidget(left, rowY + 4, FACE_COL_W - GAP, 12,
                    Component.translatable("pipe.nerospace.face." + face.getName()), this.font));
            for (int t = 0; t < PipeResourceType.VALUES.length; t++) {
                final int fd = d;
                final int ft = t;
                addRenderableWidget(Button.builder(modeLabel(this.modes[d][t]), button -> {
                    this.modes[fd][ft] = this.modes[fd][ft].next();
                    button.setMessage(modeLabel(this.modes[fd][ft]));
                    ClientPacketDistributor.sendToServer(
                            new SetPipeModePayload(this.pos, fd, ft, this.modes[fd][ft].ordinal()));
                }).bounds(left + FACE_COL_W + t * (CELL_W + GAP), rowY, CELL_W, CELL_H).build());
            }
        }

        int doneY = headerY + 16 + 6 * (CELL_H + GAP) + 6;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + (gridW - 100) / 2, doneY, 100, CELL_H).build());
    }

    private static Component modeLabel(PipeIoMode mode) {
        return Component.translatable("pipe.nerospace.mode." + mode.getSerializedName());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
