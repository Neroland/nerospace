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

        // Phase 3 — Greenxertz ores.
        public static final TagKey<Block> ORES_NEROSTEEL = blockTag("c", "ores/nerosteel");
        public static final TagKey<Block> ORES_XERTZ_QUARTZ = blockTag("c", "ores/xertz_quartz");
        public static final TagKey<Block> STORAGE_BLOCKS_NEROSTEEL = blockTag("c", "storage_blocks/nerosteel");

        // Glacira ore (NEW_DESTINATION_DESIGN.md).
        public static final TagKey<Block> ORES_GLACITE = blockTag("c", "ores/glacite");
        public static final TagKey<Block> STORAGE_BLOCKS_GLACITE = blockTag("c", "storage_blocks/glacite");

        // --- Oxygen field (terraform design) -------------------------------
        /** Full, airtight blocks: stop ALL oxygen flow (opaque cubes, glass, station walls). */
        public static final TagKey<Block> OXYGEN_SEALING = blockTag("nerospace", "oxygen_sealing");
        /** Non-full / leaky blocks: allow PARTIAL flow (fences, slabs, torches, open trapdoors). */
        public static final TagKey<Block> OXYGEN_LEAKS = blockTag("nerospace", "oxygen_leaks");
        /** Blocks that act as oxygen sources for the field (generators; later: alien flora). */
        public static final TagKey<Block> OXYGEN_SOURCE = blockTag("nerospace", "oxygen_source");

        // --- Terraform conversion table (data-driven) ----------------------
        /** Surface blocks a Terraformer turns into grass (deadrock, basalt, dirt, sand, …). */
        public static final TagKey<Block> TERRAFORM_TO_GRASS = blockTag("nerospace", "terraform_to_grass");
        /** Sub-surface blocks a Terraformer turns into dirt. */
        public static final TagKey<Block> TERRAFORM_TO_DIRT = blockTag("nerospace", "terraform_to_dirt");
    }

    public static final class Items {

        private Items() {
        }

        public static final TagKey<Item> ORES_NEROSIUM = itemTag("c", "ores/nerosium");
        public static final TagKey<Item> INGOTS_NEROSIUM = itemTag("c", "ingots/nerosium");
        public static final TagKey<Item> DUSTS_NEROSIUM = itemTag("c", "dusts/nerosium");
        public static final TagKey<Item> RAW_MATERIALS_NEROSIUM = itemTag("c", "raw_materials/nerosium");
        public static final TagKey<Item> STORAGE_BLOCKS_NEROSIUM = itemTag("c", "storage_blocks/nerosium");
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_NEROSIUM = itemTag("c", "storage_blocks/raw_nerosium");

        // Phase 3 — Greenxertz materials.
        public static final TagKey<Item> ORES_NEROSTEEL = itemTag("c", "ores/nerosteel");
        public static final TagKey<Item> ORES_XERTZ_QUARTZ = itemTag("c", "ores/xertz_quartz");
        public static final TagKey<Item> INGOTS_NEROSTEEL = itemTag("c", "ingots/nerosteel");
        public static final TagKey<Item> RAW_MATERIALS_NEROSTEEL = itemTag("c", "raw_materials/nerosteel");
        public static final TagKey<Item> STORAGE_BLOCKS_NEROSTEEL = itemTag("c", "storage_blocks/nerosteel");
        /** Mirrors {@code c:gems/quartz}; xertz quartz is a gem-style drop. */
        public static final TagKey<Item> GEMS_XERTZ_QUARTZ = itemTag("c", "gems/xertz_quartz");

        // Phase 7a — Cindara materials.
        /** Cindrite shards (repair material of the Tier 2 Oxygen Suit). */
        public static final TagKey<Item> GEMS_CINDRITE = itemTag("c", "gems/cindrite");

        // Glacira materials (NEW_DESTINATION_DESIGN.md).
        /** Glacite crystals (future cold-suit repair / terraforming water-cycle feedstock). */
        public static final TagKey<Item> GEMS_GLACITE = itemTag("c", "gems/glacite");

        // Deeper terraforming (DEEPER_TERRAFORM_DESIGN.md §3.1).
        /**
         * Items the Hydration Module melts into hydration units for the Terraformer's water stage.
         * Glacite-only by default (vanilla ice would bypass the Glacira gate) — the tag lets packs
         * widen it.
         */
        public static final TagKey<Item> HYDRATION_INPUT = itemTag("nerospace", "hydration_input");
    }
}
