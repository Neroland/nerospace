package za.co.neroland.nerospace.telemetry;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A hidden developer diagnostic block: it is deliberately left OUT of the creative menu, so the only
 * way to get one is {@code /give @s nerospace:sentry_test}. Placing it fires a single synthetic
 * Sentry event through {@link NerospaceTelemetry#sendTestEvent(String)}, which is the easiest way to
 * confirm end-to-end that error reporting reaches the dashboard on a real (production) jar.
 *
 * <p>Everything happens server-side; the placer gets an action-bar line saying whether the event was
 * dispatched or skipped because telemetry is opted out. The synthetic exception originates in
 * Nerospace code, so it passes the package-only {@code beforeSend} filter; per-session de-duplication
 * means repeat placements in the same session collapse to one event (restart to test again).</p>
 */
public class SentryTestBlock extends Block {

    public SentryTestBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) {
            return;
        }
        boolean dispatched = NerospaceTelemetry.sendTestEvent("placed at " + pos.toShortString());
        if (placer instanceof Player player) {
            player.sendSystemMessage(Component.translatable(dispatched
                    ? "message.nerospace.sentry_test.sent"
                    : "message.nerospace.sentry_test.disabled"));
        }
    }
}
