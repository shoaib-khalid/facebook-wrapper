package com.kalsym.facebook.wrapper.callback.handlers;

import com.github.messenger4j.webhook.event.AccountLinkingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author z33Sh
 */
public class AccountLinkingHandler {

    private static final Logger LOG = LoggerFactory.getLogger("application");

    /**
     *
     * @param event
     * @return
     */
    public static String handleAccountLinkingEvent(AccountLinkingEvent event) {
        String senderId = "";
        try {
            LOG.debug("Handling AccountLinkingEvent");
            senderId = event.senderId();
            LOG.debug("senderId: {}", senderId);
            final AccountLinkingEvent.Status accountLinkingStatus = event.status();
            LOG.debug("accountLinkingStatus: {}", accountLinkingStatus);
            final String authorizationCode = event.authorizationCode().orElse("Empty authorization code!!!"); // You can throw an Exception
            LOG.debug("authorizationCode: {}", authorizationCode);
            LOG.info("Received account linking event for user '{}' with status '{}' and auth code '{}'", senderId, accountLinkingStatus, authorizationCode);
//            sendTextMessage(senderId, "AccountLinking event tapped", true);
            return "SUCCESS";
        } catch (Exception ex) {
            LOG.error("{} Error while passing thread control: {} ", senderId, ex);
            return "EXCEPTION";
        }
    }
}
