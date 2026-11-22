package za.co.neroland.nerospace.client;

import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;

import za.co.neroland.nerospace.client.ui.HudHover;

/**
 * The mouse in GUI pixels. The cursor accessor has been renamed across the 1.21 and 26.x lines, so the
 * lookup is reflective and cached; a miss returns NaN and the HUD simply shows no tooltip.
 */
public final class GuiPointer {

    private static final String[] X_NAMES = { "xpos", "getXpos", "getX" };
    private static final String[] Y_NAMES = { "ypos", "getYpos", "getY" };

    private static boolean resolved;
    private static Method xMethod;
    private static Method yMethod;

    private GuiPointer() {
    }

    public static double guiX(Minecraft mc) {
        return axis(mc, true);
    }

    public static double guiY(Minecraft mc) {
        return axis(mc, false);
    }

    private static double axis(Minecraft mc, boolean x) {
        if (mc == null || mc.mouseHandler == null || mc.getWindow() == null) {
            return Double.NaN;
        }
        resolve(mc.mouseHandler);
        Method method = x ? xMethod : yMethod;
        if (method == null) {
            return Double.NaN;
        }
        try {
            Object value = method.invoke(mc.mouseHandler);
            if (!(value instanceof Number number)) {
                return Double.NaN;
            }
            int screen = x ? mc.getWindow().getScreenWidth() : mc.getWindow().getScreenHeight();
            int gui = x ? mc.getWindow().getGuiScaledWidth() : mc.getWindow().getGuiScaledHeight();
            return HudHover.toGui(number.doubleValue(), screen, gui);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Double.NaN;
        }
    }

    private static void resolve(Object mouse) {
        if (resolved) {
            return;
        }
        resolved = true;
        xMethod = find(mouse, X_NAMES);
        yMethod = find(mouse, Y_NAMES);
    }

    private static Method find(Object mouse, String[] names) {
        for (String name : names) {
            try {
                Method method = mouse.getClass().getMethod(name);
                if (method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    return method;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // try the next name
            }
        }
        return null;
    }
}
