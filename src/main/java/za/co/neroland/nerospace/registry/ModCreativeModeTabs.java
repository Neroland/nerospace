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
                        output.accept(ModItems.NEROSIUM_PICKAXE.get());
                        output.accept(ModBlocks.NEROSIUM_ORE.get());
                        output.accept(ModBlocks.DEEPSLATE_NEROSIUM_ORE.get());
                        output.accept(ModBlocks.NEROSIUM_BLOCK.get());
                        output.accept(ModBlocks.RAW_NEROSIUM_BLOCK.get());
                    })
                    .build());

    private ModCreativeModeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
