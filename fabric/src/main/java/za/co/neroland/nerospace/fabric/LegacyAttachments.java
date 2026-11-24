package za.co.neroland.nerospace.fabric;

//? if <1.20.1 || >=1.20.2 <1.20.4 {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public interface LegacyAttachments {
    String KEY = "fabric:attachments";
    String OXYGEN = "nerospace:oxygen";
    String TERRAFORMED = "nerospace:terraformed";
    String TERRAFORM_STAGE = "nerospace:terraform_stage";
    String STAR_GUIDE_SEEN = "nerospace:star_guide_seen";

    CompoundTag nerospace$data();

    default void nerospace$read(CompoundTag from) {
        CompoundTag data = nerospace$data();
        CompoundTag all = from.getCompound(KEY);
        for (String key : java.util.List.of(OXYGEN, TERRAFORMED, TERRAFORM_STAGE, STAR_GUIDE_SEEN)) {
            Tag value = all.get(key);
            if (value != null) {
                data.put(key, value.copy());
            } else {
                data.remove(key);
            }
        }
    }

    default void nerospace$write(CompoundTag to) {
        if (!nerospace$data().isEmpty()) {
            CompoundTag all = to.getCompound(KEY);
            all.merge(nerospace$data());
            to.put(KEY, all);
        }
    }
}
*///?}
