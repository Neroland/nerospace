package za.co.neroland.nerospace.pipe;

import net.minecraft.util.StringRepresentable;

/**
 * Per-face mode of a {@link UniversalPipeBlockEntity} connection to a neighbouring (non-pipe) block.
 * Set with the Configurator. {@code AUTO} both pulls from providers and pushes to receivers;
 * {@code PULL}/{@code PUSH} restrict the direction; {@code DISABLED} ignores that face entirely.
 */
public enum PipeConnectionMode implements StringRepresentable {
    AUTO("auto"),
    PULL("pull"),
    PUSH("push"),
    DISABLED("disabled");

    private final String name;

    PipeConnectionMode(String name) {
        this.name = name;
    }

    public boolean canPull() {
        return this == AUTO || this == PULL;
    }

    public boolean canPush() {
        return this == AUTO || this == PUSH;
    }

    public boolean isConnected() {
        return this != DISABLED;
    }

    public PipeConnectionMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
