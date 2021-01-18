package com.kalsym.facebook.wrapper.callback.handlers;

import com.github.messenger4j.webhook.event.MessageReadEvent;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author z33Sh
 */
public class MessageReadHandler {

    private static final Logger LOG = LoggerFactory.getLogger("application");

    /**
     *
     * @param event
     * @return
     */
    public static String handleMessageReadEvent(MessageReadEvent event) {
        String senderId = "";
        try {
            LOG.debug("Handling MessageReadEvent");
            senderId = event.senderId();
            LOG.debug("senderId: {}", senderId);
            final Instant watermark = event.watermark();
            LOG.debug("watermark: {}", watermark);

            LOG.info("All messages before '{}' were read by user '{}'", watermark, senderId);
            return "SUCCESS";
        } catch (Exception ex) {
           LOG.error("{} Error: {} ", senderId, ex);
            return "EXCEPTION";
        }
    }
}
