package za.co.neroland.nerospace.gas;

import com.mojang.serialization.Codec;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.transfer.resource.Resource;

/**
 * A gas the mod's logistics can store and move — the resource side of the dedicated gas layer, built
 * on the same NeoForge transfer framework as fluids/items so handlers, transactions and
 * {@code ResourceHandlerUtil} all work unchanged. Oxygen is the first gas; more (hydrogen, fuel
 * vapour, ...) can be added as new constants without touching the transport.
 */
public enum GasResource implements Resource, StringRepresentable {
    EMPTY("empty", 0x00000000),
    OXYGEN("oxygen", 0xFF54D46A);

    public static final Codec<GasResource> CODEC = StringRepresentable.fromEnum(GasResource::values);

    private final String name;
    private final int color;

    GasResource(String name, int color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public boolean isEmpty() {
        return this == EMPTY;
    }

    /** Display colour (ARGB) for streams, gauges and the Configurator UI. */
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
}
