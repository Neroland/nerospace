package za.co.neroland.nerospace.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Nerospace's NeroQuests pack follows the sibling-pack format NeroQuests' {@code SiblingPacksTest} validates (the
 * Gson-only copy from NeroQuests' "Shipping a quest pack" page, so there is no dependency on NeroQuests), plus the
 * {@code worlds_apart} contract: one quest line per Worlds planet, milestone gates that a file defines, and no
 * quest for the hidden Luna step.
 */
class QuestPackFormatTest {

    static final String NAMESPACE = "nerospace";
    static final Set<String> OBJECTIVES = Set.of("collect_item", "craft_item", "kill_entity", "reach_dimension",
            "visit_structure", "gate_open", "quest_complete", "custom_event", "economy", "manual");
    static final Set<String> REWARDS = Set.of("item", "xp", "currency", "mod_hook");
    static final String KEY = "[a-z0-9_-]+(\\.[a-z0-9_-]+)+";

    static Path root() {
        for (Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath(); p != null; p = p.getParent()) {
            Path c = p.resolve("common/src/main/resources");
            if (Files.isDirectory(c)) {
                return c;
            }
        }
        throw new IllegalStateException("common/src/main/resources not found");
    }

    @Test
    @DisplayName("the pack follows the NeroQuests sibling-pack format")
    void packFollowsTheNeroQuestsFormat() throws IOException {
        Path root = root();
        Path data = root.resolve("data/" + NAMESPACE + "/neroquests");
        Map<String, JsonObject> quests = read(data.resolve("quests"));
        Map<String, JsonObject> chapters = read(data.resolve("chapters"));
        JsonObject lang = JsonParser.parseString(Files.readString(
                root.resolve("assets/" + NAMESPACE + "/lang/en_us.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        List<String> problems = new ArrayList<>();
        Set<String> serverOk = new HashSet<>();
        chapters.forEach((id, c) -> {
            key(problems, lang, id, c, "title", true);
            for (JsonElement e : c.getAsJsonArray("quests")) {
                String q = e.getAsJsonObject().get("quest").getAsString();
                if (!quests.containsKey(q)) {
                    problems.add(id + " lists unknown/foreign quest " + q);
                }
                if (c.has("allow_server_scope") && c.get("allow_server_scope").getAsBoolean()) {
                    serverOk.add(q);
                }
            }
        });
        quests.forEach((id, q) -> {
            key(problems, lang, id, q, "title", true);
            key(problems, lang, id, q, "description", false);
            boolean server = q.has("scope") && "server".equals(q.get("scope").getAsString());
            if (server && !serverOk.contains(id)) {
                problems.add(id + ": scope server needs allow_server_scope");
            }
            if (q.has("prerequisites")) {
                q.getAsJsonArray("prerequisites").forEach(p -> ref(problems, quests, id, p.getAsString()));
            }
            JsonArray objectives = q.getAsJsonArray("objectives");
            if (objectives == null || objectives.isEmpty()) {
                problems.add(id + ": no objectives");
            } else {
                for (JsonElement e : objectives) {
                    JsonObject o = e.getAsJsonObject();
                    String type = o.get("type").getAsString().replace("neroquests:", "");
                    if (!OBJECTIVES.contains(type)) {
                        problems.add(id + ": unknown objective " + type);
                    }
                    if (type.equals("quest_complete")) {
                        ref(problems, quests, id, o.get("quest").getAsString());
                    }
                    if (type.equals("custom_event") && !server && !(o.has("audience")
                            && "everyone".equals(o.get("audience").getAsString()))) {
                        problems.add(id + ": custom_event on a player quest needs \"audience\": \"everyone\"");
                    }
                }
            }
            if (q.has("rewards")) {
                for (JsonElement e : q.getAsJsonArray("rewards")) {
                    JsonObject r = e.getAsJsonObject();
                    String type = r.get("type").getAsString().replace("neroquests:", "");
                    if (!REWARDS.contains(type)) {
                        problems.add(id + ": reward " + type + " not allowed in a pack");
                    }
                    if (type.equals("mod_hook") && !r.get("gate").getAsString().startsWith(NAMESPACE + ":hook/")) {
                        problems.add(id + ": mod_hook gate must be " + NAMESPACE + ":hook/<id>");
                    }
                }
            }
        });
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    @DisplayName("worlds_apart: one four-quest line per Worlds planet, gates defined, the hidden Luna step is no quest")
    void worldsApart() throws IOException {
        Path root = root();
        Path data = root.resolve("data/" + NAMESPACE + "/neroquests");
        Map<String, JsonObject> quests = read(data.resolve("quests"));
        JsonObject chapter = JsonParser.parseString(Files.readString(data.resolve("chapters/worlds_apart.json"),
                StandardCharsets.UTF_8)).getAsJsonObject();
        List<String> listed = new ArrayList<>();
        chapter.getAsJsonArray("quests").forEach(e -> listed.add(e.getAsJsonObject().get("quest").getAsString()));
        assertEquals(WorldsProgress.WORLDS.size() * 4, listed.size());
        for (String planet : WorldsProgress.WORLDS) {
            for (String s : List.of("landfall", "prospector", "weathered", "rooted")) {
                assertTrue(listed.contains("nerospace:worlds_apart/" + planet + "_" + s), planet + " " + s);
            }
        }
        for (String id : listed) {
            JsonObject q = quests.get(id);
            String text = q.toString().toLowerCase(java.util.Locale.ROOT);
            assertFalse(text.contains("chees") || text.contains("big_"), id + " must not be the hidden Luna step");
            for (JsonElement e : q.getAsJsonArray("objectives")) {
                JsonObject o = e.getAsJsonObject();
                if (o.get("type").getAsString().equals("neroquests:gate_open")) {
                    assertGateFile(root, o.get("gate").getAsString(), id);
                }
            }
            for (JsonElement e : q.getAsJsonArray("rewards")) {
                JsonObject r = e.getAsJsonObject();
                if (r.get("type").getAsString().equals("neroquests:mod_hook")) {
                    assertGateFile(root, r.get("gate").getAsString(), id);
                }
            }
        }
        assertGateFile(root, WorldsProgress.WORLDS_BRIEFING.toString(), "hook");
        assertGateFile(root, WorldsProgress.gate("mars", "weather").toString(), "gate");
    }

    private static void assertGateFile(Path root, String gate, String owner) {
        assertTrue(gate.startsWith(NAMESPACE + ":"), owner + " gate " + gate);
        Path file = root.resolve("data/" + NAMESPACE + "/neroland_gates/" + gate.substring(NAMESPACE.length() + 1)
                + ".json");
        assertTrue(Files.isRegularFile(file), owner + ": no gate file for " + gate);
    }

    static void ref(List<String> problems, Map<String, JsonObject> quests, String id, String target) {
        if (target.startsWith("neroquests:")) {
            return;
        }
        if (!target.startsWith(NAMESPACE + ":")) {
            problems.add(id + ": depends on another pack's quest " + target);
        } else if (!quests.containsKey(target)) {
            problems.add(id + ": unknown quest " + target);
        }
    }

    static void key(List<String> problems, JsonObject lang, String id, JsonObject o, String field, boolean required) {
        if (!o.has(field)) {
            if (required) {
                problems.add(id + ": missing " + field);
            }
            return;
        }
        String key = o.get(field).getAsString();
        if (!key.matches(KEY)) {
            problems.add(id + ": " + field + " is not a translation key");
        } else if (!lang.has(key)) {
            problems.add(id + ": en_us.json has no " + key);
        }
    }

    static Map<String, JsonObject> read(Path dir) throws IOException {
        Map<String, JsonObject> out = new TreeMap<>();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                String rel = dir.relativize(f).toString().replace('\\', '/');
                out.put(NAMESPACE + ":" + rel.substring(0, rel.length() - 5),
                        JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject());
            }
        }
        return out;
    }
}
