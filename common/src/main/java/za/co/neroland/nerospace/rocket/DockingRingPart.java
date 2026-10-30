package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;

/** The nine cells of a Docking Ring, named by their offset from the centre. */
public enum DockingRingPart implements StringRepresentable {
    CENTER("center", 0, 0),
    NORTH("north", 0, -1),
    SOUTH("south", 0, 1),
    EAST("east", 1, 0),
    WEST("west", -1, 0),
    NORTH_EAST("north_east", 1, -1),
    NORTH_WEST("north_west", -1, -1),
    SOUTH_EAST("south_east", 1, 1),
    SOUTH_WEST("south_west", -1, 1);

    private final String name;
    private final int dx;
    private final int dz;

    DockingRingPart(String name, int dx, int dz) {
        this.name = name;
        this.dx = dx;
        this.dz = dz;
    }

    public int dx() {
        return this.dx;
    }

    public int dz() {
        return this.dz;
    }

    /** This part's position for a ring centred on {@code centre}. */
    public BlockPos at(BlockPos centre) {
        return centre.offset(this.dx, 0, this.dz);
    }

    /** The ring centre for a part standing at {@code pos}. */
    public BlockPos centreFrom(BlockPos pos) {
        return pos.offset(-this.dx, 0, -this.dz);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
