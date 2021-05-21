package com.kalsym.facebook.wrapper.sender;

import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerApiExceptionFactory;
import com.github.messenger4j.internal.gson.GsonFactory;
import com.github.messenger4j.messengerprofile.SetupResponse;
import com.github.messenger4j.messengerprofile.SetupResponseFactory;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.spi.MessengerHttpClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kalsym.facebook.wrapper.config.ConfigReader;
import java.io.IOException;
import java.util.Optional;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author z33Sh
 */
public class GraphApiSender {

    private final static Logger LOG = LoggerFactory.getLogger("application");
    String token;
    private final Gson gson;
    RestTemplate restTemplate = new RestTemplate();
    HttpEntity<String> entity;

    String url = ConfigReader.environment.getProperty("graph.api.url", "https://graph.facebook.com/v2.11/me/messages");

    public GraphApiSender(String token) {
        this.token = token;
        this.gson = GsonFactory.createGson();

    }

    /**
     *
     * @param payload
     * @return
     * @throws java.io.IOException
     */
    public MessengerHttpClient.HttpResponse sendMessage(MessagePayload payload) throws IOException {
        try {
            Optional<Object> pay = of(payload);
            String urlWithToken = url.trim() + "?access_token=" + token;
            LOG.info("urlWithToken:{}", urlWithToken);

            DefaultMessengerHttpClient dmh = new DefaultMessengerHttpClient();
            Optional<String> jsonBody = pay.map(gson::toJson);
            MessengerHttpClient.HttpResponse httpResponse = dmh.execute(MessengerHttpClient.HttpMethod.POST, urlWithToken, jsonBody.orElse(null));

            return httpResponse;
        } catch (IOException ex) {
            throw ex;
        }
    }

    public class DefaultMessengerHttpClient implements MessengerHttpClient {

        private static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8";

        private final OkHttpClient okHttp = new OkHttpClient();

        @Override
        public MessengerHttpClient.HttpResponse execute(MessengerHttpClient.HttpMethod httpMethod, String url, String jsonBody) throws IOException {
            final Request.Builder requestBuilder = new Request.Builder().url(url);
            if (httpMethod != MessengerHttpClient.HttpMethod.GET) {
                final MediaType jsonMediaType = MediaType.parse(APPLICATION_JSON_CHARSET_UTF_8);
                final RequestBody requestBody = RequestBody.create(jsonMediaType, jsonBody);
                requestBuilder.method(httpMethod.name(), requestBody);
            }
            final Request request = requestBuilder.build();
            try (Response response = this.okHttp.newCall(request).execute()) {
                return new MessengerHttpClient.HttpResponse(response.code(), response.body().string());
            }
        }
    }
}
