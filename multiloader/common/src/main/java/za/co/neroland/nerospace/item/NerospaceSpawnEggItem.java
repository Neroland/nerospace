package za.co.neroland.nerospace.item;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A spawn egg for a Nerospace creature. 26.1 dropped NeoForge's {@code DeferredSpawnEggItem} and vanilla
 * {@code SpawnEggItem} binds its entity type too early (items register before entity types). So this
 * resolves the {@link EntityType} <em>lazily</em> via a {@link Supplier} at use time and spawns the mob
 * on right-click — mirroring vanilla spawn-egg behaviour. Its icon is a flat egg texture (no procedural
 * tinting needed), which also keeps it loader-agnostic.
 */
public class NerospaceSpawnEggItem extends Item {

    private final Supplier<? extends EntityType<? extends Mob>> type;

    public NerospaceSpawnEggItem(Properties properties, Supplier<? extends EntityType<? extends Mob>> type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
                ? clicked : clicked.relative(face);
        Player player = context.getPlayer();

        Mob mob = this.type.get().spawn(level, stack, player, spawnPos, EntitySpawnReason.SPAWN_ITEM_USE,
                true, !clicked.equals(spawnPos) && face == Direction.UP);
        if (mob != null && (player == null || !player.getAbilities().instabuild)) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
