package com.kalsym.facebook.wrapper.callback;

import static com.github.messenger4j.Messenger.CHALLENGE_REQUEST_PARAM_NAME;
import static com.github.messenger4j.Messenger.MODE_REQUEST_PARAM_NAME;
import static com.github.messenger4j.Messenger.SIGNATURE_HEADER_NAME;
import static com.github.messenger4j.Messenger.VERIFY_TOKEN_REQUEST_PARAM_NAME;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import com.github.messenger4j.Messenger;
import com.github.messenger4j.common.WebviewHeightRatio;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.exception.MessengerVerificationException;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.CallButton;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.button.UrlButton;
import com.github.messenger4j.send.recipient.IdRecipient;
import com.github.messenger4j.send.recipient.Recipient;
import com.github.messenger4j.send.recipient.UserRefRecipient;
import com.github.messenger4j.spi.MessengerHttpClient;
import com.github.messenger4j.webhook.Event;
import static com.github.messenger4j.webhook.event.BaseEventType.STANDBY;
import com.github.messenger4j.webhook.event.QuickReplyMessageEvent;
import com.github.messenger4j.webhook.event.TextMessageEvent;
import static com.kalsym.facebook.wrapper.Application.agent_sessions;
import static com.kalsym.facebook.wrapper.callback.handlers.AccountLinkingHandler.handleAccountLinkingEvent;
import static com.kalsym.facebook.wrapper.callback.handlers.AttachmentMessageHandler.handleAttachmentMessageEvent;
import static com.kalsym.facebook.wrapper.callback.handlers.MessageEchoHandler.handleMessageEchoEvent;
import com.kalsym.facebook.wrapper.callback.handlers.PostbackHandler;
import com.kalsym.facebook.wrapper.callback.handlers.TextMessageHandler;
import com.kalsym.facebook.wrapper.config.ConfigReader;
import com.kalsym.facebook.wrapper.enums.ButtonType;
import com.kalsym.facebook.wrapper.handover.HandoverHelper;
import com.kalsym.facebook.wrapper.handover.HandoverHelperFacebook;
import com.kalsym.facebook.wrapper.models.MenuItem;
import com.kalsym.facebook.wrapper.models.RequestPayload;
import com.kalsym.facebook.wrapper.models.FbWrapperSession;
import com.kalsym.facebook.wrapper.repository.AppTokenRepository;
import com.kalsym.facebook.wrapper.repository.FbWrapperSessionRepository;
import com.kalsym.facebook.wrapper.sender.SendHelper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author z33Sh
 */
@RestController
@RequestMapping("/callback")
public class CallbackController {

    private static final Logger LOG = LoggerFactory.getLogger("application");
    private final Messenger messenger;
    @Autowired
    private Environment env;

    @Autowired
    private AppTokenRepository appTokenRepository;

    @Autowired
    private FbWrapperSessionRepository fbWrapperSessionRepository;

    @Autowired
    public CallbackController(final Messenger messenger) {
        this.messenger = messenger;
    }

