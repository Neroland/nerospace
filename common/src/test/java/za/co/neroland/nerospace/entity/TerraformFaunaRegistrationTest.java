package za.co.neroland.nerospace.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Terraform fauna: each of the Moon Hopper, Dust Strider and Dusk Moth is registered as an entity
 * with a spawn egg, has attributes and a spawn placement rule, a loot table on both resource trees, lang in the three
 * shipped languages, a placeholder renderer and membership of {@code #nerospace:native_fauna}. Source and data checks
 * only (no registry bootstrap), plus sanity bounds on the attribute numbers.
 */
class TerraformFaunaRegistrationTest {

    /** id, registering Content class, entity class, its registry constant. */
    private static final List<String[]> FAUNA = List.of(
            new String[] {"moon_hopper", "world/planet/luna/LunaContent.java", "world/planet/luna/entity/MoonHopper.java",
                    "MOON_HOPPER", "world/planet/luna/client/LunaEntityRenderers.java"},
            new String[] {"dust_strider", "world/planet/mars/MarsContent.java",
                    "world/planet/mars/entity/DustStrider.java", "DUST_STRIDER",
                    "world/planet/mars/client/MarsEntityRenderers.java"},
            new String[] {"dusk_moth", "world/planet/nyxara/NyxaraContent.java",
                    "world/planet/nyxara/entity/DuskMoth.java", "DUSK_MOTH",
                    "world/planet/nyxara/client/NyxaraEntityRenderers.java"});

    static Path commonMain() {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            Path c = p.resolve("common/src/main");
            if (Files.isDirectory(c)) {
                return c;
            }
        }
        throw new IllegalStateException("common/src/main not found");
    }

    private static String java(String rel) throws IOException {
        return Files.readString(commonMain().resolve("java/za/co/neroland/nerospace").resolve(rel),
                StandardCharsets.UTF_8);
    }

    private static JsonObject json(String rel) throws IOException {
        return JsonParser.parseString(Files.readString(commonMain().resolve(rel), StandardCharsets.UTF_8))
                .getAsJsonObject();
    }

    @Test
    @DisplayName("each species registers an entity type and a spawn egg through the shared providers")
    void registered() throws IOException {
        for (String[] f : FAUNA) {
            String content = java(f[1]);
            assertTrue(Pattern.compile("ENTITY_TYPES\\.register\\(\\s*\"" + f[0] + "\"").matcher(content).find(),
                    f[0] + " entity type");
            assertTrue(content.contains("ModItems.ITEMS.register(\"" + f[0] + "_spawn_egg\""), f[0] + " spawn egg");
            assertTrue(Files.isRegularFile(commonMain().resolve("resources/assets/nerospace/items/" + f[0]
                    + "_spawn_egg.json")), f[0] + " spawn egg item model");
        }
    }

    @Test
    @DisplayName("each species has attributes, a placement rule, a renderer and vanilla sounds")
    void wired() throws IOException {
        String attributes = java("registry/ModEntityAttributes.java");
        String placements = java("registry/ModSpawnPlacements.java");
        String renderers = java("client/ClientEntityRenderers.java");
        for (String[] f : FAUNA) {
            assertTrue(attributes.contains("." + f[3] + ".get()"), f[0] + " attributes");
            assertTrue(placements.contains("." + f[3] + ")"), f[0] + " spawn placement");
            String renderer = java(f[4]);
            assertTrue(renderer.contains("." + f[3] + ".get()"), f[0] + " renderer");
            String cls = f[4].substring(f[4].lastIndexOf('/') + 1, f[4].length() - ".java".length());
            assertTrue(renderers.contains(cls + ".registerAll(sink)"), f[0] + " renderer reaches the call site");
            String entity = java(f[2]);
            assertTrue(entity.contains("createAttributes()"), f[0] + " createAttributes");
            assertTrue(entity.contains("SoundEvents."), f[0] + " reuses vanilla sounds");
            assertFalse(entity.contains("ModSounds."), f[0] + " registers no new sounds");
        }
    }

    @Test
    @DisplayName("each species has a one-pool loot table on both resource trees and lang in en/it/zh")
    void dataAndLang() throws IOException {
        for (String[] f : FAUNA) {
            for (String tree : List.of("resources", "resources-26.3")) {
                JsonObject loot = json(tree + "/data/nerospace/loot_table/entities/" + f[0] + ".json");
                JsonArray pools = loot.getAsJsonArray("pools");
                assertTrue(pools.size() == 1, f[0] + " " + tree + " one pool");
                String item = pools.get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject()
                        .get("name").getAsString();
                assertTrue(item.startsWith("nerospace:"), f[0] + " drops an existing Nerospace item");
            }
            for (String lang : List.of("en_us", "it_it", "zh_cn")) {
                JsonObject l = json("resources/assets/nerospace/lang/" + lang + ".json");
                assertTrue(l.has("entity.nerospace." + f[0]), lang + " entity name " + f[0]);
                assertTrue(l.has("item.nerospace." + f[0] + "_spawn_egg"), lang + " spawn egg " + f[0]);
            }
        }
    }

    @Test
    @DisplayName("the three species are #nerospace:native_fauna, and contact hazards skip natives")
    void nativeFauna() throws IOException {
        JsonArray values = json("resources/data/nerospace/tags/entity_type/native_fauna.json").getAsJsonArray("values");
        for (String[] f : FAUNA) {
            boolean found = false;
            for (var v : values) {
                found |= v.getAsString().equals("nerospace:" + f[0]);
            }
            assertTrue(found, f[0] + " in native_fauna");
        }
        assertTrue(java("block/ContactHazardBlock.java").contains("!PlanetHazards.isNative(entity)"));
        assertTrue(java("world/planet/PlanetHazards.java").contains("ModTags.EntityTypes.NATIVE_FAUNA"));
    }

    @Test
    @DisplayName("attribute numbers are sane: positive health and speed, a dim-only moth, a periodic hop")
    void attributeSanity() {
        assertTrue(FaunaStats.HOPPER_HEALTH > 0 && FaunaStats.HOPPER_HEALTH <= 20);
        assertTrue(FaunaStats.STRIDER_HEALTH > FaunaStats.HOPPER_HEALTH && FaunaStats.STRIDER_HEALTH <= 40);
        assertTrue(FaunaStats.MOTH_HEALTH > 0 && FaunaStats.MOTH_HEALTH < FaunaStats.HOPPER_HEALTH);
        for (double s : new double[] {FaunaStats.HOPPER_SPEED, FaunaStats.STRIDER_SPEED, FaunaStats.MOTH_SPEED,
                FaunaStats.MOTH_FLYING_SPEED}) {
            assertTrue(s > 0.0 && s <= 1.0, "speed " + s);
        }
        assertTrue(FaunaStats.mothMaySpawnAt(0));
        assertTrue(FaunaStats.mothMaySpawnAt(7));
        assertFalse(FaunaStats.mothMaySpawnAt(8));
        assertFalse(FaunaStats.mothMaySpawnAt(15));
        assertTrue(FaunaStats.hopperHopsNow(true, true, FaunaStats.HOPPER_HOP_INTERVAL * 3));
        assertFalse(FaunaStats.hopperHopsNow(false, true, 0));
        assertFalse(FaunaStats.hopperHopsNow(true, false, 0));
        assertFalse(FaunaStats.hopperHopsNow(true, true, 1));
    }
}
