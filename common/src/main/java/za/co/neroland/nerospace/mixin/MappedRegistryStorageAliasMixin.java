package za.co.neroland.nerospace.mixin;

import java.util.Optional;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cross-loader world-compat alias for the storage blocks that moved from Nerospace
 * into Neroland Core (Battery / Fluid Tank / Gas Tank / Item Store + Creative
 * variants). Forge handles this via its {@code MissingMappingsEvent}; modern
 * NeoForge removed that event and Fabric never had one, so on those loaders we
 * remap the eight old {@code nerospace:*} ids to their {@code nerolandcore:*}
 * equivalents at the registry lookup the chunk/block-entity loaders use.
 *
 * <p>The chunk block-state decoder resolves a block's {@code Name} through the
 * registry's <b>Holder</b> lookup ({@code get(ResourceKey)} → {@code Optional<Holder.Reference>}),
 * not the value lookup, so that is the primary target; {@code getValue(Identifier)}
 * is also hooked for direct value lookups (commands, etc.). Block-entity types are
 * remapped too so a migrated block keeps its stored contents. Strictly scoped to the
 * BLOCK / ITEM / BLOCK_ENTITY_TYPE registries and the eight moved ids; everything
 * else is untouched.
 *
 * <p>{@code require = 0}: if a signature ever fails to match, the injector is simply
 * skipped (no crash) and behaviour falls back to the default (re-place the block).
 */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryStorageAliasMixin<T> implements Registry<T> {

    private static final String NEROSPACE_NS = "nerospace";
    private static final String CORE_NS = "nerolandcore";

    private static final Set<String> MOVED = Set.of(
            "battery", "fluid_tank", "gas_tank", "item_store",
            "creative_battery", "creative_fluid_tank", "creative_gas_tank", "creative_item_store",
            "trash_can");

    /** Holder lookup by key — the path the chunk block-state decoder uses. */
    @Inject(method = "get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void nerospace$aliasHolderByKey(ResourceKey<T> key, CallbackInfoReturnable<Optional<Holder.Reference<T>>> cir) {
        Identifier coreId = nerospace$coreIdFor(key.identifier());
        if (coreId != null) {
            cir.setReturnValue(this.get(ResourceKey.create(this.key(), coreId)));
        }
    }

    /** Holder lookup by id (the by-id default funnels here on some paths). */
    @Inject(method = "get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void nerospace$aliasHolderById(Identifier id, CallbackInfoReturnable<Optional<Holder.Reference<T>>> cir) {
        Identifier coreId = nerospace$coreIdFor(id);
        if (coreId != null) {
            cir.setReturnValue(this.get(ResourceKey.create(this.key(), coreId)));
        }
    }

    /** Value lookup by id — direct registry queries (commands, code). */
    @Inject(method = "getValue(Lnet/minecraft/resources/Identifier;)Ljava/lang/Object;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void nerospace$aliasValueById(Identifier id, CallbackInfoReturnable<T> cir) {
        Identifier coreId = nerospace$coreIdFor(id);
        if (coreId != null) {
            cir.setReturnValue(this.getValue(coreId));
        }
    }

    /**
     * If {@code id} is one of the moved {@code nerospace:*} ids AND this is the
     * BLOCK / ITEM / BLOCK_ENTITY_TYPE registry, return the {@code nerolandcore:}
     * equivalent id; otherwise {@code null} (leave the lookup untouched).
     */
    private Identifier nerospace$coreIdFor(Identifier id) {
        if (id == null || !NEROSPACE_NS.equals(id.getNamespace()) || !MOVED.contains(id.getPath())) {
            return null;
        }
        ResourceKey<?> registryKey = this.key();
        if (registryKey != Registries.BLOCK && registryKey != Registries.ITEM
                && registryKey != Registries.BLOCK_ENTITY_TYPE) {
            return null;
        }
        return Identifier.fromNamespaceAndPath(CORE_NS, id.getPath());
    }
}
