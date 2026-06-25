package za.co.neroland.nerospace.registry;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import org.jspecify.annotations.NonNull;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Fabric {@link RegistrationProvider.Factory}: registers eagerly via
 * {@code Registry.register}. The factory supplies the entry's {@link ResourceKey}
 * so the value can set its own id before registration.
 *
 * <p>Registered via {@code META-INF/services/
 * za.co.neroland.nerospace.registry.RegistrationProvider$Factory}.
 */
public final class FabricRegistrationFactory implements RegistrationProvider.Factory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> RegistrationProvider<T> create(ResourceKey<? extends Registry<T>> registryKey, String modId) {
        ResourceKey<? extends Registry<T>> nonNullRegistryKey = NerospaceCommon.requireNonNull(registryKey);
        String nonNullModId = NerospaceCommon.requireNonNull(modId);
        Registry<T> registry = NerospaceCommon.requireNonNull(
                (Registry<T>) BuiltInRegistries.REGISTRY.getValue(nonNullRegistryKey.identifier()));
        return new Provider<>(registry, nonNullRegistryKey, nonNullModId);
    }

    private static final class Provider<T> implements RegistrationProvider<T> {

        private final @NonNull Registry<T> registry;
        private final @NonNull ResourceKey<? extends Registry<T>> registryKey;
        private final @NonNull String modId;

        Provider(@NonNull Registry<T> registry, @NonNull ResourceKey<? extends Registry<T>> registryKey,
                @NonNull String modId) {
            this.registry = registry;
            this.registryKey = registryKey;
            this.modId = modId;
        }

        @Override
        public <I extends T> RegistryEntry<I> register(
                String name, Function<@NonNull ResourceKey<T>, I> factory) {
            String nonNullName = NerospaceCommon.requireNonNull(name);
            Identifier id = Identifier.fromNamespaceAndPath(modId, nonNullName);
            ResourceKey<T> key = ResourceKey.create(registryKey, id);
            I value = NerospaceCommon.requireNonNull(factory.apply(key));
            Registry.register(registry, key, value);
            return new RegistryEntry<>() {
                @Override
                public I get() {
                    return value;
                }

                @Override
                public Identifier id() {
                    return id;
                }
            };
        }
    }
}
