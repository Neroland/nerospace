package za.co.neroland.nerospace.fabric.mixin;

//? if <1.20.1 || >=1.20.2 <1.20.4 {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import za.co.neroland.nerospace.fabric.LegacyAttachments;

@Mixin(ServerPlayer.class)
abstract class LegacyPlayerAttachmentsMixin implements LegacyAttachments {
    @Unique
    private final CompoundTag nerospace$attachments = new CompoundTag();

    @Override
    public CompoundTag nerospace$data() {
        return nerospace$attachments;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void nerospace$loadAttachments(CompoundTag tag, CallbackInfo ci) {
        nerospace$read(tag);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void nerospace$saveAttachments(CompoundTag tag, CallbackInfo ci) {
        nerospace$write(tag);
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void nerospace$copyAttachments(ServerPlayer previous, boolean alive, CallbackInfo ci) {
        nerospace$attachments.merge(((LegacyAttachments) previous).nerospace$data().copy());
    }
}
*///?}
