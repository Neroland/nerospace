package za.co.neroland.nerospace.village;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

/**
 * The Village Core's owner record — the only personal data a core holds, and it is a UUID only.
 *
 * <p>1.6.0 also stored the owner's display name ({@code OwnerName}). 2.0 never reads or writes it: the key simply
 * disappears the next time the core's chunk saves, and chat resolves the name from the UUID at the moment it is
 * shown (online player, else "another player"). No name lookup is ever persisted.</p>
 *
 * <p>POPIA/GDPR: the owner is cleared by {@link #forget(UUID)} from the erasure sweep, and a core that loads later
 * in the same server session drops an erased owner on load ({@link #read}).</p>
 */
public final class VillageCoreOwnership {

    /** NBT key of the owner UUID (string, empty for unclaimed). */
    public static final String OWNER_KEY = "Owner";
    /** The 1.6.0 display-name key. Never read, never written. */
    public static final String LEGACY_NAME_KEY = "OwnerName";

    @Nullable
    private UUID owner;

    @Nullable
    public UUID owner() {
        return this.owner;
    }

    public boolean isClaimed() {
        return this.owner != null;
    }

    public boolean isOwner(@Nullable UUID player) {
        return player != null && player.equals(this.owner);
    }

    public void claim(UUID player) {
        this.owner = Objects.requireNonNull(player);
    }

    /**
     * Erasure: clears the owner if it is {@code player}. The core keeps its stockpile, buildings and quest (not
     * personal data) and becomes unclaimed.
     *
     * @return whether the owner was cleared
     */
    public boolean forget(@Nullable UUID player) {
        if (player != null && player.equals(this.owner)) {
            this.owner = null;
            return true;
        }
        return false;
    }

    /** Clears any owner (a pending erasure applied to a core that was unloaded at erasure time). */
    public boolean clear() {
        boolean had = this.owner != null;
        this.owner = null;
        return had;
    }

    /**
     * Loads the owner from a stored string field. {@code OwnerName} is ignored on purpose. An owner erased during
     * this server session ({@code erased}) is dropped on load.
     */
    public void read(Function<String, String> stored, Predicate<UUID> erased) {
        this.owner = parse(stored.apply(OWNER_KEY));
        if (this.owner != null && erased.test(this.owner)) {
            this.owner = null;
        }
    }

    /** Writes the owner field only — there is no name to write. */
    public void write(BiConsumer<String, String> out) {
        out.accept(OWNER_KEY, this.owner == null ? "" : this.owner.toString());
    }

    @Nullable
    static UUID parse(@Nullable String stored) {
        if (stored == null || stored.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(stored);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
