package com.kalsym.facebook.wrapper.callback.handlers;

import com.github.messenger4j.webhook.event.MessageEchoEvent;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author z33Sh
 */
public class MessageEchoHandler {

    private static final Logger LOG = LoggerFactory.getLogger("application");

    public static String handleMessageEchoEvent(MessageEchoEvent event) {
        String senderId = "";
        try {
            LOG.debug("Handling MessageEchoEvent");
            senderId = event.senderId();
            final String recipientId = event.recipientId();
            final String messageId = event.messageId();
            final Instant timestamp = event.timestamp();

            LOG.info("Received echo for message '{}' that has been sent to recipient '{}' by sender '{}' at '{}'", messageId, recipientId, senderId, timestamp);
            return "SUCCESS";
        } catch (Exception ex) {
           LOG.error("{} Error: {} ", senderId, ex);
            return "EXCEPTION";
        }
    }
}
