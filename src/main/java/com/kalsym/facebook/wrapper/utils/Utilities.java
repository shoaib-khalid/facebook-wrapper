package com.kalsym.facebook.wrapper.utils;

import com.kalsym.facebook.wrapper.config.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author z33Sh
 */
public class Utilities {

    private static final Logger LOG = LoggerFactory.getLogger("application");
    private static final Logger cdrWriter = LoggerFactory.getLogger("cdr");
    private final static String backendIP = ConfigReader.environment.getProperty("backend.ip", "127.0.0.1");
    private final static int backendPort = ConfigReader.getPropertyAsInt("backend.port", 9100);

    public static void writeCDR(String refId, String currEvent, String aParty, String bParty, String param1, String param2) {
        try {
            String rowData = refId + "," + currEvent + "," + aParty + "," + bParty + "," + param1 + "," + param2;
            cdrWriter.info(rowData);
        } catch (Exception ex) {
            LOG.error("Log CDR fail " + refId + " error:", ex);
        }
    }

    /**
     *
     * @param msisdn
     * @param countryCode
     * @return
     */
    public static String normalizeNumberWithCountryCode(String msisdn, String countryCode) {
        msisdn = msisdn.trim();
        if (msisdn.startsWith("00" + countryCode)) {
            msisdn = msisdn.substring(2);
        } else if (msisdn.startsWith("0")) {
            msisdn = msisdn.substring(1);
            msisdn = countryCode + msisdn;
        }
        return msisdn;
    }

    /**
     * Sends post request to backend Server
     *
     * @param backendEndPoint
     * @param queryParams
     * @param requestBody
     * @return
     */
    public static String sendPostRequest(String backendEndPoint, String queryParams, String requestBody) {
        // TODO: Initialize following things using @Autowired if possible. 
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        String response = "";
        try {
            LOG.debug("Trying to send http request to server on requestBody: " + requestBody);
            LOG.debug("Request : " + "http://" + backendIP + ":" + backendPort + "/" + backendEndPoint + "/?" + queryParams);

            response = restTemplate.postForObject("http://" + backendIP + ":" + backendPort + "/" + backendEndPoint + "/?" + queryParams, entity, String.class);
            LOG.info("got response : " + response);
            return response;
        } catch (Exception ex) {
            LOG.error("Error while sending request", ex);
            return "Exception";
        }
    }

}
