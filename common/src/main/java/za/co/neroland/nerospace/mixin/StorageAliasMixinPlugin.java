package za.co.neroland.nerospace.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Logs when the storage-alias injections do not land. The injectors stay {@code require = 0} so a
 * signature change cannot take the game down; this plugin is the warning that replace-by-hand is now
 * the only migration path. Every other mixin in the config is left alone.
 */
public final class StorageAliasMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
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
        if (mixinClassName == null || !mixinClassName.endsWith("MappedRegistryStorageAliasMixin") || targetClass == null) {
            return;
        }
        boolean holderByKey = has(targetClass, "nerospace$aliasHolderByKey");
        boolean holderById = has(targetClass, "nerospace$aliasHolderById");
        boolean valueById = has(targetClass, "nerospace$aliasValueById");
        String warning = StorageAliasAudit.warning(holderByKey, holderById, valueById);
        if (warning != null) {
            LoggerFactory.getLogger("Nerospace").warn(warning);
        }
    }

    private static boolean has(ClassNode node, String name) {
        for (MethodNode method : node.methods) {
            if (method.name != null && method.name.startsWith(name)) {
                return true;
            }
        }
        return false;
    }
}
