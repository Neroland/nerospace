package za.co.neroland.nerospace.client;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.entity.AlienVillager;

/**
 * Renderer for the Alien Villager. Per-individual palette tint (getModelTint), per-planet + per-biome
 * skin (getTextureLocation: Greenxertz green/steel with a lighter meadow set, Cindara ember/red,
 * Glacira frost/pale), a trust-warmed mood tint, and the emissive eye/crystal glow.
 */
public class AlienVillagerRenderer
        extends MobRenderer<AlienVillager, AlienVillagerRenderState, EntityModel<AlienVillagerRenderState>> {

    private static Identifier tex(String n) {
        return Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/entity/" + n + ".png");
    }

    private static final Identifier BASE = tex("alien_villager");
    private static final Identifier MEADOW = tex("alien_villager_meadow");
    private static final Identifier CINDARA = tex("alien_villager_cindara");
    private static final Identifier GLACIRA = tex("alien_villager_glacira");
    private static final Identifier DUSKBORN = tex("alien_villager_duskborn");
    private static final Identifier GLOW = tex("alien_villager_glow");
    /** Per-culture glow: the Ember-kin lava hem, the Frostbound eyes, the Duskborn markings. */
    private static final Identifier CINDARA_GLOW = tex("alien_villager_cindara_glow");
    private static final Identifier GLACIRA_GLOW = tex("alien_villager_glacira_glow");
    private static final Identifier DUSKBORN_GLOW = tex("alien_villager_duskborn_glow");

    @SuppressWarnings("this-escape")
    public AlienVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new AlienVillagerModel(AlienVillagerModel.createBodyLayer().bakeRoot()), 0.4F);
        this.addLayer(new GlowEyesLayer<AlienVillagerRenderState>(this, AlienVillagerRenderer::glowTexture));
    }

    /** The glow layer texture for a villager's culture. */
    static Identifier glowTexture(AlienVillagerRenderState state) {
        return switch (state.planet) {
            case CINDARA -> CINDARA_GLOW;
            case GLACIRA -> GLACIRA_GLOW;
            case NYXARA -> DUSKBORN_GLOW;
            case GREENXERTZ -> GLOW;
        };
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
        return switch (state.planet) {
            case CINDARA -> CINDARA;
            case GLACIRA -> GLACIRA;
            case NYXARA -> DUSKBORN;
            case GREENXERTZ -> (state.biomeId != null && state.biomeId.contains("meadow")) ? MEADOW : BASE;
        };
    }

    /**
     * Per-culture silhouette on the shared model: Ember-kin stocky, Frostbound bulked by fur wraps, Duskborn
     * tall and slender, Xertz unchanged. The hitbox is the same for every culture.
     */
    @Override
    protected void scale(AlienVillagerRenderState state, PoseStack poseStack) {
        float[] s = silhouette(state.planet);
        poseStack.scale(s[0], s[1], s[0]);
    }

    /** {width, height} scale for a species. */
    static float[] silhouette(AlienVillager.Planet planet) {
        return switch (planet) {
            case CINDARA -> new float[] {1.15F, 0.88F};
            case GLACIRA -> new float[] {1.08F, 1.04F};
            case NYXARA -> new float[] {0.9F, 1.14F};
            case GREENXERTZ -> new float[] {1.0F, 1.0F};
        };
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
