package za.co.neroland.nerospace.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * Shared renderer for the Phase 5 Greenxertz creatures. One renderer class serves all three mob types
 * (each registered with its own texture) via the common {@link GreenxertzCreatureModel}. Uses the base
 * {@link LivingEntityRenderState}, so no custom render state is needed yet.
 */
public class GreenxertzCreatureRenderer extends MobRenderer<Mob, LivingEntityRenderState, GreenxertzCreatureModel> {

    private final Identifier texture;

    public GreenxertzCreatureRenderer(EntityRendererProvider.Context context, Identifier texture) {
        super(context, new GreenxertzCreatureModel(context.bakeLayer(GreenxertzCreatureModel.LAYER)), 0.4F);
        this.texture = texture;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return this.texture;
    }
}
