package za.co.neroland.nerospace.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;

/**
 * The single creative tab for Nerospace. Its title key {@code itemGroup.nerospace} is translated
 * via the generated language file.
 */
public final class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Nerospace.MODID);

    public static final Supplier<CreativeModeTab> NEROSPACE_TAB = CREATIVE_MODE_TABS.register(
            "nerospace",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.nerospace"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> new ItemStack(ModItems.NEROSIUM_INGOT.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.RAW_NEROSIUM.get());
                        output.accept(ModItems.NEROSIUM_INGOT.get());
                        output.accept(ModItems.NEROSIUM_DUST.get());
                        output.accept(ModItems.NEROSIUM_PICKAXE.get());
                        output.accept(ModBlocks.NEROSIUM_ORE.get());
                        output.accept(ModBlocks.DEEPSLATE_NEROSIUM_ORE.get());
                        output.accept(ModBlocks.NEROSIUM_BLOCK.get());
                        output.accept(ModBlocks.RAW_NEROSIUM_BLOCK.get());
                        output.accept(ModBlocks.NEROSIUM_GRINDER.get());

                        // Phase 3 — Greenxertz.
                        output.accept(ModItems.RAW_NEROSTEEL.get());
                        output.accept(ModItems.NEROSTEEL_INGOT.get());
                        output.accept(ModItems.XERTZ_QUARTZ.get());
                        output.accept(ModBlocks.NEROSTEEL_ORE.get());
                        output.accept(ModBlocks.XERTZ_QUARTZ_ORE.get());
                        output.accept(ModBlocks.NEROSTEEL_BLOCK.get());

                        // Phase 7 — Cindara.
                        output.accept(ModItems.CINDRITE.get());
                        output.accept(ModBlocks.CINDRITE_ORE.get());
                        output.accept(ModBlocks.CINDRITE_BLOCK.get());

                        // Phase 7c — station.
                        output.accept(ModBlocks.STATION_FLOOR.get());
                        output.accept(ModBlocks.STATION_WALL.get());

                        // Phase 4 — Rockets.
                        output.accept(ModBlocks.ROCKET_LAUNCH_PAD.get());
                        output.accept(ModBlocks.FUEL_TANK.get());
                        output.accept(ModBlocks.OXYGEN_GENERATOR.get());
                        output.accept(ModBlocks.TERRAFORMER.get());
                        output.accept(ModBlocks.COMBUSTION_GENERATOR.get());
                        output.accept(ModBlocks.PASSIVE_GENERATOR.get());
                        output.accept(ModBlocks.UNIVERSAL_PIPE.get());
                        output.accept(ModItems.CONFIGURATOR.get());

                        // Storage endpoints + creative sources.
                        output.accept(ModBlocks.BATTERY.get());
                        output.accept(ModBlocks.FLUID_TANK.get());
                        output.accept(ModBlocks.GAS_TANK.get());
                        output.accept(ModBlocks.ITEM_STORE.get());
                        output.accept(ModBlocks.CREATIVE_BATTERY.get());
                        output.accept(ModBlocks.CREATIVE_FLUID_TANK.get());
                        output.accept(ModBlocks.CREATIVE_GAS_TANK.get());
                        output.accept(ModBlocks.CREATIVE_ITEM_STORE.get());
                        output.accept(ModItems.OXYGEN_SUIT_HELMET.get());
                        output.accept(ModItems.OXYGEN_SUIT_CHESTPLATE.get());
                        output.accept(ModItems.OXYGEN_SUIT_LEGGINGS.get());
                        output.accept(ModItems.OXYGEN_SUIT_BOOTS.get());
                        output.accept(ModItems.ROCKET_FUEL_CANISTER.get());
                        output.accept(ModItems.ROCKET_FUEL_BUCKET.get());
                        output.accept(ModItems.ROCKET_TIER_1.get());
                        output.accept(ModItems.ROCKET_TIER_2.get());
                        output.accept(ModItems.ROCKET_TIER_3.get());

                        // Spawn eggs.
                        output.accept(ModItems.XERTZ_STALKER_SPAWN_EGG.get());
                        output.accept(ModItems.QUARTZ_CRAWLER_SPAWN_EGG.get());
                        output.accept(ModItems.GREENLING_SPAWN_EGG.get());
                        output.accept(ModItems.CINDER_STALKER_SPAWN_EGG.get());

                        // Creative-only travel devices (no survival recipe).
                        output.accept(ModItems.GREENXERTZ_NAVIGATOR.get());
                        output.accept(ModItems.STATION_COMPASS.get());
                        output.accept(ModItems.GREENXERTZ_COMPASS.get());
                        output.accept(ModItems.CINDARA_COMPASS.get());
                    })
                    .build());

    private ModCreativeModeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
