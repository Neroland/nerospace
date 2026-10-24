package za.co.neroland.nerospace.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LightEngine;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import za.co.neroland.nerospace.block.light.LightAbsorbing;
import za.co.neroland.nerospace.block.light.NyxIndexHolder;
import za.co.neroland.nerospace.block.light.NyxLightIndex;

/**
 * Nyx Stone light absorption, part 1 of 2 (decision 002 §11.1): keeps each <b>block</b>-light engine's
 * {@link NyxLightIndex} in step with the world. Common to every loader and to 26.1.2/26.2/26.3 (the targeted
 * signatures are identical in all three). Sky light is never touched.
 *
 * <ul>
 *   <li>{@code setLightEnabled(chunk, true)} — runs before a chunk is first lit and when a loaded chunk's light is
 *       enabled: sections whose palette may hold an absorber are scanned (others cost one palette check).</li>
 *   <li>{@code checkBlock(pos)} — every block change with different light properties (Nyx Stone opts in through
 *       {@code useShapeForLightOcclusion}). A toggled absorber re-checks its 26 neighbours so the −2 halo is
 *       re-propagated.</li>
 * </ul>
 * {@code require = 0}: if another mod replaces the light engine, the injectors are skipped and Nyx Stone falls back
 * to a plain full light blocker (no emission).
 */
@SuppressWarnings("cast") // the (Object) casts are required by javac for mixin self-checks
@Mixin(LightEngine.class)
public abstract class LightEngineNyxMixin implements NyxIndexHolder {

    @Shadow
    @Final
    protected LightChunkGetter chunkSource;

    @Unique
    private final NyxLightIndex nerospace$index = new NyxLightIndex();

    @Unique
    private boolean nerospace$rechecking;

    @Shadow
    public abstract void checkBlock(BlockPos pos);

    @Shadow
    protected abstract BlockState getState(BlockPos pos);

    @Override
    public NyxLightIndex nerospace$nyxIndex() {
        return this.nerospace$index;
    }

    @Inject(method = "checkBlock(Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), require = 0)
    private void nerospace$trackAbsorber(BlockPos pos, CallbackInfo ci) {
        if (!((Object) this instanceof BlockLightEngine) || this.nerospace$rechecking) {
            return;
        }
        boolean absorber = LightAbsorbing.isAbsorber(getState(pos)) && LightAbsorbing.enabled();
        boolean changed = absorber
                ? this.nerospace$index.add(pos.getX(), pos.getY(), pos.getZ())
                : this.nerospace$index.remove(pos.getX(), pos.getY(), pos.getZ());
        if (!changed) {
            return;
        }
        this.nerospace$rechecking = true;
        try {
            BlockPos.MutableBlockPos n = new BlockPos.MutableBlockPos();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx != 0 || dy != 0 || dz != 0) {
                            checkBlock(n.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz));
                        }
                    }
                }
            }
        } finally {
            this.nerospace$rechecking = false;
        }
    }

    @Inject(method = "setLightEnabled(Lnet/minecraft/world/level/ChunkPos;Z)V", at = @At("TAIL"), require = 0)
    private void nerospace$indexChunk(ChunkPos chunkPos, boolean enabled, CallbackInfo ci) {
        if (!((Object) this instanceof BlockLightEngine)) {
            return;
        }
        this.nerospace$index.clearChunk(chunkPos.x(), chunkPos.z());
        if (!enabled || !LightAbsorbing.enabled()) {
            return;
        }
        LightChunk light = this.chunkSource.getChunkForLighting(chunkPos.x(), chunkPos.z());
        if (!(light instanceof ChunkAccess chunk)) {
            return;
        }
        LevelChunkSection[] sections = chunk.getSections();
        int minSectionY = chunk.getMinSectionY();
        int baseX = chunkPos.getMinBlockX();
        int baseZ = chunkPos.getMinBlockZ();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir() || !section.maybeHas(LightAbsorbing::isAbsorber)) {
                continue;
            }
            int baseY = (minSectionY + i) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (LightAbsorbing.isAbsorber(section.getBlockState(x, y, z))) {
                            this.nerospace$index.add(baseX + x, baseY + y, baseZ + z);
                        }
                    }
                }
            }
        }
    }
}
