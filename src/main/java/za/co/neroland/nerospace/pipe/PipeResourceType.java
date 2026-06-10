package za.co.neroland.nerospace.pipe;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * The four resource layers a Universal Pipe carries simultaneously over its one connection graph.
 * Each has a display colour (ARGB) used by the streams in the pipe renderer and the Configurator UI.
 */
public enum PipeResourceType implements StringRepresentable {
    ENERGY("energy", 0xFFE0506A),  // red — FE
    FLUID("fluid", 0xFF3C78F0),    // blue
    GAS("gas", 0xFF78D2F0),        // O₂ cyan (art overhaul A4: gas reads as oxygen everywhere)
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
    public String getSerializedName() {
        return this.name;
    }
}
