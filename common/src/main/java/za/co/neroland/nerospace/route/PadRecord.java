package za.co.neroland.nerospace.route;

import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.api.route.PadHandle;

/**
 * One registered Cargo Pad (internal, persisted). {@code owner} is the placer's UUID string, {@code ""}
 * when unowned (erased, or registered before anyone claimed it); {@code access} holds UUID strings of
 * players the owner let route here. Immutable — mutators return copies.
 *
 * @param id       stable id, never reused (always positive; stations use negative ids)
 * @param name     player-facing label
 * @param dim      dimension id string
 * @param pos      block position
 * @param owner    owner UUID string or {@code ""}
 * @param isPublic anyone may route here
 * @param access   UUID strings with route access
 */
public record PadRecord(int id, String name, String dim, BlockPos pos, String owner, boolean isPublic,
        List<String> access) {

    /** Cap on the access list so one pad cannot bloat the save. */
    public static final int MAX_ACCESS = 32;

    public static final Codec<PadRecord> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("id").forGetter(PadRecord::id),
            Codec.STRING.fieldOf("name").forGetter(PadRecord::name),
            Codec.STRING.fieldOf("dim").forGetter(PadRecord::dim),
            BlockPos.CODEC.fieldOf("pos").forGetter(PadRecord::pos),
            Codec.STRING.optionalFieldOf("owner", "").forGetter(PadRecord::owner),
            Codec.BOOL.optionalFieldOf("public", false).forGetter(PadRecord::isPublic),
            Codec.STRING.listOf().optionalFieldOf("access", List.of()).forGetter(PadRecord::access)
    ).apply(inst, PadRecord::of));

    public PadRecord {
        name = name == null ? "" : name;
        owner = owner == null ? "" : owner;
        pos = pos.immutable();
        access = access == null ? List.of() : List.copyOf(access);
    }

    private static PadRecord of(Integer id, String name, String dim, BlockPos pos, String owner, Boolean isPublic,
            List<String> access) {
        return new PadRecord(id.intValue(), name, dim, pos, owner, isPublic.booleanValue(), access);
    }

    public ResourceKey<Level> dimension() {
        return RouteRegistry.dimKey(this.dim);
    }

    public boolean ownedBy(UUID player) {
        return player != null && !this.owner.isEmpty() && this.owner.equals(player.toString());
    }

    public boolean accessibleBy(UUID player) {
        return this.isPublic || ownedBy(player) || (player != null && this.access.contains(player.toString()));
    }

    /** Whether {@code player} may change this pad (owner, or anyone while it is unowned). */
    public boolean managedBy(UUID player) {
        return this.owner.isEmpty() || ownedBy(player);
    }

    PadRecord withOwner(String newOwner) {
        return new PadRecord(this.id, this.name, this.dim, this.pos, newOwner, this.isPublic, this.access);
    }

    PadRecord withName(String newName) {
        return new PadRecord(this.id, newName, this.dim, this.pos, this.owner, this.isPublic, this.access);
    }

    PadRecord withPublic(boolean value) {
        return new PadRecord(this.id, this.name, this.dim, this.pos, this.owner, value, this.access);
    }

    PadRecord withAccess(List<String> newAccess) {
        return new PadRecord(this.id, this.name, this.dim, this.pos, this.owner, this.isPublic, newAccess);
    }

    /** The API-facing snapshot (no owner leaks through the interface). */
    public PadHandle view() {
        return new PadView(this.id, this.name, dimension(), this.pos, false, this.isPublic, this.owner, this.access);
    }
}
