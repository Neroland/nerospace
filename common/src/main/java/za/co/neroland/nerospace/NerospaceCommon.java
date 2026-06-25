package za.co.neroland.nerospace;

import java.util.Objects;
import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import za.co.neroland.nerospace.platform.Services;
import za.co.neroland.nerospace.registry.ModRegistries;

/**
 * Loader-agnostic entry point. Both {@code NerospaceFabric} and
 * {@code NerospaceNeoForge} call {@link #init()} during mod construction.
 * Loader-specific behaviour is reached only through {@link Services}, keeping
 * this module free of {@code net.neoforged.*} / {@code net.fabricmc.*} imports.
 */
public final class NerospaceCommon {

    public static final String MOD_ID = "nerospace";
    public static final Logger LOGGER = LoggerFactory.getLogger("Nerospace");
    public static final Codec<ItemStack> ITEM_STACK_CODEC =
            requireNonNull(ItemStack.OPTIONAL_CODEC.xmap(NerospaceCommon::requireNonNull, stack -> stack));

    private NerospaceCommon() {
    }

    public static Identifier id(String path) {
        return requireNonNull(Identifier.fromNamespaceAndPath(MOD_ID, path));
    }

    public static Identifier id(String namespace, String path) {
        return requireNonNull(Identifier.fromNamespaceAndPath(namespace, path));
    }

    public static <T> T requireNonNull(@Nullable T value) {
        return Objects.requireNonNull(value);
    }

    public static <T extends @Nullable Object> T orElse(
            Optional<? extends T> optional, T fallback) {
        return optional.isPresent() ? requireNonNull(optional.get()) : fallback;
    }

    /** Called once per loader during mod construction. */
    public static void init() {
        LOGGER.info("[Nerospace] common init on platform: {} (dev={})",
                Services.PLATFORM.getPlatformName(),
                Services.PLATFORM.isDevelopmentEnvironment());

        // Shared content registration via the RegistrationProvider seam. On
        // NeoForge this builds DeferredRegisters (the loader entry point then
        // attaches them to the mod bus); on Fabric it registers eagerly.
        ModRegistries.init();
    }
}
