package za.co.neroland.nerospace.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;

/**
 * Central item registry for Nerospace (Phase 1 — materials slice).
 *
 * <p>Static fields initialise in source order, so {@link #NEROSIUM_TOOL_MATERIAL} is declared
 * before any tool that consumes it. Tools in 26.1 are plain {@link Item}s configured through
 * {@code Item.Properties} delegates (e.g. {@code props.pickaxe(...)}).</p>
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nerospace.MODID);

    /**
     * Nerosium sits between iron and diamond: iron-tier mining (reuses
     * {@code minecraft:incorrect_for_iron_tool}), higher durability and a small attack bonus.
     * Repaired with items tagged {@code c:ingots/nerosium}.
     */
    public static final ToolMaterial NEROSIUM_TOOL_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL,
            350,
            7.0F,
            2.5F,
            15,
            ModTags.Items.INGOTS_NEROSIUM);

    // --- Materials ----------------------------------------------------------

    public static final DeferredItem<Item> RAW_NEROSIUM = ITEMS.registerSimpleItem("raw_nerosium");
    public static final DeferredItem<Item> NEROSIUM_INGOT = ITEMS.registerSimpleItem("nerosium_ingot");

    // --- Tools --------------------------------------------------------------

    public static final DeferredItem<Item> NEROSIUM_PICKAXE = ITEMS.registerItem(
            "nerosium_pickaxe",
            props -> new Item(props.pickaxe(NEROSIUM_TOOL_MATERIAL, 1.0F, -2.8F)));

    // --- Block items --------------------------------------------------------

    public static final DeferredItem<BlockItem> NEROSIUM_ORE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.NEROSIUM_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_NEROSIUM_ORE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_NEROSIUM_ORE);
    public static final DeferredItem<BlockItem> NEROSIUM_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.NEROSIUM_BLOCK);
    public static final DeferredItem<BlockItem> RAW_NEROSIUM_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.RAW_NEROSIUM_BLOCK);

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
