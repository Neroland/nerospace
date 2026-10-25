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
                    .displayItems((params, output) -> {
                        ModItems.creativeContents().forEach(output::accept);
                        output.accept(za.co.neroland.nerospace.rocket.DockingContent.DOCKING_RING_ITEM.get());
                    })
                    .build());

    /**
     * The planets tab: every planet's natural blocks and drops, by planet in roster order (Luna, Mars, ...).
     * Machines stay in the main tab.
     */
    public static final RegistryEntry<CreativeModeTab> PLANETS = TABS.register("planets",
            key -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
                    .title(Component.translatable("itemGroup.nerospace.planets"))
                    .icon(() -> new ItemStack(za.co.neroland.nerospace.world.planet.luna.LunaContent.REGOLITH_ITEM.get()))
                    .displayItems((params, output) -> {
                        za.co.neroland.nerospace.world.planet.luna.LunaContent.creativeContents()
                                .forEach(s -> output.accept(s.get()));
                        za.co.neroland.nerospace.world.planet.mars.MarsContent.creativeContents()
                                .forEach(s -> output.accept(s.get()));
                        za.co.neroland.nerospace.world.planet.cindara.CindaraContent.creativeContents()
                                .forEach(s -> output.accept(s.get()));
                        za.co.neroland.nerospace.world.planet.glacira.GlaciraContent.creativeContents()
                                .forEach(s -> output.accept(s.get()));
                        za.co.neroland.nerospace.world.planet.greenxertz.GreenxertzContent.creativeContents()
                                .forEach(s -> output.accept(s.get()));
                        za.co.neroland.nerospace.world.planet.nyxara.NyxaraContent.creativeContents()
                                .forEach(s -> output.accept(s.get()));
                        za.co.neroland.nerospace.world.station.StationContent.creativeContents()
                                .forEach(s -> output.accept(s.get()));
                    })
                    .build());

    private ModCreativeTab() {
    }

    public static void init() {
    }
}
