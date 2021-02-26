package com.kalsym.facebook.wrapper.handover;

import com.github.messenger4j.Messenger;
import com.github.messenger4j.handover.HandoverPassThreadControlPayload;
import com.github.messenger4j.handover.HandoverPayload;
import com.github.messenger4j.handover.HandoverResponse;
import com.github.messenger4j.handover.SecondaryReceiversResponse;
import com.github.messenger4j.handover.ThreadOwnerResponse;
import com.github.messenger4j.webhook.event.AppRolesEvent;
import com.github.messenger4j.webhook.event.BaseEventType;
import com.github.messenger4j.webhook.event.PassThreadControlEvent;
import com.github.messenger4j.webhook.event.RequestThreadControlEvent;
import com.github.messenger4j.webhook.event.TakeThreadControlEvent;
import com.kalsym.facebook.wrapper.config.ConfigReader;
import java.time.Instant;
import static java.util.Optional.of;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author z33Sh
 */
public class HandoverHelperFacebook {

    private static final Logger LOG = LoggerFactory.getLogger("application");

    /**
     * Attempts to pass control of recipient conversation to secondary receiver
     *
     * @param messenger
     * @param recipient
     * @param message
     * @return
     */
    public static HandoverResponse handoverToSecondaryReceiver(Messenger messenger, String recipient, String message) {

        LOG.info("Received handoverToSecondaryReceiver from '{}'", recipient);
        try {
            HandoverPayload handoverPayload = HandoverPassThreadControlPayload.create(recipient, ConfigReader.environment.getProperty("secondary_receiver_app_id", "835103589938459"), of(message));
            HandoverResponse res = messenger.passThreadControl(handoverPayload);
            LOG.info("{} pass handover response: {}", recipient, res);
            return res;
        } catch (Exception ex) {
            LOG.error("{} Error: {} ", recipient, ex);
            return null;
        }
    }

    /**
     * Attempts to take back control of recipient conversation from secondary
     * receiver
     *
     * @param messenger
     * @param recipient
     * @return
     */
    public static HandoverResponse takeFromSecondaryReceiver(Messenger messenger, String recipient) {
        LOG.info("Received takeFromSecondaryReceiver for '{}'", recipient);
        try {
            HandoverPayload handoverPayload = HandoverPassThreadControlPayload.create(recipient, ConfigReader.environment.getProperty("secondary_receiver_app_id", "835103589938459"), of("Message from user: handover content"));

            HandoverResponse res = messenger.takeThreadControl(handoverPayload);
            LOG.info("{} take handover response: {}", recipient, res);
            return res;
        } catch (Exception ex) {
            LOG.error("{} Error: {} ", recipient, ex);
            return null;
        }
    }

    /**
     * Queries secondary receivers and current owner of recipient conversation.
     * And sends as message to user
     *
     * @param messenger
     * @param recipient
     * @return
     */
    public static ThreadOwnerResponse getThreadOwner(Messenger messenger, String recipient) {
        LOG.debug("Handling getThreadOwner for {}", recipient);
        try {
            SecondaryReceiversResponse receivers = messenger.getSecondaryReceivers();
            LOG.info("{} Secondary receivers: {}", recipient, receivers);

            ThreadOwnerResponse res = messenger.getThreadOwner(recipient);
            LOG.info("{} get thread owners response: {}", recipient, res);

            return res;
        } catch (Exception ex) {
            LOG.error("{} Error: {} ", recipient, ex);
            return null;
        }
    }

    /**
     *
     * @param event
     */
    public static void handlePassThreadControlEvent(PassThreadControlEvent event) {
        LOG.debug("Handling PassThreadControlEvent");
        final String senderId = event.senderId();
        LOG.debug("senderId: {}", senderId);
        final BaseEventType baseEventType = event.baseEventType();
        LOG.debug("baseEventType: {}", baseEventType);
        final Instant timestamp = event.timestamp();
        LOG.debug("timestamp: {}", timestamp);

        event.getPassThreadControl().ifPresent(passThreadControl -> {
            LOG.info(String.format("Now this app has the control. Received -> %s", passThreadControl));
//            sendTextMessage(event.senderId(), "Hi I'm the App again", true);
        });
    }

    /**
     *
     * @param event
     */
    public static void handleRequestThreadControlEvent(RequestThreadControlEvent event) {
        LOG.debug("Handling RequestThreadControlEvent");
        final String senderId = event.senderId();
        LOG.debug("senderId: {}", senderId);
        final String recipientId = event.recipientId();
        LOG.debug("recipientId: {}", recipientId);
        final BaseEventType baseEventType = event.baseEventType();
        LOG.debug("baseEventType: {}", baseEventType);
        final Instant timestamp = event.timestamp();
        LOG.debug("timestamp: {}", timestamp);

        event.getRequestThreadControl().ifPresent(requestThreadControl -> {
            /**
             * is up to you to pass the control to the secondary receiver
             * messenger.passThreadControl();
             *
             */

            LOG.info(String.format("Secondary Receiver requested to this app has the control. Received -> %s", requestThreadControl));
        });

    }

    /**
     *
     * @param event
     */
    public static void handleTakeThreadControlEvent(TakeThreadControlEvent event) {
        LOG.debug("Handling TakeThreadControlEvent");
        final String senderId = event.senderId();
        LOG.debug("senderId: {}", senderId);
        final String recipientId = event.recipientId();
        LOG.debug("recipientId: {}", recipientId);
        final BaseEventType baseEventType = event.baseEventType();
        LOG.debug("baseEventType: {}", baseEventType);
        final Instant timestamp = event.timestamp();
        LOG.debug("timestamp: {}", timestamp);

        event.getTakeThreadControl().ifPresent(takeThreadControl -> {
            /**
             * is up to you to request again the control to the primary receiver
             * messenger.requestThreadControl();
             *
             */
            LOG.info(String.format("Primary Receiver has taken away the app control. Received -> %s", takeThreadControl));
        });

    }

    /**
     *
     * @param event
     */
    public static void handleAppRolesEvent(AppRolesEvent event) {
        LOG.debug("Handling AppRolesEvent");
        final String recipientId = event.recipientId();
        LOG.debug("recipientId: {}", recipientId);
        final BaseEventType baseEventType = event.baseEventType();
        LOG.debug("baseEventType: {}", baseEventType);
        final Instant timestamp = event.timestamp();
        LOG.debug("timestamp: {}", timestamp);

        event.getAppRoles().ifPresent(appRoles -> {
            /**
             * Triggered when App Roles are changed from Page configuration
             *
             */
            LOG.info(String.format("App Roles updated. Received -> %s", appRoles));
        });

    }

}
