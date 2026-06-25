package za.co.neroland.nerospace.pipe;

import net.minecraft.util.StringRepresentable;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Per-face, per-resource-type input/output mode of a Universal Pipe. Every face holds one of these for
 * each {@link PipeResourceType}, so a single face can (e.g.) take fluid IN while sending energy OUT.
 * {@code AUTO} both pulls from providers and pushes to receivers.
 *
 * <p>Cross-loader port: pure vanilla ({@link StringRepresentable}); identical to the standalone mod.</p>
 */
public enum PipeIoMode implements StringRepresentable {
    AUTO("auto"),
    IN("in"),
    OUT("out"),
    OFF("off");

    public static final PipeIoMode[] VALUES = values();

    private final String name;

    PipeIoMode(String name) {
        this.name = name;
    }

    public boolean canPull() {
        return this == AUTO || this == IN;
    }

    public boolean canPush() {
        return this == AUTO || this == OUT;
    }

    public boolean isConnected() {
        return this != OFF;
    }

    public PipeIoMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    @Override
    public @NonNull String getSerializedName() {
        return NerospaceCommon.requireNonNull(this.name);
    }
}
