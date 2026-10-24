package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

/**
 * Custom tag keys for Nerospace. Cross-mod material tags live in the {@code c} namespace (unified with
 * Fabric); mod-specific behaviour tags (oxygen sealing, terraform conversion) live in {@code nerospace}.
 * Pure {@link TagKey} constants — tag membership is data ({@code tags/**.json}).
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

    private static TagKey<Biome> biomeTag(String namespace, String path) {
        return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(namespace, path));
    }

    public static final class Biomes {

        private Biomes() {
        }

        // --- Per-biome gravity overrides (GRAVITY_DESIGN.md) ----------------
        // A biome carrying one of these tags overrides its dimension's default gravity factor. Most
        // biomes carry none and simply inherit the dimension default. Membership is data
        // (tags/worldgen/biome/*.json).
        /** Micro-gravity biomes (~0.15× — craters, voids). */
        public static final TagKey<Biome> GRAVITY_MICRO = biomeTag("nerospace", "gravity_micro");
        /** Low-gravity biomes (~0.4×). */
        public static final TagKey<Biome> GRAVITY_LOW = biomeTag("nerospace", "gravity_low");
        /** High-gravity biomes (~1.5× — dense basins). */
        public static final TagKey<Biome> GRAVITY_HIGH = biomeTag("nerospace", "gravity_high");

        // --- Per-biome hazard overrides (2.0) ---------------------------------
        // A biome carrying one of these replaces its planet's primary hazard at that position
        // (NerospacePlanets.hazardAt). Radiation outranks heat, heat outranks cold.
        public static final TagKey<Biome> HAZARD_RADIATION = biomeTag("nerospace", "hazard/radiation");
        public static final TagKey<Biome> HAZARD_HEAT = biomeTag("nerospace", "hazard/heat");
        public static final TagKey<Biome> HAZARD_COLD = biomeTag("nerospace", "hazard/cold");
    }

    public static final class Blocks {

        private Blocks() {
        }

        /** The common "all ores" convention tag (any mod's ores) — used by the Xertz Resonator's ore ping. */
        public static final TagKey<Block> ORES = blockTag("c", "ores");

        public static final TagKey<Block> ORES_NEROSIUM = blockTag("c", "ores/nerosium");
        public static final TagKey<Block> STORAGE_BLOCKS_NEROSIUM = blockTag("c", "storage_blocks/nerosium");
        public static final TagKey<Block> STORAGE_BLOCKS_RAW_NEROSIUM = blockTag("c", "storage_blocks/raw_nerosium");

        public static final TagKey<Block> ORES_NEROSTEEL = blockTag("c", "ores/nerosteel");
        public static final TagKey<Block> ORES_XERTZ_QUARTZ = blockTag("c", "ores/xertz_quartz");
        public static final TagKey<Block> STORAGE_BLOCKS_NEROSTEEL = blockTag("c", "storage_blocks/nerosteel");

        public static final TagKey<Block> ORES_CINDRITE = blockTag("c", "ores/cindrite");
        public static final TagKey<Block> STORAGE_BLOCKS_CINDRITE = blockTag("c", "storage_blocks/cindrite");

        public static final TagKey<Block> ORES_GLACITE = blockTag("c", "ores/glacite");
        public static final TagKey<Block> STORAGE_BLOCKS_GLACITE = blockTag("c", "storage_blocks/glacite");

        // --- Oxygen field (terraform design) -------------------------------
        /** Full, airtight blocks: stop ALL oxygen flow (opaque cubes, glass, station walls). */
        public static final TagKey<Block> OXYGEN_SEALING = blockTag("nerospace", "oxygen_sealing");
        /** Non-full / leaky blocks: allow PARTIAL flow (fences, slabs, torches, open trapdoors). */
        public static final TagKey<Block> OXYGEN_LEAKS = blockTag("nerospace", "oxygen_leaks");
        /**
         * Airtight hull (decision 002 §10.5): a subset of {@link #OXYGEN_SEALING} — Hull Plating and Hull Window
         * only. The oxygen field treats it as a seal; the Station Console counts it for "pressurised".
         */
        public static final TagKey<Block> AIRTIGHT = blockTag("nerospace", "airtight");
        /** Tops that cancel a Cindara ember strike (Basalt Spire). */
        public static final TagKey<Block> LIGHTNING_SAFE = blockTag("nerospace", "lightning_safe");
        /** Ground a Hydration Module can never thaw (Permafrost). */
        public static final TagKey<Block> HYDRATION_IMMUNE = blockTag("nerospace", "hydration_immune");
        /** The only substrate Xertz crops and Spore Moss root in (Xertz Soil). */
        public static final TagKey<Block> XERTZ_ROOTABLE = blockTag("nerospace", "xertz_rootable");
        /** Blocks a beam can be deflected by (Void Crystal); additive API, see {@code api.optics.BeamRefraction}. */
        public static final TagKey<Block> BEAM_REFRACTORS = blockTag("nerospace", "beam_refractors");
        /** Blocks custom lava interactions must never replace (Magma Glass). */
        public static final TagKey<Block> LAVA_IMMUNE = blockTag("nerospace", "lava_immune");
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

        public static final TagKey<Item> ORES_NEROSTEEL = itemTag("c", "ores/nerosteel");
        public static final TagKey<Item> ORES_XERTZ_QUARTZ = itemTag("c", "ores/xertz_quartz");
        public static final TagKey<Item> INGOTS_NEROSTEEL = itemTag("c", "ingots/nerosteel");
        public static final TagKey<Item> RAW_MATERIALS_NEROSTEEL = itemTag("c", "raw_materials/nerosteel");
        public static final TagKey<Item> STORAGE_BLOCKS_NEROSTEEL = itemTag("c", "storage_blocks/nerosteel");
        public static final TagKey<Item> GEMS_XERTZ_QUARTZ = itemTag("c", "gems/xertz_quartz");

        public static final TagKey<Item> GEMS_CINDRITE = itemTag("c", "gems/cindrite");
        public static final TagKey<Item> ORES_CINDRITE = itemTag("c", "ores/cindrite");
        public static final TagKey<Item> STORAGE_BLOCKS_CINDRITE = itemTag("c", "storage_blocks/cindrite");

        public static final TagKey<Item> GEMS_GLACITE = itemTag("c", "gems/glacite");
        public static final TagKey<Item> ORES_GLACITE = itemTag("c", "ores/glacite");
        public static final TagKey<Item> STORAGE_BLOCKS_GLACITE = itemTag("c", "storage_blocks/glacite");

        /** Items the Hydration Module melts into hydration units for the Terraformer's water stage. */
        public static final TagKey<Item> HYDRATION_INPUT = itemTag("nerospace", "hydration_input");

        /** Tiered "alien" meteor loot, grouped for the future scanner/upgrade system. */
        public static final TagKey<Item> ALIEN_MATERIALS = itemTag("nerospace", "alien_materials");
    }
}
