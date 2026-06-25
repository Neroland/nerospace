package za.co.neroland.nerospace.pipe;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * The four resource layers a Universal Pipe carries simultaneously over its one connection graph.
 * Each has a display colour (ARGB) used by the streams in the pipe renderer and the Configurator UI.
 *
 * <p>Cross-loader port: pure vanilla ({@link StringRepresentable}/{@link Component}); identical to the
 * standalone mod. Note: the multiloader relay currently moves energy, gas and items — the {@code FLUID}
 * layer is reserved (its per-face mode is stored but inert until the fluid relay lands).</p>
 */
public enum PipeResourceType implements StringRepresentable {
    ENERGY("energy", 0xFFE0506A),  // red — FE
    FLUID("fluid", 0xFF3C78F0),    // blue
    GAS("gas", 0xFF78D2F0),        // O₂ cyan
    ITEM("item", 0xFFE8E8F4);      // white — items render as themselves

    public static final PipeResourceType[] VALUES = values();

    private final String name;
    private final int color;

    PipeResourceType(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public int color() {
        return this.color;
    }

    public Component label() {
        return Component.translatable("pipe.nerospace.type." + this.name);
    }

    @Override
    public @NonNull String getSerializedName() {
        return NerospaceCommon.requireNonNull(this.name);
    }
}
