package za.co.neroland.nerospace.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import za.co.neroland.nerospace.world.station.HydroponicsGate;

/**
 * Hydroponics Tray gate: a crop standing on a tray grows only with a Hull Window within three
 * blocks above the tray and inside an oxygen field, and an INDUSTRIAL station skips half its growth ticks. Crops on
 * anything else are untouched (one block-state read). {@code require = 0}: if the target ever moves, trays fall back
 * to plain always-moist farmland.
 */
@Mixin(CropBlock.class)
public abstract class CropHydroponicsMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true, require = 0)
    private void nerospace$hydroponicsGate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
            CallbackInfo ci) {
        if (!HydroponicsGate.allowsGrowth(level, pos, random)) {
            ci.cancel();
        }
    }
}
