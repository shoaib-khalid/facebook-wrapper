package com.kalsym.facebook.wrapper.callback.handlers;

import com.github.messenger4j.webhook.event.MessageDeliveredEvent;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author z33Sh
 */
public class MessageDeliveredHandler {

    private static final Logger LOG = LoggerFactory.getLogger("application");

    public static String handleMessageDeliveredEvent(MessageDeliveredEvent event) {
        String senderId = "";
        try {
            LOG.debug("Handling MessageDeliveredEvent");
            senderId = event.senderId();
            LOG.debug("senderId: {}", senderId);
            final List<String> messageIds = event.messageIds().orElse(Collections.emptyList());
            final Instant watermark = event.watermark();
            LOG.debug("watermark: {}", watermark);

            messageIds.forEach(
                    messageId -> {
                        LOG.info("Received delivery confirmation for message '{}'", messageId);
                    });

            LOG.info("All messages before '{}' were delivered to user '{}'", watermark, senderId);
            return "SUCCESS";
        } catch (Exception ex) {
           LOG.error("{} Error: {} ", senderId, ex);
            return "EXCEPTION";
        }
    }
}
