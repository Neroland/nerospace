package za.co.neroland.nerospace.route;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.api.route.PadHandle;

/**
 * The immutable {@link PadHandle} handed to API consumers. The owner and access list are private fields
 * answered only through {@link #ownedBy} / {@link #accessibleBy}, so the public interface never leaks a
 * UUID that is not already the caller's (POPIA/GDPR).
 */
final class PadView implements PadHandle {

    private final int id;
    private final String name;
    private final ResourceKey<Level> dimension;
    private final BlockPos position;
    private final boolean station;
    private final boolean isPublic;
    private final String owner;
    private final List<String> access;

    PadView(int id, String name, ResourceKey<Level> dimension, BlockPos position, boolean station,
            boolean isPublic, String owner, List<String> access) {
        this.id = id;
        this.name = name;
        this.dimension = dimension;
        this.position = position.immutable();
        this.station = station;
        this.isPublic = isPublic;
        this.owner = owner == null ? "" : owner;
        this.access = access == null ? List.of() : List.copyOf(access);
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    @Override
    public BlockPos position() {
        return this.position;
    }

    @Override
    public boolean isStation() {
        return this.station;
    }

    @Override
    public boolean isPublic() {
        return this.isPublic;
    }

    @Override
    public boolean ownedBy(UUID player) {
        return player != null && !this.owner.isEmpty() && this.owner.equals(player.toString());
    }

    @Override
    public boolean accessibleBy(UUID player) {
        if (this.isPublic || ownedBy(player)) {
            return true;
        }
        if (player == null) {
            return false;
        }
        // An unowned station is manageable by anyone (the station rule); an unowned pad is not routable
        // by strangers until someone claims it.
        return this.station ? this.owner.isEmpty() : this.access.contains(player.toString());
    }

    @Override
    public String toString() {
        return "Pad#" + this.id + "(" + this.name + ")";
    }
}
