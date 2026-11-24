package za.co.neroland.nerospace.registry;

//? if >=1.20.5 {
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.pipe.FaceFilter;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

// Item data components, ported cross-loader through {@link RegistrationProvider} over the vanilla
// {@code DATA_COMPONENT_TYPE} registry (the root used a NeoForge {@code DeferredRegister.DataComponents}).
//
// <p>Cross-loader port note: the root's {@code FILTER_ITEM} stored a NeoForge-transfer {@code ItemResource};
// the multiloader uses a vanilla {@link ItemStack} (the advanced-pipe filter that consumes it is ported on
// the cross-loader item model). These back the Configurator + Pipe Filter (advanced pipes batch).</p>
public final class ModDataComponents {

    public static final RegistrationProvider<DataComponentType<?>> COMPONENTS =
            RegistrationProvider.get(Registries.DATA_COMPONENT_TYPE, NerospaceCommon.MOD_ID);

    // Index into the pipe resource layers — which layer the Configurator is editing.
    public static final RegistryEntry<DataComponentType<Integer>> SELECTED_PIPE_TYPE =
            COMPONENTS.register("selected_pipe_type", key -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, 3))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    // The item a Pipe Filter is set to (applied to pipe faces to restrict the item layer).
    public static final RegistryEntry<DataComponentType<ItemStack>> FILTER_ITEM =
            COMPONENTS.register("filter_item", key -> DataComponentType.<ItemStack>builder()
                    .persistent(ItemStack.CODEC)
                    .networkSynchronized(ItemStack.STREAM_CODEC)
                    .build());

    // The multi-entry filter configured on an Advanced Pipe Filter (issue #25). Network
// sync falls back to the persistent codec (no explicit stream codec needed).
    public static final RegistryEntry<DataComponentType<FaceFilter>> ADVANCED_FILTER =
            COMPONENTS.register("advanced_filter", key -> DataComponentType.<FaceFilter>builder()
                    .persistent(FaceFilter.CODEC)
                    .build());

    // Rad Lining upgrade on an Oxygen Suit piece (applied at a smithing table to a T2, heat or cryo piece; the item
// id and every other behaviour stay as they were). A full suit of lined pieces shields RADIATION.
    public static final RegistryEntry<DataComponentType<Unit>> RAD_LINING =
            COMPONENTS.register("rad_lining", key -> DataComponentType.<Unit>builder()
                    //? if >=1.21 {
                    .persistent(Unit.CODEC)
                    //?} else {
                    /*.persistent(Codec.unit(Unit.INSTANCE))
                    *///?}
                    .networkSynchronized(Unit.STREAM_CODEC)
                    .build());

    // 2.1: the charge (FE) a packed-up Mars rover keeps on its {@code rover} item, restored when it is placed again.
    public static final RegistryEntry<DataComponentType<Integer>> ROVER_ENERGY =
            COMPONENTS.register("rover_energy", key -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, Integer.MAX_VALUE))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    private ModDataComponents() {
    }

    public static void init() {
    }
}
//?} else {
/*import com.mojang.serialization.Codec;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.pipe.FaceFilter;

public final class ModDataComponents {
    public static final LegacyComponent<Integer> SELECTED_PIPE_TYPE =
            new LegacyComponent<>("nerospace:selected_pipe_type", Codec.intRange(0, 3));
    public static final LegacyComponent<ItemStack> FILTER_ITEM =
            new LegacyComponent<>("nerospace:filter_item", ItemStack.CODEC);
    public static final LegacyComponent<FaceFilter> ADVANCED_FILTER =
            new LegacyComponent<>("nerospace:advanced_filter", FaceFilter.CODEC);
    public static final LegacyComponent<Unit> RAD_LINING =
            new LegacyComponent<>("nerospace:rad_lining", Codec.unit(Unit.INSTANCE));
    public static final LegacyComponent<Integer> ROVER_ENERGY =
            new LegacyComponent<>("nerospace:rover_energy", Codec.intRange(0, Integer.MAX_VALUE));

    private ModDataComponents() {
    }

    public static void init() {
    }

    public record LegacyComponent<T>(String key, Codec<T> codec) {
        public T getOrDefault(ItemStack stack, T fallback) {
            Tag tag = stack.getTag() == null ? null : stack.getTag().get(key);
            return tag == null ? fallback : codec.parse(NbtOps.INSTANCE, tag).result().orElse(fallback);
        }

        public void set(ItemStack stack, T value) {
            Tag encoded = codec.encodeStart(NbtOps.INSTANCE, value).result()
                    .orElseThrow(() -> new IllegalArgumentException("Could not encode item key " + key));
            stack.getOrCreateTag().put(key, encoded);
        }

        public void remove(ItemStack stack) {
            stack.removeTagKey(key);
        }

        public boolean has(ItemStack stack) {
            return stack.getTag() != null && stack.getTag().contains(key);
        }
    }
}
*///?}
