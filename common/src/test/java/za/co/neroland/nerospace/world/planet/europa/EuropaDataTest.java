package za.co.neroland.nerospace.world.planet.europa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.machine.GrinderRows;
import za.co.neroland.nerospace.world.planet.europa.block.ShellIceBlock;

/** Every Europa block and item has its assets, drops, tags and name, in both resource trees where they are mirrored. */
class EuropaDataTest {

    /** The isolation tests' pattern: no Europa file may match it. */
    private static final Pattern BANNED = Pattern.compile("(?<!cave_|sloped_)che" + "ese");

    /** Blocks whose normal drop is not the block: a silk-touch alternative first, then the drop. */
    private static final List<String> ORE_STYLE = List.of("deuterium_ice", "brine_salt_ore", "vent_sulfide_ore",
            "oxidant_ice");

    private static boolean file(Path p) {
        return Files.isRegularFile(p);
    }

    private static List<String> values(Path tag) throws IOException {
        List<String> out = new ArrayList<>();
        for (JsonElement e : EuropaFiles.json(tag).getAsJsonArray("values")) {
            out.add(e.isJsonObject() ? e.getAsJsonObject().get("id").getAsString() : e.getAsString());
        }
        return out;
    }

    private static Path tag(String namespace, String rel) {
        return EuropaFiles.commonMain().resolve("resources/data/" + namespace + "/tags/" + rel + ".json");
    }

