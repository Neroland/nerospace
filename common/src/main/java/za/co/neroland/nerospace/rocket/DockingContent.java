package za.co.neroland.nerospace.rocket;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/**
 * The Docking Ring's block and item (pickaxe/iron, 3.0 / 12.0, light 6, METAL). Registered from
 * the shared registry init after {@code ModBlocks}/{@code ModItems}.
 */
public final class DockingContent {

    public static final RegistryEntry<DockingRingBlock> DOCKING_RING = ModBlocks.BLOCKS.register("docking_ring",
            (ResourceKey<Block> key) -> new DockingRingBlock(BlockBehaviour.Properties.of().setId(key)
                    .mapColor(MapColor.COLOR_ORANGE).strength(3.0F, 12.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.METAL).noOcclusion().randomTicks().lightLevel(s -> 6)));

    public static final RegistryEntry<BlockItem> DOCKING_RING_ITEM = ModItems.ITEMS.register("docking_ring",
            key -> new BlockItem(DOCKING_RING.get(), new Item.Properties().setId(key)));

    private DockingContent() {
    }

    /**
     * Registers the content and installs the rocket package's hooks into the station code (which has no compile
     * dependency on the rocket package): the real Docking Ring placer and the visitor-rocket sender.
     */
    public static void init() {
        za.co.neroland.nerospace.world.station.StationModules.setDockPlacer(DockingRingBlock::placeRing);
        za.co.neroland.nerospace.world.station.StationHooks.setVisitorSender(StationVisitorSender::sendHome);
    }
}
