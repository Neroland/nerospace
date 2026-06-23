package za.co.neroland.nerospace.registry;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/**
 * Item data components, ported cross-loader through {@link RegistrationProvider} over the vanilla
 * {@code DATA_COMPONENT_TYPE} registry (the root used a NeoForge {@code DeferredRegister.DataComponents}).
 *
 * <p>Cross-loader port note: the root's {@code FILTER_ITEM} stored a NeoForge-transfer {@code ItemResource};
 * the multiloader uses a vanilla {@link ItemStack} (the advanced-pipe filter that consumes it is ported on
 * the cross-loader item model). These back the Configurator + Pipe Filter (advanced pipes batch).</p>
 */
public final class ModDataComponents {

    public static final RegistrationProvider<DataComponentType<?>> COMPONENTS =
            RegistrationProvider.get(Registries.DATA_COMPONENT_TYPE, NerospaceCommon.MOD_ID);

    /** Index into the pipe resource layers — which layer the Configurator is editing. */
    public static final RegistryEntry<DataComponentType<Integer>> SELECTED_PIPE_TYPE =
            COMPONENTS.register("selected_pipe_type", key -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, 3))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** The item a Pipe Filter is set to (applied to pipe faces to restrict the item layer). */
    public static final RegistryEntry<DataComponentType<ItemStack>> FILTER_ITEM =
            COMPONENTS.register("filter_item", key -> DataComponentType.<ItemStack>builder()
                    .persistent(ItemStack.CODEC)
                    .networkSynchronized(ItemStack.STREAM_CODEC)
                    .build());

    private ModDataComponents() {
    }

    public static void init() {
    }
}
