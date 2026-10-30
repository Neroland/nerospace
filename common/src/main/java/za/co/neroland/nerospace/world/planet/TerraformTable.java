package za.co.neroland.nerospace.world.planet;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * One planet's terraform payoff: what each stage of the Rooted → Hydrated → Living pipeline writes
 * on that world. {@code TerraformConversion} reads this table instead of branching on the dimension.
 *
 * <p>Plain data on purpose: block, entity, particle and feature references are ids resolved against the live
 * registries at conversion time, so the table is available during static init and in unit tests. An id that does
 * not resolve (content not registered yet) is skipped, never substituted.</p>
 *
 * @param langId       the planet's short id ({@code luna}, {@code mars} …) — also the Monitor lang-key segment
 * @param rooted       stage-1 biome
 * @param hydrated     stage-2 biome
 * @param living       stage-3 biome (the planet's {@code terraformTarget})
 * @param surfaceTop   stage-1 surface block written over {@code #nerospace:terraform_to_grass}
 * @param surfaceSub   stage-1 sub-surface block written over {@code #nerospace:terraform_to_dirt} (3 deep)
 * @param extraSurface the planet's own ground blocks that convert like the tagged ones (regolith, nyx stone …)
 * @param livingTop    optional stage-3 surface block written over the stage-1 top (Greenxertz: Xertz Soil)
 * @param liquid       the stage-2 hydration liquid (a still source block)
 * @param flora        stage-1 ground-cover scatter, weighted
 * @param livingFlora  stage-3 ground cover on the living top, weighted (may be empty)
 * @param fauna        stage-3 starter-herd species (entity ids; the first that resolves is used)
 * @param ores         tier-3 ore seed set (block ids)
 * @param treeFeature  optional feature grown instead of the planet's vanilla tree family on part of the rolls
 * @param treeChance   multiplier on the Living tree chance (0 = no trees)
 * @param particle     frontier particle (a simple particle type id)
 * @param rules        planet-specific rules
 */
public record TerraformTable(
        String langId,
        ResourceKey<Biome> rooted,
        ResourceKey<Biome> hydrated,
        ResourceKey<Biome> living,
        Identifier surfaceTop,
        Identifier surfaceSub,
        List<Identifier> extraSurface,
        Optional<Identifier> livingTop,
        Identifier liquid,
        List<Weighted> flora,
        List<Weighted> livingFlora,
        List<Identifier> fauna,
        List<Identifier> ores,
        Optional<Identifier> treeFeature,
        double treeChance,
        Identifier particle,
        List<Rule> rules) {

    /** A weighted id (flora scatter). */
    public record Weighted(Identifier id, int weight) {
    }

    /** Planet-specific behaviour switches (each read by exactly one place in the conversion). */
    public enum Rule {
        /** Nyxara: roots only in {@code #nerospace:nyxara_band}; refuses on the dayside and nightside. */
        BAND_ONLY,
        /** Luna: the Living edge glows — a shimmer-dome particle ring at the terraformer's radius (no block). */
        SHIMMER_DOME,
        /** Mars: stage 2 opens one spring per chunk at the chunk's lowest surface cell (first open water). */
        OPEN_WATER_SPRING
    }

    public TerraformTable {
        extraSurface = List.copyOf(extraSurface);
        flora = List.copyOf(flora);
        livingFlora = List.copyOf(livingFlora);
        fauna = List.copyOf(fauna);
        ores = List.copyOf(ores);
        rules = List.copyOf(rules);
        if (treeChance < 0) {
            throw new IllegalArgumentException("treeChance must be >= 0");
        }
    }

    public boolean has(Rule rule) {
        return this.rules.contains(rule);
    }

    /** The biome written by {@code stage} (1..3); stage 0 or below has none. */
    public Optional<ResourceKey<Biome>> stageBiome(int stage) {
        return switch (stage) {
            case 1 -> Optional.of(this.rooted);
            case 2 -> Optional.of(this.hydrated);
            case 3 -> Optional.of(this.living);
            default -> Optional.empty();
        };
    }

    /** Whether {@code biome} is one of this table's three stage biomes. */
    public boolean isTrioBiome(ResourceKey<Biome> biome) {
        return this.rooted.equals(biome) || this.hydrated.equals(biome) || this.living.equals(biome);
    }

    /** Picks a weighted entry for {@code roll} in {@code [0, totalWeight)}; empty list → empty. */
    public static Optional<Identifier> pick(List<Weighted> entries, int roll) {
        int r = roll;
        for (Weighted w : entries) {
            if (r < w.weight()) {
                return Optional.of(w.id());
            }
            r -= w.weight();
        }
        return Optional.empty();
    }

    public static int totalWeight(List<Weighted> entries) {
        int total = 0;
        for (Weighted w : entries) {
            total += w.weight();
        }
        return total;
    }

    // --- builder helpers for the per-planet tables ------------------------------------------------

    public static Identifier mc(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    public static Identifier ns(String path) {
        return Identifier.fromNamespaceAndPath("nerospace", path);
    }

    public static ResourceKey<Biome> biome(String path) {
        return ResourceKey.create(Registries.BIOME, ns(path));
    }

    public static Weighted w(Identifier id, int weight) {
        return new Weighted(id, weight);
    }

    /** The 1.x stage-1 scatter (6 % sapling, 24 % four flowers, 70 % short grass) — kept for the 1.x planets. */
    public static List<Weighted> legacyFlora() {
        return List.of(w(mc("oak_sapling"), 6), w(mc("poppy"), 6), w(mc("dandelion"), 6), w(mc("cornflower"), 6),
                w(mc("azure_bluet"), 6), w(mc("short_grass"), 70));
    }

    /** The 1.x tier-3 ore seed list. */
    public static List<Identifier> legacyOres() {
        return List.of(ns("nerosteel_ore"), ns("xertz_quartz_ore"), ns("nerosium_ore"));
    }
}