    @Test
    @DisplayName("the roster matches registration: 21 blocks in order, brine last and item-less, 7 items")
    void roster() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        assertEquals(21, EuropaFiles.BLOCKS.size());
        assertEquals(7, EuropaFiles.ITEMS.size());
        String src = Files.readString(EuropaFiles.commonMain().resolve(
                "java/za/co/neroland/nerospace/world/planet/europa/EuropaContent.java"), StandardCharsets.UTF_8);
        int at = 0;
        for (String id : EuropaFiles.BLOCKS) {
            int found = src.indexOf("(\"" + id + "\",", at);
            assertTrue(found >= 0, id + " is not registered after the block before it (registration order is fixed)");
            at = found;
        }
        for (String id : EuropaFiles.ITEMS) {
            assertTrue(src.contains("\"" + id + "\""), id);
        }
        assertTrue(src.contains("blockNoItem(\"brine\""), "brine has no block item");
        assertFalse(src.contains("blockItem(\"brine\""));
    }

    @Test
    @DisplayName("every block has a blockstate whose models exist; every item form has a definition, model and texture")
    void assets() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        for (String id : EuropaFiles.BLOCKS) {
            Path state = EuropaFiles.assets("blockstates/" + id + ".json");
            assertTrue(file(state), "blockstate " + id);
            for (var variant : EuropaFiles.json(state).getAsJsonObject("variants").entrySet()) {
                String model = variant.getValue().getAsJsonObject().get("model").getAsString();
                assertTrue(model.startsWith("nerospace:block/"), id + " " + model);
                modelAndTextures(model);
            }
            if (id.equals("brine")) {
                assertFalse(file(EuropaFiles.assets("items/brine.json")), "brine has no item");
                continue;
            }
            itemForm(id);
        }
        for (String id : EuropaFiles.ITEMS) {
            itemForm(id);
            assertTrue(file(EuropaFiles.assets("textures/item/" + id + ".png")), "item texture " + id);
        }
        // State variants the behaviour step switches to: their models and textures ship now.
        for (String model : List.of("oxidant_ice_0", "oxidant_ice_1", "oxidant_ice_2", "oxidant_ice_3",
                "europium_lamp_off", "smoker_chimney", "smoker_chimney_capped")) {
            modelAndTextures("nerospace:block/" + model);
        }
        for (String animated : List.of("brine", "tube_mat", "deuterium_ice")) {
            assertTrue(file(EuropaFiles.assets("textures/block/" + animated + ".png.mcmeta")), animated + " animation");
        }
    }

    private static void itemForm(String id) throws IOException {
        Path def = EuropaFiles.assets("items/" + id + ".json");
        assertTrue(file(def), "item definition " + id);
        modelAndTextures(EuropaFiles.json(def).getAsJsonObject("model").get("model").getAsString());
    }

    private static void modelAndTextures(String model) throws IOException {
        Path file = EuropaFiles.assets("models/" + model.substring("nerospace:".length()) + ".json");
        assertTrue(file(file), "model " + model);
        JsonObject textures = EuropaFiles.json(file).getAsJsonObject("textures");
        assertTrue(textures != null && textures.size() > 0, model + " names no texture");
        for (var t : textures.entrySet()) {
            String tex = t.getValue().getAsString();
            assertTrue(tex.startsWith("nerospace:"), model + " " + tex);
            assertTrue(file(EuropaFiles.assets("textures/" + tex.substring("nerospace:".length()) + ".png")),
                    model + " texture " + tex);
        }
    }

    @Test
    @DisplayName("loot: a table per block in both trees (brine has none); the ore-style four carry both alternatives")
    void loot() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        for (String tree : EuropaFiles.TREES) {
            for (String id : EuropaFiles.BLOCKS) {
                Path table = EuropaFiles.data(tree, "loot_table/blocks/" + id + ".json");
                if (id.equals("brine")) {
                    assertFalse(file(table), "brine drops nothing (noLootTable)");
                    continue;
                }
                assertTrue(file(table), tree + " loot " + id);
                JsonObject json = EuropaFiles.json(table);
                assertEquals("minecraft:block", json.get("type").getAsString());
                assertEquals("nerospace:blocks/" + id, json.get("random_sequence").getAsString());
                String text = json.toString();
                assertTrue(text.contains("survives_explosion") || text.contains("explosion_decay")
                        || id.equals("tube_mat"), tree + " " + id + " ignores explosions");
            }
            for (String id : ORE_STYLE) {
                JsonObject json = EuropaFiles.json(EuropaFiles.data(tree, "loot_table/blocks/" + id + ".json"));
                JsonObject entry = json.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries")
                        .get(0).getAsJsonObject();
                assertEquals("minecraft:alternatives", entry.get("type").getAsString(), tree + " " + id);
                JsonArray children = entry.getAsJsonArray("children");
                assertEquals(2, children.size(), tree + " " + id);
                JsonObject silk = children.get(0).getAsJsonObject();
                assertEquals("nerospace:" + id, silk.get("name").getAsString(), tree + " " + id + " silk touch");
                assertTrue(silk.toString().contains("silk_touch"), tree + " " + id);
                JsonObject normal = children.get(1).getAsJsonObject();
                assertFalse(normal.get("name").getAsString().equals("nerospace:" + id), tree + " " + id);
                boolean fortune = normal.toString().contains("minecraft:fortune");
                assertEquals(!id.equals("oxidant_ice"), fortune, tree + " " + id + " fortune");
            }
        }
    }

    @Test
    @DisplayName("tags: tools, shade and sealing sets, the fracture and heat sets, common ore tags")
    void tags() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        List<String> pickaxe = values(tag("minecraft", "block/mineable/pickaxe"));
        for (String id : EuropaFiles.BLOCKS) {
            boolean expected = !List.of("lineae_salt", "tube_mat", "brine").contains(id);
            assertEquals(expected, pickaxe.contains("nerospace:" + id), "pickaxe " + id);
        }
        assertTrue(values(tag("minecraft", "block/mineable/shovel")).contains("nerospace:lineae_salt"));
        assertTrue(values(tag("minecraft", "block/needs_stone_tool")).containsAll(
                List.of("nerospace:shell_ice", "nerospace:brine_salt_ore")));
        assertTrue(values(tag("minecraft", "block/needs_iron_tool")).containsAll(List.of("nerospace:cryovent",
                "nerospace:deuterium_ice", "nerospace:oxidant_ice", "nerospace:vent_sulfide_ore",
                "nerospace:europium_ore")));
        assertEquals(List.of("nerospace:hydrothermal_vent"), values(tag("minecraft", "block/needs_diamond_tool")));

        List<String> shade = values(tag("nerospace", "block/radiation_shade"));
        List<String> ice = List.of("nerospace:europa_ice", "nerospace:chaos_ice", "nerospace:cut_europa_ice",
                "nerospace:europa_ice_bricks", "nerospace:shell_ice");
        assertTrue(shade.containsAll(ice));
        for (String ore : List.of("deuterium_ice", "oxidant_ice", "brine_salt_ore")) {
            assertFalse(shade.contains("nerospace:" + ore), "ore-bearing ice does not shade: " + ore);
        }
        List<String> sealing = values(tag("nerospace", "block/oxygen_sealing"));
        assertTrue(sealing.containsAll(ice));
        assertTrue(sealing.containsAll(List.of("nerospace:vent_basalt", "nerospace:polished_vent_basalt",
                "nerospace:vent_basalt_bricks")));
        for (String v : values(tag("nerospace", "block/airtight"))) {
            assertFalse(EuropaFiles.BLOCKS.contains(v.replace("nerospace:", "")), "nothing Europan is airtight hull: " + v);
        }
        assertEquals(List.of("nerospace:europa_ice", "nerospace:chaos_ice", "nerospace:lineae_salt",
                "nerospace:shell_ice", "nerospace:penitente_blade", "nerospace:brine", "nerospace:deuterium_ice",
                "nerospace:brine_salt_ore", "nerospace:oxidant_ice"),
                values(tag("nerospace", "block/europa_fracture_replaceable")));
        assertEquals(List.of("nerospace:hydrothermal_vent", "minecraft:lava", "minecraft:magma_block",
                "minecraft:campfire", "minecraft:soul_campfire"), values(tag("nerospace", "block/europa_heat_sources")));
        for (String v : values(tag("nerospace", "item/hydration_input"))) {
            assertFalse(EuropaFiles.BLOCKS.contains(v.replace("nerospace:", "")), "no Europa ice feeds hydration: " + v);
        }

        List<String> ores = List.of("deuterium_ice", "oxidant_ice", "brine_salt_ore", "vent_sulfide_ore",
                "europium_ore");
        List<String> kinds = List.of("deuterium", "oxidant", "salt", "sulfide", "europium");
        for (String kind : List.of("block", "item")) {
            List<String> all = values(tag("c", kind + "/ores"));
            for (int i = 0; i < ores.size(); i++) {
                assertTrue(all.contains("nerospace:" + ores.get(i)), kind + " ores " + ores.get(i));
                assertEquals(List.of("nerospace:" + ores.get(i)), values(tag("c", kind + "/ores/" + kinds.get(i))));
            }
        }
        assertTrue(values(tag("c", "item/gems")).containsAll(
                List.of("nerospace:deuterium_shard", "nerospace:oxidant_crystal")));
        assertTrue(values(tag("c", "item/dusts")).contains("nerospace:brine_salt"));
        assertTrue(values(tag("c", "item/raw_materials")).contains("nerospace:raw_vent_sulfide"));
        List<String> highlight = values(tag("neroland", "item/highlight/materials"));
        assertTrue(highlight.containsAll(List.of("nerospace:deuterium_shard", "nerospace:oxidant_crystal",
                "nerospace:brine_salt", "nerospace:raw_vent_sulfide")));
        assertFalse(highlight.contains("nerospace:raw_europium"), "europium stays out of the highlight set");
    }

    /** The 27 recipe files of 2.4.0. */
    private static final List<String> RECIPES = List.of("europa_ice_from_penitente_blades", "cut_europa_ice",
            "cut_europa_ice_from_europa_ice_stonecutting", "cut_europa_ice_from_chaos_ice_stonecutting",
            "cut_europa_ice_from_shell_ice_stonecutting", "europa_ice_bricks",
            "europa_ice_bricks_from_cut_europa_ice_stonecutting", "packed_ice_from_europa_ice_bricks",
            "brine_salt_from_smelting_lineae_salt", "brine_salt_from_brine_bucket",
            "cut_europa_ice_from_cryovent_stonecutting", "vent_basalt_bricks_from_hydrothermal_vent_stonecutting",
            "polished_vent_basalt", "polished_vent_basalt_from_vent_basalt_stonecutting",
            "vent_basalt_bricks_from_vent_basalt_stonecutting", "vent_basalt_bricks",
            "vent_basalt_bricks_from_polished_vent_basalt_stonecutting",
            "smooth_basalt_from_smelting_vent_basalt_bricks", "glowstone_dust_from_tube_mat",
            "raw_europium_from_smelting_europium_ore", "europium_lamp", "raw_europium_from_europium_lamp",
            "deuterium_cell", "rocket_fuel_canister_from_deuterium", "perchlorate_oxidiser_from_brine_salt",
            "copper_ingot_from_smelting_raw_vent_sulfide", "copper_ingot_from_blasting_raw_vent_sulfide");

    /** The three decorative recipes: no unlock advancement. */
    private static final List<String> NO_UNLOCK = List.of("europium_lamp", "raw_europium_from_europium_lamp",
            "raw_europium_from_smelting_europium_ore");

    /** Vanilla ids and tags the recipes may name. */
    private static final List<String> VANILLA = List.of("minecraft:packed_ice", "minecraft:smooth_basalt",
            "minecraft:glowstone_dust", "minecraft:copper_ingot", "minecraft:blaze_powder", "#c:ingots/iron");

    private static List<String> ingredients(JsonObject recipe) {
        List<String> out = new ArrayList<>();
        if (recipe.has("key")) {
            for (var e : recipe.getAsJsonObject("key").entrySet()) {
                out.add(e.getValue().getAsString());
            }
        }
        if (recipe.has("ingredients")) {
            for (JsonElement e : recipe.getAsJsonArray("ingredients")) {
                out.add(e.getAsString());
            }
        }
        if (recipe.has("ingredient")) {
            out.add(recipe.get("ingredient").getAsString());
        }
        return out;
    }

    private static void resolves(String id, String where) {
        if (id.startsWith("nerospace:")) {
            assertTrue(file(EuropaFiles.assets("items/" + id.substring("nerospace:".length()) + ".json")),
                    where + " names an unknown item " + id);
        } else {
            assertTrue(VANILLA.contains(id), where + " names " + id);
        }
    }

    @Test
    @DisplayName("recipes: the 27 files exist in short form, every id resolves, unlocks for all but the decorative three")
    void recipes() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        assertEquals(27, RECIPES.size());
        assertEquals(27, RECIPES.stream().distinct().count());
        for (String name : RECIPES) {
            Path file = EuropaFiles.data("resources", "recipe/" + name + ".json");
            assertTrue(file(file), "recipe " + name);
            JsonObject recipe = EuropaFiles.json(file);
            List<String> in = ingredients(recipe);
            assertFalse(in.isEmpty(), name + " has no ingredient");
            for (String id : in) {
                resolves(id, name);
            }
            resolves(recipe.getAsJsonObject("result").get("id").getAsString(), name);
            String category = recipe.has("category") ? recipe.get("category").getAsString() : "misc";
            Path unlock = EuropaFiles.data("resources", "advancement/recipes/" + category + "/" + name + ".json");
            assertEquals(!NO_UNLOCK.contains(name), file(unlock), "unlock advancement " + name);
        }
        // The bucket recipe hands the bucket back through the item's crafting remainder; no recipe field is needed.
        JsonObject canister = EuropaFiles.json(EuropaFiles.data("resources",
                "recipe/rocket_fuel_canister_from_deuterium.json"));
        assertEquals(2, canister.getAsJsonObject("result").get("count").getAsInt());
        assertEquals(2, ingredients(canister).stream().filter("nerospace:deuterium_cell"::equals).count());
    }

    @Test
    @DisplayName("every block has a recipe use: an ingredient of a recipe file or a grinder row (brine through its bucket)")
    void everyBlockIsUsed() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        List<String> used = new ArrayList<>();
        for (String name : RECIPES) {
            used.addAll(ingredients(EuropaFiles.json(EuropaFiles.data("resources", "recipe/" + name + ".json"))));
        }
        for (GrinderRows.Row row : GrinderRows.EUROPA) {
            used.add(row.input());
        }
        for (String id : EuropaFiles.BLOCKS) {
            String form = id.equals("brine") ? "brine_bucket" : id;
            assertTrue(used.contains("nerospace:" + form), id + " has no recipe use");
        }
        for (String id : List.of("deuterium_shard", "brine_salt", "raw_vent_sulfide", "raw_europium", "deuterium_cell")) {
            assertTrue(used.contains("nerospace:" + id), id + " has no recipe use");
        }
        // The decorative gem enters nothing but the lamp, and no grinder row touches it.
        for (String name : RECIPES) {
            JsonObject recipe = EuropaFiles.json(EuropaFiles.data("resources", "recipe/" + name + ".json"));
            if (!NO_UNLOCK.contains(name)) {
                assertFalse(recipe.toString().contains("europium"), name);
            }
        }
    }

    @Test
    @DisplayName("blockstates name the real properties, and the two message keys exist")
    void states() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        assertEquals(List.of("charge=0", "charge=1", "charge=2", "charge=3"), variantKeys("oxidant_ice"));
        assertEquals(List.of("lit=false", "lit=true"), variantKeys("europium_lamp"));
        assertEquals(List.of("capped=false", "capped=true"), variantKeys("smoker_chimney"));
        assertEquals(List.of("axis=x", "axis=z"), variantKeys("penitente_blade"));
        JsonObject states = EuropaFiles.json(EuropaFiles.assets("blockstates/oxidant_ice.json"))
                .getAsJsonObject("variants");
        for (int charge = 0; charge <= 3; charge++) {
            assertEquals("nerospace:block/oxidant_ice_" + charge,
                    states.getAsJsonObject("charge=" + charge).get("model").getAsString());
        }
        JsonObject lang = EuropaFiles.json(EuropaFiles.assets("lang/en_us.json"));
        assertTrue(lang.has(ShellIceBlock.WARNING_KEY));
        assertTrue(lang.has(RawEuropiumItem.TOOLTIP_KEY));
    }

    private static List<String> variantKeys(String id) throws IOException {
        return new ArrayList<>(EuropaFiles.json(EuropaFiles.assets("blockstates/" + id + ".json"))
                .getAsJsonObject("variants").keySet());
    }

    @Test
    @DisplayName("names: destination, every block and item form, the ten biomes")
    void lang() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        JsonObject lang = EuropaFiles.json(EuropaFiles.assets("lang/en_us.json"));
        assertEquals("Europa", lang.get("destination.nerospace.europa").getAsString());
        assertTrue(lang.has("destination.nerospace.europa.short"));
        for (String id : EuropaFiles.BLOCKS) {
            assertTrue(lang.has("block.nerospace." + id), "block name " + id);
            // A block item shows the raw key unless the item alias exists too.
            assertEquals(!id.equals("brine"), lang.has("item.nerospace." + id), "item alias " + id);
        }
        for (String id : EuropaFiles.ITEMS) {
            assertTrue(lang.has("item.nerospace." + id), "item name " + id);
        }
        List<String> biomes = new ArrayList<>(EuropaFiles.NATURAL_BIOMES);
        biomes.addAll(EuropaFiles.TRIO);
        for (String b : biomes) {
            assertTrue(lang.has("biome.nerospace." + b), "biome name " + b);
        }
    }

    @Test
    @DisplayName("no Europa source, asset or data file contains the banned word")
    void bannedWord() throws IOException {
        Assumptions.assumeTrue(EuropaFiles.commonMain() != null, "common sources not found");
        List<Path> files = new ArrayList<>();
        try (Stream<Path> s = Files.walk(EuropaFiles.commonMain().resolve(
                "java/za/co/neroland/nerospace/world/planet/europa"))) {
            s.filter(Files::isRegularFile).forEach(files::add);
        }
        List<String> ids = new ArrayList<>(EuropaFiles.BLOCKS);
        ids.addAll(EuropaFiles.ITEMS);
        for (String id : ids) {
            for (String rel : List.of("blockstates/" + id + ".json", "items/" + id + ".json",
                    "models/block/" + id + ".json", "models/item/" + id + ".json")) {
                if (file(EuropaFiles.assets(rel))) {
                    files.add(EuropaFiles.assets(rel));
                }
            }
            for (String tree : EuropaFiles.TREES) {
                Path loot = EuropaFiles.data(tree, "loot_table/blocks/" + id + ".json");
                if (file(loot)) {
                    files.add(loot);
                }
            }
        }
        files.add(EuropaFiles.assets("sky/europa.json"));
        assertTrue(files.size() > 80, "scanned " + files.size());
        for (Path f : files) {
            assertFalse(BANNED.matcher(Files.readString(f, StandardCharsets.UTF_8)).find(), f.toString());
        }
    }
}
