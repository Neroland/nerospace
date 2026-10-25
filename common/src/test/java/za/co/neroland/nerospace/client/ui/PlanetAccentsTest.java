package za.co.neroland.nerospace.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerospace.registry.ModDimensions;

/** 004 §2.3: every planet and HOME has an accent and a dark pair; accents stay ≥ 3:1 against the hull. */
class PlanetAccentsTest {

    private static final int HULL = 0xFF0D1119;

    static double lum(int argb) {
        double[] c = { ((argb >> 16) & 0xFF) / 255.0, ((argb >> 8) & 0xFF) / 255.0, (argb & 0xFF) / 255.0 };
        for (int i = 0; i < 3; i++) {
            c[i] = c[i] <= 0.03928 ? c[i] / 12.92 : Math.pow((c[i] + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
    }

    static double contrast(int a, int b) {
        double la = lum(a);
        double lb = lum(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    @Test
    void everyWorldHasAPair() {
        List<ResourceKey<Level>> dims = List.of(ModDimensions.OVERWORLD, ModDimensions.STATION_LEVEL, ModDimensions.LUNA_LEVEL,
                ModDimensions.GREENXERTZ_LEVEL, ModDimensions.MARS_LEVEL, ModDimensions.CINDARA_LEVEL,
                ModDimensions.GLACIRA_LEVEL, ModDimensions.NYXARA_LEVEL);
        for (ResourceKey<Level> d : dims) {
            int accent = PlanetAccents.of(d);
            int dark = PlanetAccents.dark(d);
            assertNotEquals(accent, dark, d.toString());
            assertTrue(contrast(accent, HULL) >= 3.0, d + " accent contrast " + contrast(accent, HULL));
        }
        assertEquals(PlanetAccents.HOME, PlanetAccents.of(null));
        assertEquals(0xFF2EC4B6, PlanetAccents.of(ModDimensions.STATION_LEVEL));
    }
}
