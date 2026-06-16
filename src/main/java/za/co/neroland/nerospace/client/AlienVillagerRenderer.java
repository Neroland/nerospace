package za.co.neroland.nerospace.client;

import java.util.Random;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.entity.AlienVillager;

/**
 * Renderer for the Alien Villager. Per-individual palette tint (getModelTint), per-planet + per-biome
 * skin (getTextureLocation: Greenxertz green/steel with a lighter meadow set, Cindara ember/red,
 * Glacira frost/pale), a trust-warmed mood tint, and the emissive eye/crystal glow.
 */
public class AlienVillagerRenderer
        extends MobRenderer<AlienVillager, AlienVillagerRenderState, EntityModel<AlienVillagerRenderState>> {

    private static Identifier tex(String n) {
        return Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/entity/" + n + ".png");
    }

    private static final Identifier BASE = tex("alien_villager");
    private static final Identifier MEADOW = tex("alien_villager_meadow");
    private static final Identifier CINDARA = tex("alien_villager_cindara");
    private static final Identifier GLACIRA = tex("alien_villager_glacira");
    private static final Identifier GLOW = tex("alien_villager_glow");

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
        return switch (state.planet) {
            case CINDARA -> CINDARA;
            case GLACIRA -> GLACIRA;
            case GREENXERTZ -> (state.biomeId != null && state.biomeId.contains("meadow")) ? MEADOW : BASE;
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
