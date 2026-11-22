package za.co.neroland.nerospace.suit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * Which module may sit in which piece, from which frame, and how many copies. Built in code so a missing or
 * broken data file still has the catalogue; {@link #overlay(String)} reads one {@code suit_modules} JSON file
 * and, if it fails, logs one line and keeps the built-in rule.
 */
public final class SuitSlotRules {

    public record Rule(String id, SuitPiece piece, SuitSlotKind kind, int maxPerPiece, SuitFrame minFrame,
            List<String> incompatible, boolean hidden, boolean inert) {
    }

    private static final Map<String, Rule> RULES = new LinkedHashMap<>();
    private static boolean loggedFallback;

    static {
        weave(SuitIds.THERMAL_WEAVE, SuitFrame.MK2, List.of(SuitIds.CRYO_WEAVE, SuitIds.ADAPTIVE_WEAVE));
        weave(SuitIds.CRYO_WEAVE, SuitFrame.MK2, List.of(SuitIds.THERMAL_WEAVE, SuitIds.ADAPTIVE_WEAVE));
        weave(SuitIds.ADAPTIVE_WEAVE, SuitFrame.MK4, List.of(SuitIds.THERMAL_WEAVE, SuitIds.CRYO_WEAVE));
        lining(SuitIds.RAD_LINING, SuitFrame.MK2);
        lining(SuitIds.PRESSURE_LINING, SuitFrame.MK2);
        module(SuitIds.LIFE_SUPPORT_TANK, SuitPiece.CHEST, SuitFrame.MK1, 2, List.of(), false, false);
        module(SuitIds.O2_RECLAIMER, SuitPiece.CHEST, SuitFrame.MK2, 2, List.of(), false, false);
        module(SuitIds.POWER_CELL, SuitPiece.CHEST, SuitFrame.MK2, 2, List.of(), false, false);
        module(SuitIds.SOLAR_SKIN, SuitPiece.CHEST, SuitFrame.MK2, 1, List.of(), false, false);
        module(SuitIds.KINETIC_SHIELD, SuitPiece.CHEST, SuitFrame.MK3, 1, List.of(), false, false);
        module(SuitIds.JUMP_JET, SuitPiece.CHEST, SuitFrame.MK3, 1, List.of(SuitIds.FLIGHT_HARNESS), false, false);
        module(SuitIds.FLIGHT_HARNESS, SuitPiece.CHEST, SuitFrame.MK4, 1, List.of(SuitIds.JUMP_JET), false, false);
        module(SuitIds.EVA_THRUSTERS, SuitPiece.CHEST, SuitFrame.MK4, 1, List.of(), true, true);
        module(SuitIds.HARDPOINT, SuitPiece.CHEST, SuitFrame.MK4, 1, List.of(), true, true);
        module(SuitIds.TOOL_LINK, SuitPiece.CHEST, SuitFrame.MK3, 1, List.of(), true, true);
        module(SuitIds.NIGHT_VISOR, SuitPiece.HELMET, SuitFrame.MK1, 1, List.of(), false, false);
        module(SuitIds.SURVEY_SCANNER, SuitPiece.HELMET, SuitFrame.MK2, 1, List.of(), false, false);
        module(SuitIds.SPORE_FILTER, SuitPiece.HELMET, SuitFrame.MK1, 1, List.of(), false, false);
        module(SuitIds.UPLINK_VISOR, SuitPiece.HELMET, SuitFrame.MK2, 1, List.of(), true, true);
        module(SuitIds.STRIDE_SERVOS, SuitPiece.LEGS, SuitFrame.MK2, 1, List.of(), false, false);
        module(SuitIds.LEAP_SERVOS, SuitPiece.LEGS, SuitFrame.MK2, 1, List.of(), false, false);
        module(SuitIds.SHOCK_ABSORBERS, SuitPiece.BOOTS, SuitFrame.MK1, 2, List.of(), false, false);
        module(SuitIds.MAG_CLAMPS, SuitPiece.BOOTS, SuitFrame.MK2, 1, List.of(), true, true);
        module(SuitIds.GLIDE_FINS, SuitPiece.BOOTS, SuitFrame.MK2, 1, List.of(), false, false);
    }

    private SuitSlotRules() {
    }

    public static Rule rule(String id) {
        return RULES.get(id);
    }

    public static List<Rule> all() {
        return List.copyOf(RULES.values());
    }

    /**
     * Whether {@code id} may be installed on {@code piece} at {@code frame} given what is already there.
     * Unknown ids are rejected at the fabricator (they are only kept when already saved).
     */
    public static boolean accepts(SuitLoadout loadout, SuitPiece piece, String id) {
        Rule rule = rule(id, piece);
        if (rule == null) {
            return false;
        }
        if (loadout.frameEnum().ordinal() < rule.minFrame.ordinal()) {
            return false;
        }
        if (rule.kind == SuitSlotKind.WEAVE) {
            if (loadout.frameEnum().weaveSlots < 1) {
                return false;
            }
            return !loadout.hasWeave();
        }
        if (rule.kind == SuitSlotKind.LINING) {
            if (loadout.linings().size() >= loadout.frameEnum().liningSlots) {
                return false;
            }
            return !loadout.linings().contains(id);
        }
        if (loadout.moduleCount(id) >= rule.maxPerPiece) {
            return false;
        }
        if (loadout.modules().size() >= loadout.frameEnum().moduleSlots(piece)) {
            return false;
        }
        for (String other : loadout.modules()) {
            Rule theirs = RULES.get(other);
            if (theirs != null && (theirs.incompatible.contains(id) || rule.incompatible.contains(other))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Replace the built-in rule for {@code json}'s id. A parse error keeps the built-in rule and logs once.
     *
     * @return true when the file was applied
     */
    public static boolean overlay(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String id = obj.get("id").getAsString();
            SuitPiece piece = SuitPiece.valueOf(obj.get("piece").getAsString());
            SuitSlotKind kind = SuitSlotKind.valueOf(obj.get("kind").getAsString());
            int max = obj.has("max") ? obj.get("max").getAsInt() : 1;
            SuitFrame min = SuitFrame.valueOf(obj.get("min_frame").getAsString());
            List<String> incompatible = new ArrayList<>();
            if (obj.has("incompatible")) {
                JsonArray array = obj.getAsJsonArray("incompatible");
                for (JsonElement element : array) {
                    incompatible.add(element.getAsString());
                }
            }
            boolean hidden = obj.has("hidden") && obj.get("hidden").getAsBoolean();
            boolean inert = obj.has("inert") && obj.get("inert").getAsBoolean();
            RULES.put(id, new Rule(id, piece, kind, max, min, List.copyOf(incompatible), hidden, inert));
            return true;
        } catch (RuntimeException e) {
            if (!loggedFallback) {
                loggedFallback = true;
                NerospaceCommon.LOGGER.warn("[Nerospace] suit module data failed to parse ({}); using the built-in "
                        + "catalogue", e.getClass().getSimpleName());
            }
            return false;
        }
    }

    /** Test hook. */
    public static void resetFallbackLog() {
        loggedFallback = false;
    }

    private static void weave(String id, SuitFrame min, List<String> incompatible) {
        // Weaves are installed on every piece; the rule's piece is a sentinel checked per call via acceptsPiece.
        for (SuitPiece piece : SuitPiece.values()) {
            RULES.put(id + "@" + piece.name(), new Rule(id, piece, SuitSlotKind.WEAVE, 1, min, incompatible, false,
                    false));
        }
    }

    private static void lining(String id, SuitFrame min) {
        for (SuitPiece piece : SuitPiece.values()) {
            RULES.put(id + "@" + piece.name(), new Rule(id, piece, SuitSlotKind.LINING, 1, min, List.of(), false,
                    false));
        }
    }

    private static void module(String id, SuitPiece piece, SuitFrame min, int max, List<String> incompatible,
            boolean hidden, boolean inert) {
        RULES.put(id, new Rule(id, piece, SuitSlotKind.MODULE, max, min, incompatible, hidden, inert));
    }

    /** Lookup that understands the per-piece keys used for weaves and linings. */
    public static Rule rule(String id, SuitPiece piece) {
        Rule direct = RULES.get(id);
        if (direct != null && direct.piece == piece) {
            return direct;
        }
        return RULES.get(id + "@" + piece.name());
    }
}
