package za.co.neroland.nerospace.mixin.shader;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Applies the shader-compat mixins only when a compatible shader mod is installed. Checks for the class file
 * as a resource, so nothing of the shader mod is loaded early. Without one, the config contributes nothing
 * and the game behaves exactly as it does with no compatibility code at all.
 */
public final class ShaderCompatMixinPlugin implements IMixinConfigPlugin {

    private static final String IRIS_CLASS = "net/irisshaders/iris/Iris.class";

    private boolean shaderModPresent;

    @Override
    public void onLoad(String mixinPackage) {
        boolean found;
        try {
            found = ShaderCompatMixinPlugin.class.getClassLoader().getResource(IRIS_CLASS) != null;
        } catch (RuntimeException e) {
            found = false;
        }
        this.shaderModPresent = found;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.shaderModPresent;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
