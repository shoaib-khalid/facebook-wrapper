package com.kalsym.facebook.wrapper.callback.handlers;

import com.github.messenger4j.webhook.event.OptInEvent;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * * * @author z33Sh
 */
public class OptInHandler {

    private static final Logger LOG = LoggerFactory.getLogger("application");

    /**
     *
     * @param event
     * @return
     */
    public static String handleOptInEvent(OptInEvent event) {
        String senderId = "";
        try {
            LOG.debug("Handling OptInEvent");
            senderId = event.senderId();
            LOG.debug("senderId: {}", senderId);
            final String recipientId = event.recipientId();
            LOG.debug("recipientId: {}", recipientId);
            final String passThroughParam = event.refPayload().orElse("empty payload");
            LOG.debug("passThroughParam: {}", passThroughParam);
            final Instant timestamp = event.timestamp();
            LOG.debug("timestamp: {}", timestamp);
            LOG.info("Received authentication for user '{}' and page '{}' with pass through param '{}' at '{}'", senderId, recipientId, passThroughParam, timestamp);
//            sendTextMessage(senderId, "Authentication successful", true);
            return "SUCCESS";
        } catch (Exception ex) {
            LOG.error("{} Error while passing thread control: {} ", senderId, ex);
            return "EXCEPTION";
        }
    }
}