    /**
     * WebHook verification endpoint.
     *
     * <p>
     * The passed verification token (as query parameter) must match the
     * configured verification token. In case this is true, the passed challenge
     * string must be returned by this endpoint.
     *
     * @param mode
     * @param verifyToken
     * @param challenge
     * @return
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<String> verify(
            @RequestParam(MODE_REQUEST_PARAM_NAME) final String mode,
            @RequestParam(VERIFY_TOKEN_REQUEST_PARAM_NAME) final String verifyToken,
            @RequestParam(CHALLENGE_REQUEST_PARAM_NAME) final String challenge) {
        LOG.info(
                "Received Webhook verification request - mode: {} | verifyToken: {} | challenge: {}",
                mode,
                verifyToken,
                challenge);
        try {
            this.messenger.verifyWebhook(mode, verifyToken);
            return ResponseEntity.ok(challenge);
        } catch (MessengerVerificationException e) {
            LOG.warn("Webhook verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * Callback endpoint responsible for processing the inbound messages and
     * events.
     *
     * @param payload
     * @param signature
     * @return
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Void> handle(
            @RequestBody final String payload,
            @RequestHeader(SIGNATURE_HEADER_NAME) final String signature) {
        LOG.info(
                "Received Messenger Platform callback - payload: {} | signature: {}", payload, signature);
        try {
            this.messenger.onReceiveEvents(payload,
                    of(signature),
                    event -> {

                        if (event.isTextMessageEvent()) {
//                            handleTextMessageEventTest1(event.asTextMessageEvent());
                            TextMessageHandler.handleTextMessageEvent(messenger, event.asTextMessageEvent());
                        } else if (event.isAttachmentMessageEvent()) {
                            handleAttachmentMessageEvent(event.asAttachmentMessageEvent());
                        } else if (event.isQuickReplyMessageEvent()) {
                            handleQuickReplyMessageEvent(event.asQuickReplyMessageEvent());
                        } else if (event.isPostbackEvent()) {
                            PostbackHandler.handlePostbackEvent(event.asPostbackEvent());
                        } else if (event.isAccountLinkingEvent()) {
                            handleAccountLinkingEvent(event.asAccountLinkingEvent());
                        } else if (event.isOptInEvent()) {
                            //                    handleOptInEvent(event.asOptInEvent());
                        } else if (event.isMessageEchoEvent()) {
                            handleMessageEchoEvent(event.asMessageEchoEvent());
                        } else if (event.isMessageDeliveredEvent()) {
                            //                    handleMessageDeliveredEvent(event.asMessageDeliveredEvent());
                        } else if (event.isMessageReadEvent()) {
                            //                    handleMessageReadEvent(event.asMessageReadEvent());
                        } else if (event.isReferralEvent()) {
                            handleReferrelEvent(event);
                        } else if (event.isPassThreadControlEvent()) {
                            HandoverHelperFacebook.handlePassThreadControlEvent(event.asPassThreadControlEvent());
                        } else if (event.isRequestThreadControlEvent()) {
                            HandoverHelperFacebook.handleRequestThreadControlEvent(event.asRequestThreadControlEvent());
                        } else if (event.isTakeThreadControlEvent()) {
                            HandoverHelperFacebook.handleTakeThreadControlEvent(event.asTakeThreadControlEvent());
                        } else if (event.isAppRolesEvent()) {
                            HandoverHelperFacebook.handleAppRolesEvent(event.asAppRolesEvent());
                        } else {
                            handleFallbackEvent(event);
                        }
                    });
            LOG.debug("Processed callback payload successfully");
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (MessengerVerificationException e) {
            LOG.warn("Processing of callback payload failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Callback endpoint responsible for processing the outbound simple message
     * to be sent to messenger psid(s)
     *
     * @param requestData
     * @return
     */
    @RequestMapping(value = "/textmessage/push/", method = RequestMethod.POST, consumes = "Application/json")
    public ResponseEntity<Void> pushSimpleMessage(@RequestBody com.kalsym.facebook.wrapper.models.PushMessage requestData) {
        try {
            LOG.info("Received /textmessage/push/ request [{}] ", requestData.toString());
            final List<String> recipientIds = requestData.getRecipientIds();
            final String refId = requestData.getRefId();
            final String message = requestData.getMessage();

            LOG.info("[{}] received /textmessage/push/ request for recipients [{}] with message [{}] and referenceId [{}]", refId, recipientIds.toString(), message, requestData.getReferenceId());
            recipientIds.forEach(recip -> {
                String recipient = (String) recip;
                String refToken = appTokenRepository.getAppToken(requestData.getReferenceId());
                if (null == requestData.getReferenceId() || null == refToken || refToken.isEmpty()) {
                    LOG.info("[{}] token empty from database for referenceId [[]], using default property messenger.pageAccessToken", refId, requestData.getReferenceId());
                    refToken = env.getProperty("messenger.pageAccessToken");
                }
                MessengerHttpClient.HttpResponse resp = SendHelper.sendTextMessage(messenger, recipient, message, requestData.isGuest(), refToken);
                LOG.debug("[{}] pushed message to [{}] with response [{}:{}]", refId, recipient, resp.statusCode(), resp.body());
                if (agent_sessions.containsKey(recip)) {

                    fbWrapperSessionRepository.deleteById(recip);
                    LOG.info("[{}] [{}] Removed fb wrapper session from database  ", refId, recip);
                }
            });
        } catch (Exception ex) {
            LOG.warn("Processing of push simple message failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Callback endpoint responsible for processing the outbound media message
     * to be sent to messenger psid(s). Media type could be IMAGE, FILE, AUDIO,
     * VIDEO or GIF
     *
     * @param requestData
     * @return
     */
    @RequestMapping(value = "/mediamessage/push/", method = RequestMethod.POST, consumes = "Application/json")
    public ResponseEntity<Void> pushMediaMessage(@RequestBody com.kalsym.facebook.wrapper.models.PushMessage requestData) {
        try {
            LOG.debug("Received media message request [{}] ", requestData.toString());
            final List<String> recipientIds = requestData.getRecipientIds();
            final String refId = requestData.getRefId();
            final String mediaType = requestData.getUrlType();
            final String mediaUrl = requestData.getUrl();

            LOG.info("[{}] received media message request for recipients [{}] with url [{}] of type [{}] ", refId, recipientIds.toString(), mediaUrl, mediaType);
            recipientIds.forEach(recip -> {
                String recipient = (String) recip;
                try {
                    MessengerHttpClient.HttpResponse resp;
                    String refToken = appTokenRepository.getAppToken(requestData.getReferenceId());
                    if (null == requestData.getReferenceId() || null == refToken || refToken.isEmpty()) {
                        LOG.info("[{}] token empty from database for referenceId [[]], using default property messenger.pageAccessToken", refId, requestData.getReferenceId());
                        refToken = env.getProperty("messenger.pageAccessToken");
                    }
                    if ("IMAGE".equalsIgnoreCase(mediaType)) {
                        resp = SendHelper.sendImageMessage(messenger, recipient, mediaUrl, refToken);
                        LOG.debug("[{}] pushed media message to [{}] with response [{}:{}]", refId, recipient, resp.statusCode(), resp.body());
                    } else if ("GIF".equalsIgnoreCase(mediaType)) {

                        resp = SendHelper.sendGifMessage(messenger, recipient, mediaUrl, refToken);
                        LOG.debug("[{}] pushed media message to [{}] with response [{}:{}]", refId, recipient, resp.statusCode(), resp.body());
                    } else if ("VIDEO".equalsIgnoreCase(mediaType)) {

                        resp = SendHelper.sendVideoMessage(messenger, recipient, mediaUrl, refToken);
                        LOG.debug("[{}] pushed media message to [{}] with response [{}:{}]", refId, recipient, resp.statusCode(), resp.body());
                    } else if ("AUDIO".equalsIgnoreCase(mediaType)) {
                        resp = SendHelper.sendAudioMessage(messenger, recipient, mediaUrl, refToken);
                        LOG.debug("[{}] pushed media message to [{}] with response [{}:{}]", refId, recipient, resp.statusCode(), resp.body());
                    } else if ("FILE".equalsIgnoreCase(mediaType)) {

                        resp = SendHelper.sendFileMessage(messenger, recipient, mediaUrl, refToken);
                        LOG.debug("[{}] pushed media message to [{}] with response [{}:{}]", refId, recipient, resp.statusCode(), resp.body());
                    } else {
                        LOG.debug("[{}] [{}] unsupported media type [{}]", refId, recipient, mediaType);
                    }
                    if (agent_sessions.containsKey(recip)) {

                        fbWrapperSessionRepository.deleteById(recip);
                        LOG.info("[{}] [{}] Removed fb wrapper session from database  ", refId, recip);
                    }
                } catch (Exception ex) {
                    LOG.warn("Processing of push media message failed: {}", ex.getMessage());
                }
            });
        } catch (Exception ex) {
            LOG.warn("Processing of push media message failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Callback endpoint responsible for processing the outbound generic
     * template message to be sent to messenger psid(s)
     *
     * @param requestData
     * @return
     */
    @RequestMapping(value = "/menumessage/push/", method = RequestMethod.POST, consumes = "Application/json")
    public ResponseEntity<Void> pushMenuMessage(@RequestBody com.kalsym.facebook.wrapper.models.PushMessage requestData) {
        try {
            LOG.debug("Received menu message request [{}]", requestData.toString());

            final List<String> recipientIds = requestData.getRecipientIds();
            final String refId = requestData.getRefId();
            final String title = requestData.getTitle();
            final String subTitle = requestData.getSubTitle();
            final String url = requestData.getUrl();
            final String UrlType = requestData.getUrlType();
            LOG.info("[{}] received menu message request for recipients [{}] with title [{}] subtitle [{}] url [{}] urlType [{}]", refId, recipientIds.toString(), title, subTitle, url, UrlType);
            List<MenuItem> menuItems = requestData.getMenuItems();
            final List<Button> buttons = new ArrayList<>();
            menuItems.forEach(menuButton -> {
                MenuItem button = (MenuItem) menuButton;
                final String buttonTitle = button.getTitle();
                final String payload = button.getPayload();
                if (button.getType().equalsIgnoreCase(ButtonType.url.toString())) {
                    try {
                        buttons.add(UrlButton.create(buttonTitle, new URL(payload), of(WebviewHeightRatio.COMPACT), of(false), empty(), empty()));
                    } catch (Exception ex) {
                        LOG.warn("[{}] malformed URL in payload [{}]", refId, payload);
                    }
                } else if (button.getType().equalsIgnoreCase(ButtonType.number.toString())) {
                    buttons.add(CallButton.create(buttonTitle, payload));
                } else {
                    buttons.add(PostbackButton.create(buttonTitle, payload));
                }
            });
            recipientIds.forEach(recip -> {
                Recipient recipient;
                if (requestData.isGuest()) {
                    /* if user is guest, use id in recipient */
                    recipient = IdRecipient.create(recip);
                } else {
                    /* if user is not guest, use user_ref in recipient */
                    recipient = UserRefRecipient.create(recip);
                }
                MessengerHttpClient.HttpResponse resp = null;
                try {
                    String refToken = appTokenRepository.getAppToken(requestData.getReferenceId());
                    if (null == requestData.getReferenceId() || null == refToken || refToken.isEmpty()) {
                        LOG.info("[{}] token empty from database for referenceId [[]], using default property messenger.pageAccessToken", refId, requestData.getReferenceId());
                        refToken = env.getProperty("messenger.pageAccessToken");
                    }
                    resp = SendHelper.sendMenuMessage(messenger, recipient, title, subTitle, url, buttons, refToken);
                    LOG.debug("[{}] pushed media message to [{}] with response [{}:{}]", refId, recipient, resp.statusCode(), resp.body());
                    if (agent_sessions.containsKey(recip)) {

                        fbWrapperSessionRepository.deleteById(recip);
                        LOG.info("[{}] [{}] Removed fb wrapper session from database  ", refId, recip);
                    }
                } catch (MessengerApiException | MessengerIOException | MalformedURLException ex) {
                    LOG.error("[{}][{}] exception sending menu message [{}]", refId, recipient, ex);
                } catch (IOException ex) {
                    LOG.error("[{}][{}] exception sending menu message [{}]", refId, recipient, ex);
                }
                LOG.debug("[{}] pushed message to [{}] with response [{}]", refId, recipient, resp);
            });
        } catch (Exception ex) {
            LOG.warn("Processing of push simple message failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Callback endpoint for passing conversation control to agent service
     *
     * @param requestData
     * @return
     */
    @RequestMapping(value = "/conversation/pass/", method = RequestMethod.POST, consumes = "Application/json")
    public ResponseEntity<Void> passConversationToCustomerService(@RequestBody com.kalsym.facebook.wrapper.models.Conversation requestData) {
        try {
            LOG.debug("Received pass conversation control request [{}] ", requestData.toString());
            final List<String> recipientIds = requestData.getRecipientIds();
            final String refId = requestData.getRefId();
            final String message = requestData.getMessage();
            final String referenceId = requestData.getReferenceId();
            LOG.info("[{}] received pass conversation control request for recipients [{}] with message [{}]", refId, recipientIds.toString(), message);
            recipientIds.forEach(recip -> {
                String recipient = (String) recip;
//                HandoverResponse resp = HandoverHelperFacebook.handoverToSecondaryReceiver(messenger, recipient, message);
                ResponseEntity<String> resp = HandoverHelper.passConverationToAgentService(recipient, message, refId, referenceId);
                LOG.info("[{}] [{}] handover service response [{}]", refId, recipient, resp);
                Timestamp ts = new Timestamp(System.currentTimeMillis());
                FbWrapperSession fbwSession = new FbWrapperSession(recipient, ts);
                fbWrapperSessionRepository.save(fbwSession);
                LOG.info("[{}] [{}] Saved fb wrapper session in database  ", refId, recipient);

            });
        } catch (Exception ex) {
            LOG.warn("Processing of pass handover failed: {}", ex);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Callback endpoint for taking conversation control from CS agents portal
     *
     * @param requestData
     * @return
     */
    @RequestMapping(value = "/conversation/handle/", method = RequestMethod.POST, consumes = "Application/json")
    public ResponseEntity<Void> takeConversationFromCustomerService(@RequestBody com.kalsym.facebook.wrapper.models.PushMessage requestData) {
        try {
            try {
                LOG.info("Received conversation handle request [{}] ", requestData.toString());
                final List<String> recipientIds = requestData.getRecipientIds();
                final String refId = requestData.getRefId();
                final String message = requestData.getMessage();
                final String referenceId = requestData.getReferenceId();
                LOG.debug("[{}] received conversation handle request for recipients [{}] with message [{}] ", refId, recipientIds.toString(), message);
                recipientIds.forEach(senderId -> {
                    if (agent_sessions.containsKey(senderId)) {
                        agent_sessions.remove(senderId);
                        LOG.debug("[{}] [{}] Removed session for user ", refId, senderId);
                    }
                    try {
                        fbWrapperSessionRepository.deleteById(senderId);
                        LOG.info("[{}] [{}] Removed fb wrapper session from database  ", refId, senderId);
                    } catch (Exception ex) {
                         LOG.error("Error sending message to core", ex);
                    }
                    try {
                        final String queryParams = "senderId=" + senderId + "&refrenceId=" + refId;

                        String messageText = "{\n"
                                + "    \"event\":\"Message\",\n"
                                + "    \"agentName\":\"" + requestData.getTitle() + "\"\n"
                                + "}";
                        RequestPayload data = new RequestPayload(messageText, requestData.getMessage(), "RocketChat", Boolean.FALSE, "http://" + ConfigReader.environment.getProperty("server.url", "127.0.0.1") + ":" + ConfigReader.environment.getProperty("server.port", "8080") + "/", referenceId);
                        RestTemplate restTemplate = new RestTemplate();
                        ResponseEntity<String> response = restTemplate.postForEntity("http://" + ConfigReader.environment.getProperty("backend.ip", "127.0.0.1") + ":" + ConfigReader.environment.getProperty("backend.port", "8080") + "/inbound/" + "?" + queryParams, data, String.class);
                        LOG.info("[{}] [{}] sent message  to core", refId, senderId, data);
                        LOG.info("[{}] [{}] response from core: ", refId, senderId, response);
                    } catch (Exception e) {
                        LOG.error("Error sending message to core", e);
                    }
                });
            } catch (Exception ex) {
                LOG.warn("Processing of push simple message failed: {}", ex.getMessage());
            }

        } catch (JSONException ex) {
            LOG.error("Processing of take handover failed ", ex);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    private void handleTextMessageEventTest1(TextMessageEvent event) {
        LOG.debug("Received TextMessageEvent: {}", event);
        final String messageId = event.messageId();
        final String messageText = event.text();
        final String senderId = event.senderId();
        final Instant timestamp = event.timestamp();

        if (event.baseEventType() != STANDBY) {

            LOG.info(
                    "Received message '{}' with text '{}' from user '{}' at '{}'",
                    messageId,
                    messageText,
                    senderId,
                    timestamp);

            try {
                switch (messageText.toLowerCase()) {
                    case "text": // not working because of some permission
                        SendHelper.sendTextMessage(messenger, senderId, "sample ", true, "");
                        break;

                    case "generic":
                        SendHelper.sendGenericMessage(messenger, senderId);
                        break;
                    case "receipt":
                        SendHelper.sendReceiptMessage(messenger, senderId);
                        break;

                    case "quick reply":
                        SendHelper.sendQuickReply(messenger, senderId);
                        break;

                    case "read receipt":
                        SendHelper.sendReadReceipt(messenger, senderId);
                        break;

                    case "typing on":
                        SendHelper.sendTypingOn(messenger, senderId);
                        break;

                    case "typing off":
                        SendHelper.sendTypingOff(messenger, senderId);
                        break;

                    case "account linking": //
                        SendHelper.sendAccountLinking(messenger, senderId);
                        break;
                    case "handover": //
                        HandoverHelperFacebook.handoverToSecondaryReceiver(messenger, senderId, "Message from user: handover content");
                        break;
                    case "takeback": //
                        HandoverHelperFacebook.takeFromSecondaryReceiver(messenger, senderId);
                        break;
                    case "getowner": //
                        HandoverHelperFacebook.getThreadOwner(messenger, senderId);
                        break;
                    default:
                        SendHelper.sendTextMessage(messenger, senderId, messageText, true, "");
                }
            } catch (MessengerApiException | MessengerIOException | MalformedURLException e) {
                handleSendException(e);
            }
        } else {
            if (Pattern.compile("stores.*store.*product").matcher(messageText.toLowerCase()).find()) {
            }
            // message received in standby, do 
            switch (messageText.toLowerCase()) {
                case "takeback": //
                    HandoverHelperFacebook.takeFromSecondaryReceiver(messenger, senderId);
                    break;
                case "getowner": //
                    HandoverHelperFacebook.getThreadOwner(messenger, senderId);
                    break;
                default:
                    LOG.debug("{} message is {} in STANDBY mode, ignore", senderId, messageText);
            }
        }
    }

    private void handleQuickReplyMessageEvent(QuickReplyMessageEvent event) {
        LOG.debug("Handling QuickReplyMessageEvent");
        final String payload = event.payload();
        LOG.debug("payload: {}", payload);
        final String senderId = event.senderId();
        LOG.debug("senderId: {}", senderId);
        final String messageId = event.messageId();
        LOG.debug("messageId: {}", messageId);
        LOG.info("Received quick reply for message '{}' with payload '{}'", messageId, payload);
        SendHelper.sendTextMessage(messenger, senderId, "Quick reply tapped", true, "");
    }

    private void handleFallbackEvent(Event event) {
        LOG.debug("Handling FallbackEvent :{} ", event);
        final String senderId = event.senderId();
        LOG.debug("senderId: {}", senderId);

        LOG.info("Received unsupported message from user '{}'", senderId);
    }

    private void handleSendException(Exception e) {
        LOG.error("Message could not be sent. An unexpected error occurred.", e);
    }

    private void handleReferrelEvent(Event event) {
        LOG.info("handleReferrelEvent. {}", event);
    }
}
