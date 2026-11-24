package za.co.neroland.nerospace.fabric.mixin;

//? if <1.20.1 || >=1.20.2 <1.20.4 {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import za.co.neroland.nerospace.fabric.LegacyAttachments;

@Mixin(ChunkAccess.class)
abstract class LegacyChunkAttachmentsMixin implements LegacyAttachments {
    @Unique
    private final CompoundTag nerospace$attachments = new CompoundTag();

    @Override
    public CompoundTag nerospace$data() {
        return nerospace$attachments;
    }
}
*///?}
