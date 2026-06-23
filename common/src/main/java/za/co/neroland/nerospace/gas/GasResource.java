package za.co.neroland.nerospace.gas;

import com.mojang.serialization.Codec;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * A gas the mod's logistics can store and move — the resource side of the dedicated gas layer. Ported
 * cross-loader as a plain vanilla enum (the root project built it on NeoForge's transfer
 * {@code Resource} framework, which is loader-specific). Oxygen is the first gas; more (hydrogen, fuel
 * vapour, ...) can be added as new constants without touching the transport.
 */
public enum GasResource implements StringRepresentable {
    EMPTY("empty", 0x00000000),
    OXYGEN("oxygen", 0xFF54D46A);

    public static final Codec<GasResource> CODEC = StringRepresentable.fromEnum(GasResource::values);

    private final String name;
    private final int color;

    GasResource(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    /** Display colour (ARGB) for streams and gauges. */
    public int color() {
        return this.color;
    }

    public Component label() {
        return Component.translatable("gas.nerospace." + this.name);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /** Parse a serialized name back to a constant (defaults to {@link #EMPTY}). */
    public static GasResource byName(String name) {
        for (GasResource gas : values()) {
            if (gas.name.equals(name)) {
                return gas;
            }
        }
        return EMPTY;
    }
}
