package za.co.neroland.nerospace.client.sky;

import java.util.Locale;
import java.util.function.Predicate;

/**
 * The client's {@code sky.bodyStyle} choice: {@link #GLOBE} draws suns, moons and planets with their shaded globe
 * textures, {@link #SQUARE} swaps each for its flat vanilla-style square variant where one exists. Pure: no
 * Minecraft or GPU types, so it is unit-tested directly.
 *
 * <p>A square variant of {@code textures/sky/<name>.png} lives at {@code textures/sky/square/<name>.png}
 * ({@code tools/gen_sky_textures.py} writes them). A texture without one keeps its globe texture.</p>
 */
public enum SkyBodyStyle {
    GLOBE,
    SQUARE;

    /** Folder the globe textures live in, and the sub-folder that holds their square variants. */
    public static final String SKY_FOLDER = "textures/sky/";
    public static final String SQUARE_FOLDER = SKY_FOLDER + "square/";

    /** {@code sky.bodyStyle} parsing: {@code square} (any case, trimmed) is square, everything else globe. */
    public static SkyBodyStyle byName(String name) {
        return name != null && "square".equals(name.trim().toLowerCase(Locale.ROOT)) ? SQUARE : GLOBE;
    }

    /** Where the square variant of the texture at {@code path} would be, or {@code path} when it can have none. */
    public static String squarePath(String path) {
        if (path == null || !path.startsWith(SKY_FOLDER) || path.startsWith(SQUARE_FOLDER)) {
            return path;
        }
        String file = path.substring(SKY_FOLDER.length());
        return file.isEmpty() || file.indexOf('/') >= 0 ? path : SQUARE_FOLDER + file;
    }

    /**
     * The texture path to draw for {@code path} under {@code style}: the square variant when the style is
     * {@link #SQUARE} and {@code exists} says that file is there, otherwise {@code path} itself (the globe texture).
     */
    public static String select(String path, SkyBodyStyle style, Predicate<String> exists) {
        if (style != SQUARE) {
            return path;
        }
        String square = squarePath(path);
        return square != null && !square.equals(path) && exists.test(square) ? square : path;
    }
}
