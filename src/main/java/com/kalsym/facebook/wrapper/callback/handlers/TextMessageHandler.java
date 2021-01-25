package com.kalsym.facebook.wrapper.callback.handlers;

import com.github.messenger4j.Messenger;
import static com.github.messenger4j.webhook.event.BaseEventType.STANDBY;
import com.github.messenger4j.webhook.event.TextMessageEvent;
import com.kalsym.facebook.wrapper.config.ConfigReader;
import com.kalsym.facebook.wrapper.handover.HandoverHelper;
import com.kalsym.facebook.wrapper.models.RequestPayload;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author z33Sh
 */
public class TextMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger("application");

    public static ResponseEntity<String> handleTextMessageEvent(Messenger messenger, TextMessageEvent event) {
        try {
            LOG.debug("Received TextMessageEvent: {}", event);
            final String messageId = event.messageId();
            String messageText = event.text();
            messageText = messageText.replaceAll("\"", "\"\"");
            final String senderId = event.senderId();
            final String recipientId = event.recipientId();
            final Instant timestamp = event.timestamp();
            if (event.baseEventType() != STANDBY) {
                final String backEndMessageEndpoint = ConfigReader.environment.getProperty("backend.message.endpoint", "callback/message/");
//            final String backEndPostbackEndpoint = ConfigReader.environment.getProperty("backend.postback.endpoint", "callback/postback");

                LOG.info("Received message '{}' with text '{}' from user '{}' to user {} at '{}'", messageId, messageText, senderId, recipientId, timestamp);
                String isGuest = "true";
                LOG.debug("isGuest: {}", isGuest);
                final String queryParams = "senderId=" + senderId + "&refrenceId=" + ConfigReader.environment.getProperty("backend.refrenced.id", "");
                /* forward to backend for */
                RequestPayload data = new RequestPayload(messageText, "", timestamp.toString(), Boolean.parseBoolean(isGuest), "http://" + ConfigReader.environment.getProperty("server.address", "127.0.0.1") + ":" + ConfigReader.environment.getProperty("server.port", "8080") + "/");
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<String> response = restTemplate.postForEntity("http://" + ConfigReader.environment.getProperty("backend.ip", "127.0.0.1") + ":" + ConfigReader.environment.getProperty("backend.port", "8080") + "/inbound/" + "?" + queryParams, data, String.class);
                return response;
            } else {

                // message received in standby
                switch (messageText.toLowerCase()) {
                    case "takeback": //
                        HandoverHelper.takeFromSecondaryReceiver(messenger, senderId);
                        break;
                    case "bye": //
                        HandoverHelper.takeFromSecondaryReceiver(messenger, senderId);
                        break;
                    case "getowner": //
                        HandoverHelper.getThreadOwner(messenger, senderId);
                        break;
                    default:
                        LOG.debug("{} message is {} in STANDBY mode, ignore", senderId, messageText);
                }
                return ResponseEntity.status(HttpStatus.OK).build();
            }

        } catch (Exception e) {
            LOG.error("Message could not be sent to backend. An unexpected error occurred.", e);
            return null;
        }
    }

}
