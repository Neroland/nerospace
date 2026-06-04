package za.co.neroland.nerospace.registry;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;

/**
 * Item data components for Nerospace. Currently holds the Configurator's <b>selected resource type</b>
 * (an ordinal into {@code PipeResourceType}) so the tool can target one of the four pipe layers at a
 * time when cycling a face's I/O mode.
 */
public final class ModDataComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Nerospace.MODID);

    /** Index into {@code PipeResourceType.VALUES} — which layer the Configurator is editing. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SELECTED_PIPE_TYPE =
            COMPONENTS.registerComponentType("selected_pipe_type", builder -> builder
                    .persistent(Codec.intRange(0, 3))
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** The item a Pipe Filter is set to (applied to pipe faces to restrict the item layer). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.neoforged.neoforge.transfer.item.ItemResource>> FILTER_ITEM =
            COMPONENTS.registerComponentType("filter_item", builder -> builder
                    .persistent(net.neoforged.neoforge.transfer.item.ItemResource.CODEC)
                    .networkSynchronized(net.neoforged.neoforge.transfer.item.ItemResource.STREAM_CODEC));

    private ModDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
