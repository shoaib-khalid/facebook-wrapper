package com.kalsym.facebook.wrapper.callback.handlers;

import com.kalsym.facebook.wrapper.models.RequestPayload;
import com.github.messenger4j.webhook.event.PostbackEvent;
import com.kalsym.facebook.wrapper.config.ConfigReader;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author z33Sh
 */
public class PostbackHandler {

    private static final Logger LOG = LoggerFactory.getLogger("application");

    public static ResponseEntity<String> handlePostbackEvent(PostbackEvent event) {
        try {
            LOG.debug("Handling PostbackEvent");
            final String payload = event.payload().orElse("empty payload");
            LOG.debug("payload title: {} payload data: {} event referal: {}", event.title(), payload, event.referral());
            String senderId = event.senderId();
            LOG.debug("senderId: {}", senderId);
            final Instant timestamp = event.timestamp();
            LOG.debug("timestamp: {}", timestamp);
            String isGuest = "true";
            try {
                if (event.referral().isPresent()) {
                    isGuest = event.referral().get().isGuestUser().orElse("true");
                }
            } catch (Exception ex) {
                LOG.debug("isGuest:{} error {}", isGuest, ex);
            }
            LOG.debug("isGuest: {}", isGuest);
            LOG.info("Received postback for user '{}' and page '{}' with payload '{}' at '{}'", senderId, senderId, payload, timestamp);
            final String queryParams = "senderId=" + senderId + "&refrenceId=" + ConfigReader.environment.getProperty("backend.refrenced.id", "");
            /* forward to backend for*/ RequestPayload data = new RequestPayload(payload, "", timestamp.toString(), Boolean.parseBoolean(isGuest), "http://" + ConfigReader.environment.getProperty("server.address", "127.0.0.1") + ":" + ConfigReader.environment.getProperty("server.port", "8080") + "/");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity("http://" + ConfigReader.environment.getProperty("backend.ip", "127.0.0.1") + ":" + ConfigReader.environment.getProperty("backend.port", "8080") + "/inbound/postback/?" + queryParams, data, String.class);
            LOG.info("got response : " + response);
            return response;
        } catch (Exception ex) {
            LOG.error("Message could not be sent to backend. An unexpected error occurred.", ex);
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
    }
}
