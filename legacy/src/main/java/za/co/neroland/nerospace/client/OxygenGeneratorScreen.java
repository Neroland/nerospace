package za.co.neroland.nerospace.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.OxygenGeneratorMenu;

/**
 * Screen for the Oxygen Generator (gas-layer rework): a power-buffer gauge (fed by pipes) and the
 * internal oxygen tank gauge with a Producing/Starved status. No slots — power and oxygen both move
 * through the Universal Pipe network.
 */
public class OxygenGeneratorScreen extends TexturedContainerScreen<OxygenGeneratorMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "textures/gui/oxygen_generator.png");
    private static final int ACCENT = 0xFF3CC8E6;     // cyan (power)
    private static final int OXYGEN = 0xFF54D46A;     // gas-layer green

    public OxygenGeneratorScreen(OxygenGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE, ACCENT, 176, 166);
        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor g) {
        int energy = this.menu.getEnergy();
        int maxEnergy = this.menu.getMaxEnergy();
        int pct = maxEnergy == 0 ? 0 : energy * 100 / maxEnergy;
        float energyFrac = maxEnergy == 0 ? 0f : (float) energy / maxEnergy;

        label(g, Component.translatable("gui.nerospace.oxygen_generator.power", pct), 8, 20, 0xFFCFE7FF);
        segGauge(g, 8, 31, 160, 6, energyFrac, ACCENT);

        int oxygen = this.menu.getOxygen();
        int maxOxygen = this.menu.getMaxOxygen();
        float o2Frac = maxOxygen == 0 ? 0f : (float) oxygen / maxOxygen;

        label(g, Component.translatable("gui.nerospace.oxygen_generator.oxygen", oxygen, maxOxygen), 8, 46, 0xFFC8F0D2);
        fluidGauge(g, 8, 57, 160, 6, o2Frac, OXYGEN);

        boolean producing = this.menu.isProducing();
        label(g, Component.translatable(producing
                ? "gui.nerospace.oxygen_generator.producing"
                : "gui.nerospace.oxygen_generator.starved"), 8, 68, producing ? 0xFF9CF0C0 : 0xFF7E8EA0);
    }
}
