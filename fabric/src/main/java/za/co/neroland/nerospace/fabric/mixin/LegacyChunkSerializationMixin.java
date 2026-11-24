package za.co.neroland.nerospace.fabric.mixin;

//? if <1.20.1 || >=1.20.2 <1.20.4 {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import za.co.neroland.nerospace.fabric.LegacyAttachments;

@Mixin(ChunkSerializer.class)
abstract class LegacyChunkSerializationMixin {
    @Inject(method = "read", at = @At("RETURN"))
    private static void nerospace$loadAttachments(ServerLevel level, PoiManager poi, ChunkPos pos, CompoundTag tag,
            CallbackInfoReturnable<ProtoChunk> cir) {
        ((LegacyAttachments) writableChunk(cir.getReturnValue())).nerospace$read(tag);
    }

    @Inject(method = "write", at = @At("RETURN"))
    private static void nerospace$saveAttachments(ServerLevel level, ChunkAccess chunk,
            CallbackInfoReturnable<CompoundTag> cir) {
        ((LegacyAttachments) writableChunk(chunk)).nerospace$write(cir.getReturnValue());
    }

    @org.spongepowered.asm.mixin.Unique
    private static ChunkAccess writableChunk(ChunkAccess chunk) {
        return chunk instanceof ImposterProtoChunk wrapper ? wrapper.getWrapped() : chunk;
    }
}
*///?}
