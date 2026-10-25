package za.co.neroland.nerospace.world.station;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Pure station access rules. Everything here takes the entry's owner and member
 * strings, the actor's UUID string and whether the actor is an operator (permission level 2 or higher), so the
 * whole matrix is unit-testable without a server. {@code StationRegistry} delegates its {@code ServerPlayer}
 * overloads here.
 */
public final class StationAccess {

    /** Members a station may hold. */
    public static final int MAX_MEMBERS = 16;

    /** Class change cooldown: one overworld day of game time. */
    public static final long CLASS_COOLDOWN_TICKS = 24_000L;

    /** Why an add-member request was refused (or {@link #OK}). */
    public enum AddVerdict { OK, FULL, IS_OWNER, ALREADY_MEMBER, NOT_ONLINE, INVALID }

    private StationAccess() {
    }

    private static boolean unowned(@Nullable String owner) {
        return owner == null || owner.isEmpty();
    }

    /** Owner, operator, or anyone on an unowned station. */
    public static boolean canManage(@Nullable String owner, @Nullable String actor, boolean op) {
        if (unowned(owner)) {
            return true;
        }
        if (op) {
            return true;
        }
        return actor != null && owner.equals(actor);
    }

    /** Owner, member, operator, or anyone on an unowned station. */
    public static boolean canUse(@Nullable String owner, List<String> members, @Nullable String actor, boolean op) {
        if (unowned(owner)) {
            return true;
        }
        if (actor != null && (owner.equals(actor) || members.contains(actor))) {
            return true;
        }
        return op;
    }

    /**
     * Whether the actor passes {@link #canManage} <em>only</em> through the operator branch — the case
     * {@code StationAudit} logs. Owner and unowned-station actions are ordinary play and are not logged.
     */
    public static boolean manageViaOp(@Nullable String owner, @Nullable String actor, boolean op) {
        return op && !unowned(owner) && (actor == null || !owner.equals(actor));
    }

    /** As {@link #manageViaOp} for {@link #canUse}: an op who is neither owner nor member. */
    public static boolean useViaOp(@Nullable String owner, List<String> members, @Nullable String actor, boolean op) {
        return op && !unowned(owner) && (actor == null || (!owner.equals(actor) && !members.contains(actor)));
    }

    /**
     * Whether the class may change now: never changed ({@code -1}) is free, otherwise one full day of game time
     * must have passed. Operators bypass the cooldown (and that bypass is logged by the caller).
     */
    public static boolean canChangeClass(long gameTime, long classChangedAt, boolean op) {
        if (op || classChangedAt < 0) {
            return true;
        }
        return gameTime - classChangedAt >= CLASS_COOLDOWN_TICKS;
    }

    /** Ticks until the class may change again (0 = now). */
    public static long classCooldownRemaining(long gameTime, long classChangedAt) {
        if (classChangedAt < 0) {
            return 0L;
        }
        return Math.max(0L, CLASS_COOLDOWN_TICKS - (gameTime - classChangedAt));
    }

    /** The add-member rule: online only, not the owner, not already a member, fewer than 16. */
    public static AddVerdict canAddMember(@Nullable String owner, List<String> members, @Nullable String candidate,
            boolean candidateOnline) {
        if (candidate == null || candidate.isEmpty()) {
            return AddVerdict.INVALID;
        }
        if (!candidateOnline) {
            return AddVerdict.NOT_ONLINE;
        }
        if (candidate.equals(owner)) {
            return AddVerdict.IS_OWNER;
        }
        if (members.contains(candidate)) {
            return AddVerdict.ALREADY_MEMBER;
        }
        if (members.size() >= MAX_MEMBERS) {
            return AddVerdict.FULL;
        }
        return AddVerdict.OK;
    }
}
