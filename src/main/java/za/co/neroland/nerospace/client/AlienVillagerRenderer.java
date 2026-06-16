package za.co.neroland.nerospace.client;

import java.util.Random;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.entity.AlienVillager;

/**
 * Renderer for the Alien Villager (Phase 1 appearance + Phase 2 mood). On top of the shared creature
 * animation it adds a per-individual palette tint (getModelTint), a per-biome accessory texture
 * (getTextureLocation), and a mood warmth that brightens the tint as the villager's trust rises. The
 * emissive eye/crystal glow rides along via GlowEyesLayer.
 */
public class AlienVillagerRenderer
        extends MobRenderer<AlienVillager, AlienVillagerRenderState, EntityModel<AlienVillagerRenderState>> {

    private static final Identifier BASE =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/entity/alien_villager.png");
    private static final Identifier MEADOW =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/entity/alien_villager_meadow.png");
    private static final Identifier GLOW =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/entity/alien_villager_glow.png");

    @SuppressWarnings("this-escape")
    public AlienVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new AlienVillagerModel(context.bakeLayer(AlienVillagerModel.LAYER)), 0.4F);
        this.addLayer(new GlowEyesLayer<AlienVillagerRenderState>(this, GLOW));
    }

    @Override
    public AlienVillagerRenderState createRenderState() {
        return new AlienVillagerRenderState();
    }

    @Override
    public void extractRenderState(AlienVillager entity, AlienVillagerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.colorSeed = entity.getColorSeed();
        state.biomeId = entity.getBiomeId();
        state.planet = entity.getPlanet();
        state.displayTier = entity.getDisplayTier();
    }

    @Override
    public Identifier getTextureLocation(AlienVillagerRenderState state) {
        if (state.biomeId != null && state.biomeId.contains("meadow")) {
            return MEADOW;
        }
        return BASE;
    }

    @Override
    protected int getModelTint(AlienVillagerRenderState state) {
        Random rnd = new Random(state.colorSeed);
        int warmth = Math.max(0, Math.min(5, state.displayTier)) * 4;
        int r = clamp(200 + rnd.nextInt(56) + warmth);
        int g = clamp(216 + rnd.nextInt(40) + warmth / 2);
        int b = clamp(190 + rnd.nextInt(56));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
