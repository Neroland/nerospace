package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Custom tag keys for Nerospace.
 *
 * <p>Per NeoForge convention, cross-mod material tags live in the {@code c} namespace
 * (unified with Fabric) so that future tech-mod integration (Mekanism, etc.) is mostly free.
 * Broad parent tags such as {@code c:ingots} or {@code c:ores} are referenced directly via
 * {@code net.neoforged.neoforge.common.Tags} in the data providers.</p>
 */
public final class ModTags {

    private ModTags() {
    }

    private static TagKey<Block> blockTag(String namespace, String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(namespace, path));
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(namespace, path));
    }

    public static final class Blocks {

        private Blocks() {
        }

        public static final TagKey<Block> ORES_NEROSIUM = blockTag("c", "ores/nerosium");
        public static final TagKey<Block> STORAGE_BLOCKS_NEROSIUM = blockTag("c", "storage_blocks/nerosium");
        public static final TagKey<Block> STORAGE_BLOCKS_RAW_NEROSIUM = blockTag("c", "storage_blocks/raw_nerosium");
    }

    public static final class Items {

        private Items() {
        }

        public static final TagKey<Item> ORES_NEROSIUM = itemTag("c", "ores/nerosium");
        public static final TagKey<Item> INGOTS_NEROSIUM = itemTag("c", "ingots/nerosium");
        public static final TagKey<Item> RAW_MATERIALS_NEROSIUM = itemTag("c", "raw_materials/nerosium");
        public static final TagKey<Item> STORAGE_BLOCKS_NEROSIUM = itemTag("c", "storage_blocks/nerosium");
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_NEROSIUM = itemTag("c", "storage_blocks/raw_nerosium");
    }
}
