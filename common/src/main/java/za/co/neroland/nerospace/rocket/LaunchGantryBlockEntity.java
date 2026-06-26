package za.co.neroland.nerospace.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * Block entity behind the {@link LaunchGantryBlock} — it exists purely so the service tower can be drawn
 * by a {@code BlockEntityRenderer}. The animation state (how far the tower has swung back to release a
 * launching rocket, and which way its service arm points) is transient client render state, updated each
 * frame by {@code LaunchGantryRenderer}; nothing here is saved or ticked server-side.
 */
public class LaunchGantryBlockEntity extends BlockEntity {

    /** Swing-back progress 0 (upright, arm over the pad) .. 1 (fully reclined to clear a launch). */
    private float swing;
    /** Cardinal direction from the tower toward the adjacent pad/rocket (the arm faces this way). */
    private int armDx = 1;
    private int armDz = 0;

    public LaunchGantryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH_GANTRY.get(), pos, state);
    }

    public float getSwing() {
        return this.swing;
    }

    public void setSwing(float swing) {
        this.swing = swing;
    }

    public int getArmDx() {
        return this.armDx;
    }

    public int getArmDz() {
        return this.armDz;
    }

    public void setArm(int dx, int dz) {
        this.armDx = dx;
        this.armDz = dz;
    }
}
