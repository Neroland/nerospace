package za.co.neroland.nerospace.client.shader;

import java.util.Locale;

/**
 * Which of a shader pack's three vanilla program sets a world asks for. A sky profile names one with its
 * optional {@code shaderDimension} field; airless worlds use {@link #END} so a pack does not paint a blue
 * sky and its own clouds over them.
 */
public enum ShaderDimension {
    OVERWORLD, NETHER, END;

    /** Lenient by-name lookup: absent or unknown is {@link #OVERWORLD} (no change to what a pack does today). */
    public static ShaderDimension byName(String name) {
        if (name == null || name.isEmpty()) {
            return OVERWORLD;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return OVERWORLD;
        }
    }

    /** The lower-case name used in sky profiles. */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
