package za.co.neroland.nerospace.client.shader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import za.co.neroland.nerospace.NerospaceCommon;

/**
 * The only place that touches a shader mod (Iris, or a port that keeps its package names). Everything is
 * reflective and fail-soft: the shader mod is optional, never a dependency, and its internals may move. The
 * first failure switches the bridge off for the session and logs one line with no pack name or path in it.
 */
public final class IrisBridge {

    private static final String API = "net.irisshaders.iris.api.v0.IrisApi";
    private static final String IRIS = "net.irisshaders.iris.Iris";
    private static final String DIMENSION_ID = "net.irisshaders.iris.shaderpack.DimensionId";

    private static volatile boolean resolved;
    private static volatile boolean present;
    private static volatile boolean internalsFailed;
    private static Object api;
    private static Method isShaderPackInUse;
    private static Method getCurrentPack;
    private static Object overworld;
    private static Object nether;
    private static Object end;

    private IrisBridge() {
    }

    /** Whether a compatible shader mod is installed (its public API class resolves). */
    public static boolean present() {
        resolve();
        return present;
    }

    /** Whether a shader pack is in use right now. False without a shader mod or on any failure. */
    public static boolean packActive() {
        resolve();
        if (!present || api == null || isShaderPackInUse == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(isShaderPackInUse.invoke(api));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** The shader mod's own constant for a vanilla program set, or null when its internals are unavailable. */
    public static Object dimensionConstant(ShaderDimension dimension) {
        resolve();
        if (internalsFailed || dimension == null) {
            return null;
        }
        return switch (dimension) {
            case OVERWORLD -> overworld;
            case NETHER -> nether;
            case END -> end;
        };
    }

    /**
     * Whether the active pack names {@code namespace:path} in its own per-dimension configuration. A
     * catch-all entry does not count. Unknown (any failure) reads as false.
     */
    public static boolean packNames(String namespace, String path) {
        resolve();
        if (internalsFailed || getCurrentPack == null) {
            return false;
        }
        try {
            Object pack = getCurrentPack.invoke(null);
            if (pack instanceof Optional<?> optional) {
                pack = optional.orElse(null);
            }
            if (pack == null) {
                return false;
            }
            Map<?, ?> map = dimensionMap(pack);
            if (map == null) {
                return false;
            }
            String wanted = namespace + ":" + path;
            for (Object key : map.keySet()) {
                if (key != null && wanted.equals(idString(key))) {
                    return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Unknown reads as "the pack does not name us"; the dimension mapping itself still works.
            return false;
        }
    }

    private static Map<?, ?> dimensionMap(Object pack) throws ReflectiveOperationException {
        try {
            Object map = pack.getClass().getMethod("getDimensionMap").invoke(pack);
            return map instanceof Map<?, ?> m ? m : null;
        } catch (NoSuchMethodException e) {
            // Older builds keep the map in a private field with no getter.
            Field field = pack.getClass().getDeclaredField("dimensionMap");
            field.setAccessible(true);
            Object map = field.get(pack);
            return map instanceof Map<?, ?> m ? m : null;
        }
    }

    /** {@code namespace:name} of a shader-mod id object: through its getters, else its string form. */
    private static String idString(Object id) {
        try {
            Object namespace = id.getClass().getMethod("getNamespace").invoke(id);
            Object name = id.getClass().getMethod("getName").invoke(id);
            return namespace + ":" + name;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return String.valueOf(id);
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        synchronized (IrisBridge.class) {
            if (resolved) {
                return;
            }
            try {
                Class<?> apiClass = Class.forName(API);
                api = apiClass.getMethod("getInstance").invoke(null);
                isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
                present = true;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                present = false;
            }
            if (present) {
                try {
                    Class<?> ids = Class.forName(DIMENSION_ID);
                    overworld = ids.getField("OVERWORLD").get(null);
                    nether = ids.getField("NETHER").get(null);
                    end = ids.getField("END").get(null);
                    getCurrentPack = Class.forName(IRIS).getMethod("getCurrentPack");
                } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                    fail("dimension ids");
                }
            }
            resolved = true;
        }
    }

    /** One log line per session; {@code what} is a fixed string, never pack or file data. */
    private static void fail(String what) {
        if (!internalsFailed) {
            internalsFailed = true;
            NerospaceCommon.LOGGER.info("[Nerospace] shader compatibility inactive ({} unavailable)", what);
        }
    }
}
