package com.kalsym.facebook.wrapper.handover;

import static com.kalsym.facebook.wrapper.Application.agent_sessions;
import com.kalsym.facebook.wrapper.config.ConfigReader;
import com.kalsym.facebook.wrapper.models.RequestPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author z33Sh
 */
public class HandoverHelper {

    private static final Logger LOG = LoggerFactory.getLogger("application");

    public static ResponseEntity<String> passConverationToAgentService(String senderId, String message, String refId, String referenceId) {
        // put new session ID for this sender
        agent_sessions.put(senderId, refId);
        return sendMessageToAgent(senderId, message, refId, referenceId);
    }

    public static ResponseEntity<String> sendMessageToAgent(String senderId, String message, String refId, String referenceId) throws RestClientException {
        String handoverServiceUrl = ConfigReader.environment.getProperty("handover.service.url", "127.0.0.1");
        int handoverServicePort = ConfigReader.getPropertyAsInt("handover.service.port", 8080);
        String handoverServiceRequestPath = ConfigReader.environment.getProperty("handover.service.request.path", "/inbound/customer/message");
        LOG.info("[{}] url:{} port:{}  and endpoint:{}", senderId, handoverServiceUrl, handoverServicePort, handoverServiceRequestPath);
        String isGuest = "true";
        final String queryParams = "senderId=" + senderId + "&refrenceId=" + refId;
        /* forward to handover service for */

        RequestPayload data = new RequestPayload(message, "", refId, Boolean.parseBoolean(isGuest), "http://" + ConfigReader.environment.getProperty("server.address", "127.0.0.1") + ":" + ConfigReader.environment.getProperty("server.port", "8080") + "/",referenceId);
        RestTemplate restTemplate = new RestTemplate();
        LOG.info("[{}] url:{} queryParams:{}  and payload:{}", senderId, queryParams, data.toString());

        ResponseEntity<String> response = restTemplate.postForEntity("http://" + handoverServiceUrl + ":" + handoverServicePort + handoverServiceRequestPath + "?" + queryParams, data, String.class);
        return response;
    }
}
