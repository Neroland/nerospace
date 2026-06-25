package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/**
 * The single dedicated Nerospace creative tab, registered cross-loader via {@link RegistrationProvider}
 * over the vanilla {@code CREATIVE_MODE_TAB} registry (the root mod's approach).
 *
 * <p>The earlier multiloader scattered items into the vanilla tabs through each loader's own injection
 * API ({@code BuildCreativeModeTabContentsEvent} on NeoForge, {@code CreativeModeTabEvents} on Fabric);
 * that never actually populated the tabs at runtime (the multiloader had only ever been build-verified).
 * A dedicated tab built via {@code CreativeModeTab.builder().displayItems(...)} is plain vanilla and so
 * works identically on both loaders, mirroring the root mod, and shows a proper "Nerospace" tab.</p>
 */
public final class ModCreativeTab {

    public static final RegistrationProvider<CreativeModeTab> TABS =
            RegistrationProvider.get(Registries.CREATIVE_MODE_TAB, NerospaceCommon.MOD_ID);

    // NOTE: vanilla CreativeModeTab.builder takes (Row, column) — the no-arg overload is NeoForge-only;
    // likewise withTabsBefore/After are NeoForge extensions, so neither is used here (common = raw vanilla).
    public static final RegistryEntry<CreativeModeTab> NEROSPACE = TABS.register("nerospace",
            key -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.nerospace"))
                    .icon(() -> new ItemStack(ModItems.NEROSIUM_INGOT.get()))
                    .displayItems((params, output) -> ModItems.creativeContents().forEach(
                            item -> output.accept(NerospaceCommon.requireNonNull(item))))
                    .build());

    private ModCreativeTab() {
    }

    public static void init() {
    }
}
