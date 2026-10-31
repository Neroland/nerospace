package za.co.neroland.nerospace.client.compat;

// Minecraft 1.21.1 only (empty on every other line). 1.21.2 split entity rendering into an extract step that fills a
// render state and a draw step that reads it. Before it there is no render state at all; this look-alike (same simple
// name, swapped in by an import rewrite in stonecutter.gradle) carries the fields Nerospace's renderers and models
// read, filled from the entity by the client.compat EntityRenderer / MobRenderer look-alikes.

//? if <1.21.2 {
/*import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class EntityRenderState {

    public double x;
    public double y;
    public double z;
    public float ageInTicks;
    public float boundingBoxWidth;
    public float boundingBoxHeight;
    public float eyeHeight;
    public boolean isInvisible;
    public int lightCoords = 0xF000F0;

    // The vanilla 1.21.2+ EntityRenderer.extractRenderState(..) base fields.
    public static void extractBase(Entity entity, EntityRenderState state, float partialTick) {
        state.x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        state.y = Mth.lerp(partialTick, entity.yOld, entity.getY());
        state.z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        state.ageInTicks = entity.tickCount + partialTick;
        state.boundingBoxWidth = entity.getBbWidth();
        state.boundingBoxHeight = entity.getBbHeight();
        state.eyeHeight = entity.getEyeHeight();
        state.isInvisible = entity.isInvisible();
    }
}
*///?}
