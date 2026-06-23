package za.co.neroland.nerospace.telemetry;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

/**
 * Log4j2 appender that feeds {@link NerospaceTelemetry}. Minecraft routes essentially every failure
 * through log4j — handled errors, event-bus listener exceptions, and the crash report itself — so
 * listening on the root logger catches Nerospace failures without mixins. Filtering (Nerospace-only),
 * de-dup, rate-limiting and PII scrubbing all happen in {@link NerospaceTelemetry}; this only selects
 * candidate log events.
 */
final class SentryLogAppender extends AbstractAppender {

    SentryLogAppender() {
        super("NerospaceSentry", null, null, false, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
        if (!NerospaceTelemetry.isActive()) {
            return;
        }
        Level level = event.getLevel();
        if (!level.isMoreSpecificThan(Level.ERROR)) {
            return;
        }
        Throwable thrown = event.getThrown();
        if (thrown != null) {
            if (NerospaceTelemetry.touchesNerospace(thrown)) {
                NerospaceTelemetry.capture(thrown);
            }
        } else if (level == Level.FATAL) {
            String message = event.getMessage() == null ? null : event.getMessage().getFormattedMessage();
            if (message != null && message.contains("za.co.neroland.nerospace")) {
                NerospaceTelemetry.captureMessage(message);
            }
        }
    }
}
