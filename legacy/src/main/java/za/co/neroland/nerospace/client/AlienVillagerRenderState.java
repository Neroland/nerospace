package za.co.neroland.nerospace.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import za.co.neroland.nerospace.entity.AlienVillager;

/**
 * Render state for the Alien Villager. Carries the per-individual variant pulled off the entity in
 * AlienVillagerRenderer#extractRenderState: the colour seed drives the palette tint, the planet +
 * home biome select the base/accessory texture, and the display tier (Phase 2) warms the tint as the
 * villager grows to trust players.
 */
public class AlienVillagerRenderState extends LivingEntityRenderState {

    public int colorSeed;
    public String biomeId = "";
    public AlienVillager.Planet planet = AlienVillager.Planet.GREENXERTZ;
    public int displayTier;
}
