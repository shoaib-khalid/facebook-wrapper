package com.kalsym.facebook.wrapper.callback;

import static com.github.messenger4j.Messenger.CHALLENGE_REQUEST_PARAM_NAME;
import static com.github.messenger4j.Messenger.MODE_REQUEST_PARAM_NAME;
import static com.github.messenger4j.Messenger.SIGNATURE_HEADER_NAME;
import static com.github.messenger4j.Messenger.VERIFY_TOKEN_REQUEST_PARAM_NAME;
import static com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type.AUDIO;
import static com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type.FILE;
import static com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type.IMAGE;
import static com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type.VIDEO;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import com.github.messenger4j.Messenger;
import com.github.messenger4j.common.WebviewHeightRatio;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.exception.MessengerVerificationException;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.MessageResponse;
import com.github.messenger4j.send.MessagingType;
import com.github.messenger4j.send.NotificationType;
import com.github.messenger4j.send.SenderActionPayload;
import com.github.messenger4j.send.message.RichMediaMessage;
import com.github.messenger4j.send.message.TemplateMessage;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.message.quickreply.QuickReply;
import com.github.messenger4j.send.message.quickreply.TextQuickReply;
import com.github.messenger4j.send.message.richmedia.UrlRichMediaAsset;
import com.github.messenger4j.send.message.template.ButtonTemplate;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.ReceiptTemplate;
import com.github.messenger4j.send.message.template.button.Button;
import com.github.messenger4j.send.message.template.button.CallButton;
import com.github.messenger4j.send.message.template.button.LogInButton;
import com.github.messenger4j.send.message.template.button.LogOutButton;
import com.github.messenger4j.send.message.template.button.PostbackButton;
import com.github.messenger4j.send.message.template.button.UrlButton;
import com.github.messenger4j.send.message.template.common.Element;
import com.github.messenger4j.send.message.template.receipt.Address;
import com.github.messenger4j.send.message.template.receipt.Adjustment;
import com.github.messenger4j.send.message.template.receipt.Item;
import com.github.messenger4j.send.message.template.receipt.Summary;
import com.github.messenger4j.send.recipient.IdRecipient;
import com.github.messenger4j.send.recipient.Recipient;
import com.github.messenger4j.send.recipient.UserRefRecipient;
import com.github.messenger4j.send.senderaction.SenderAction;
import com.github.messenger4j.userprofile.UserProfile;
import com.github.messenger4j.webhook.Event;
import com.github.messenger4j.webhook.event.AccountLinkingEvent;
import com.github.messenger4j.webhook.event.AttachmentMessageEvent;
import com.github.messenger4j.webhook.event.MessageDeliveredEvent;
import com.github.messenger4j.webhook.event.MessageEchoEvent;
import com.github.messenger4j.webhook.event.MessageReadEvent;
import com.github.messenger4j.webhook.event.OptInEvent;
import com.github.messenger4j.webhook.event.PostbackEvent;
import com.github.messenger4j.webhook.event.QuickReplyMessageEvent;
import com.github.messenger4j.webhook.event.TextMessageEvent;
import com.github.messenger4j.webhook.event.attachment.Attachment;
import com.github.messenger4j.webhook.event.attachment.LocationAttachment;
import com.github.messenger4j.webhook.event.attachment.RichMediaAttachment;
import com.kalsym.facebook.wrapper.enums.ButtonType;
import com.kalsym.facebook.wrapper.models.MenuItem;
import com.kalsym.facebook.wrapper.models.RequestPayload;
import com.kalsym.facebook.wrapper.utils.Utilities;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
public class CallbackHandler {

    private static final String RESOURCE_URL
            = "https://raw.githubusercontent.com/fbsamples/messenger-platform-samples/master/node/public";

    private static final Logger LOG = LoggerFactory.getLogger("application");
    private final Messenger messenger;
    @Autowired
    private Environment env;

    @Autowired
    public CallbackHandler(final Messenger messenger) {
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
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(MODE_REQUEST_PARAM_NAME) final String mode,
            @RequestParam(VERIFY_TOKEN_REQUEST_PARAM_NAME) final String verifyToken,
            @RequestParam(CHALLENGE_REQUEST_PARAM_NAME) final String challenge) {
        LOG.debug(
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
    public ResponseEntity<Void> handleCallback(
            @RequestBody final String payload,
            @RequestHeader(SIGNATURE_HEADER_NAME) final String signature) {
        LOG.debug(
                "Received Messenger Platform callback - payload: {} | signature: {}", payload, signature);
        try {
            //            LOG.debug("Received Full Payload:" + payload);
            this.messenger.onReceiveEvents(
                    payload,
                    of(signature),
                    event -> {
                        if (event.isTextMessageEvent()) {
                            handleTextMessageEvent(event.asTextMessageEvent());
                            //                    handleTextMessageEventTest(event.asTextMessageEvent());
                        } else if (event.isAttachmentMessageEvent()) {
                            handleAttachmentMessageEvent(event.asAttachmentMessageEvent());
                        } else if (event.isQuickReplyMessageEvent()) {
                            handleQuickReplyMessageEvent(event.asQuickReplyMessageEvent());
                        } else if (event.isPostbackEvent()) {
                            handlePostbackEvent(event.asPostbackEvent());
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
    @RequestMapping(
            value = "/pushSimpleMessage",
            method = RequestMethod.POST,
            consumes = "Application/json")
    public ResponseEntity<Void> pushSimpleMessage(
            @RequestBody com.kalsym.facebook.wrapper.models.SimpleMessage requestData) {
        try {
            LOG.debug("[{}] received simple message request [{}] ", requestData.getRefId(), requestData.toString());
            //            System.out.println("params:"+params);
            final List<String> recipientIds = requestData.getRecipientIds();
            final String refId = requestData.getRefId();
            final String message = requestData.getMessage();
            LOG.info(
                    "[{}] received simple message request for recipients [{}]  with message [{}] ",
                    refId,
                    recipientIds.toString(),
                    message);
            recipientIds.forEach(
                    recip -> {
                        String recipient = (String) recip;
                        MessageResponse resp = sendTextMessage(recipient, message, requestData.isGuest());
                        LOG.debug("[{}] pushed message to [{}] with response [{}]", refId, recipient, resp);
                    });

        } catch (Exception ex) {
            LOG.warn("Processing of push simple message failed: {}", ex.getMessage());
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
    @RequestMapping(
            value = "/pushMenuMessage",
            method = RequestMethod.POST,
            consumes = "Application/json")
    public ResponseEntity<Void> pushMenuMessage(
            @RequestBody com.kalsym.facebook.wrapper.models.PushMessage requestData) {
        try {
            final List<String> recipientIds = requestData.getRecipientIds();
            final String refId = requestData.getRefId();
            final String title = requestData.getTitle();
            final String subTitle = requestData.getSubTitle();
            final String url = requestData.getUrl();
            final String UrlType = requestData.getUrlType();

            LOG.info(
                    "[{}] received menu message request for recipients [{}]  with title [{}] subtitle [{}] url [{}] urlType [{}]",
                    refId,
                    recipientIds.toString(),
                    title,
                    subTitle,
                    url,
                    UrlType);
            List<MenuItem> menuItems = requestData.getMenuItems();
            final List<Button> buttons = new ArrayList<>();
            menuItems.forEach(
                    menuButton -> {
                        MenuItem button = (MenuItem) menuButton;
                        final String buttonTitle = button.getTitle();
                        final String payload = button.getPayload();
                        if (button.getType().equalsIgnoreCase(ButtonType.url.toString())) {
                            try {
                                buttons.add(
                                        UrlButton.create(
                                                buttonTitle,
                                                new URL(payload),
                                                of(WebviewHeightRatio.COMPACT),
                                                of(false),
                                                empty(),
                                                empty()));
                            } catch (Exception ex) {
                                LOG.warn("[{}] malformed URL in payload [{}]", refId, payload);
                            }
                        } else if (button.getType().equalsIgnoreCase(ButtonType.number.toString())) {
                            buttons.add(CallButton.create(buttonTitle, payload));
                        } else {
                            buttons.add(PostbackButton.create(buttonTitle, payload));
                        }
                    });

            recipientIds.forEach(
                    recip -> {
//                        String recipient = (String) recip;
                        Recipient recipient;
                        if (requestData.isGuest()) { // if user is guest, use id in recipient
                            recipient = IdRecipient.create(recip);
                        } else { // if user is not guest, use user_ref in recipient
                            recipient = UserRefRecipient.create(recip);
                        }
                        MessageResponse resp = null;
                        try {
//                            resp = sendButtonMessage(recipient, title, buttons);
                            resp = sendMenuMessage(recipient, title, subTitle, url, buttons);
                        } catch (MessengerApiException | MessengerIOException | MalformedURLException ex) {
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

    private MessageResponse sendMenuMessage(
            Recipient recipientId, String title, String subTitle, String url, List<Button> buttons)
            throws MessengerApiException, MessengerIOException, MalformedURLException {

        final List<Element> elements = new ArrayList<>();
        if (null == url) {
            url = "http://kalsym.com";
        }
        elements.add(Element.create(title, of(subTitle), of(new URL(url)), empty(), of(buttons)));
        //        elements.add(Element.create("touch", of("Your Hands, Now in VR"), of(new
        // URL("https://www.oculus.com/en-us/touch/")), empty(), of(touchButtons)));

        final GenericTemplate genericTemplate = GenericTemplate.create(elements);
        final TemplateMessage templateMessage = TemplateMessage.create(genericTemplate);
        final MessagePayload messagePayload
                = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        return this.messenger.send(messagePayload);
    }

    private void sendGenericMessage(String recipientId)
            throws MessengerApiException, MessengerIOException, MalformedURLException {
        List<Button> riftButtons = new ArrayList<>();
        riftButtons.add(
                UrlButton.create("Open Web URL", new URL("https://www.oculus.com/en-us/rift/")));
        riftButtons.add(
                UrlButton.create("Open Web URL", new URL("https://www.oculus.com/en-us/rift/")));
        riftButtons.add(PostbackButton.create("Call Postback", "Payload for first bubble"));

        List<Button> touchButtons = new ArrayList<>();
        touchButtons.add(
                UrlButton.create("Open Web URL", new URL("https://www.oculus.com/en-us/touch/")));
        touchButtons.add(PostbackButton.create("Call Postback", "Payload for second bubble"));

        final List<Element> elements = new ArrayList<>();

        elements.add(
                Element.create(
                        "rift",
                        of("Next-generation virtual reality"),
                        of(new URL("https://www.oculus.com/en-us/rift/")),
                        empty(),
                        of(riftButtons)));
        elements.add(
                Element.create(
                        "touch",
                        of("Your Hands, Now in VR"),
                        of(new URL("https://www.oculus.com/en-us/touch/")),
                        empty(),
                        of(touchButtons)));

        final GenericTemplate genericTemplate = GenericTemplate.create(elements);
        final TemplateMessage templateMessage = TemplateMessage.create(genericTemplate);
        final MessagePayload messagePayload
                = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        this.messenger.send(messagePayload);
    }

    private MessageResponse sendButtonMessage(Recipient recipientId, String title, List<Button> buttons)
            throws MessengerApiException, MessengerIOException, MalformedURLException {
        //        final List<Button> buttons = Arrays.asList(
        //                UrlButton.create("Open Web URL", new
        // URL("https://www.oculus.com/en-us/rift/"), of(WebviewHeightRatio.COMPACT), of(false),
        // empty(), empty()),
        //                PostbackButton.create("Trigger Postback", "DEVELOPER_DEFINED_PAYLOAD"),
        // CallButton.create("Call Phone Number", "+16505551234")
        //        );

        final ButtonTemplate buttonTemplate = ButtonTemplate.create(title, buttons);
        final TemplateMessage templateMessage = TemplateMessage.create(buttonTemplate);
        final MessagePayload messagePayload
                = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        return this.messenger.send(messagePayload);
    }

    private void handleTextMessageEventTest(TextMessageEvent event) {
        LOG.debug("Received TextMessageEvent: {}", event);

        final String messageId = event.messageId();
        final String messageText = event.text();
        final String senderId = event.senderId();
        final Instant timestamp = event.timestamp();

        LOG.info(
                "Received message '{}' with text '{}' from user '{}' at '{}'",
                messageId,
                messageText,
                senderId,
                timestamp);

        try {
            switch (messageText.toLowerCase()) {
                case "user": // not working because of some permission
                    sendUserDetails(senderId);
                    break;

                case "image":
                    sendImageMessage(senderId);
                    break;

                case "gif":
                    sendGifMessage(senderId);
                    break;

                case "audio":
                    sendAudioMessage(senderId);
                    break;

                case "video":
                    sendVideoMessage(senderId);
                    break;

                case "file":
                    sendFileMessage(senderId);
                    break;

                //                case "button":
                //                    sendButtonMessage(senderId);
                //                    break;
                case "generic":
                    sendGenericMessage(senderId);
                    break;

                //                case "list": // does not work, to be depreciated soon
                //                    sendListMessageMessage(senderId);
                //                    break;
                case "receipt":
                    sendReceiptMessage(senderId);
                    break;

                case "quick reply":
                    sendQuickReply(senderId);
                    break;

                case "read receipt":
                    sendReadReceipt(senderId);
                    break;

                case "typing on":
                    sendTypingOn(senderId);
                    break;

                case "typing off":
                    sendTypingOff(senderId);
                    break;

                case "account linking": //
                    sendAccountLinking(senderId);
                    break;
                default:
                    sendTextMessage(senderId, messageText, true);
            }
        } catch (MessengerApiException | MessengerIOException | MalformedURLException e) {
            handleSendException(e);
        }
    }

    private void handleTextMessageEvent(TextMessageEvent event) {
        try {
            final String backEndMessageEndpoint
                    = env.getProperty("backend.message.endpoint", "callback/message");
            final String backEndPostbackEndpoint
                    = env.getProperty("backend.postback.endpoint", "callback/postback");

            LOG.debug("Received TextMessageEvent: {}", event);
            final String messageId = event.messageId();
            String messageText = event.text();
            messageText = messageText.replaceAll("\"", "\"\"");
            final String senderId = event.senderId();
            final String recipientId = event.recipientId();
            final Instant timestamp = event.timestamp();
            LOG.info(
                    "Received message '{}' with text '{}' from user '{}'  to user {} at '{}'",
                    messageId,
                    messageText,
                    senderId,
                    recipientId,
                    timestamp);
            String isGuest = "true";

            LOG.debug("isGuest: {}", isGuest);

            final String queryParams
                    = "senderId=" + senderId + "&refrenceId=" + env.getProperty("backend.refrenced.id", "");
            // forward to backend for
//            JSONObject json = new JSONObject();
//            json.put("data", messageText);
//            json.put("refId", messageId);
//            json.put("isGuest", Boolean.parseBoolean(isGuest));
//            json.put("msgId", messageId);
//            json.put(
//                    "callbackUrl",
//                    "http://"
//                    + env.getProperty("server.address", "127.0.0.1")
//                    + ":"
//                    + env.getProperty("server.port", "8080")
//                    + "/");
//            System.out.println("json:" + json);
//            Utilities.sendPostRequest(backEndMessageEndpoint, queryParams, json.toString());
            RequestPayload data = new RequestPayload(messageText, "", timestamp.toString(), Boolean.parseBoolean(isGuest),
                    "http://"
                    + env.getProperty("server.address", "127.0.0.1")
                    + ":"
                    + env.getProperty("server.port", "8080")
                    + "/");
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity("http://"
                    + env.getProperty("backend.ip", "127.0.0.1")
                    + ":"
                    + env.getProperty("backend.port", "8080")
                    + backEndMessageEndpoint + "/"
                    + "?" + queryParams, data, String.class);
        } catch (Exception e) {
            handleSendException(e);
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
        sendTextMessage(senderId, "Quick reply tapped", true);
    }

    private void handlePostbackEvent(PostbackEvent event) {
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

        LOG.info(
                "Received postback for user '{}' and page '{}' with payload '{}' at '{}'",
                senderId,
                senderId,
                payload,
                timestamp);
//        if ("false".equalsIgnoreCase(isGuest)) {
//            sendTextMessageToUserRef(senderId, payload);
//        }

        final String queryParams
                = "senderId=" + senderId + "&refrenceId=" + env.getProperty("backend.refrenced.id", "");
        // forward to backend for
        RequestPayload data = new RequestPayload(payload, "", timestamp.toString(), Boolean.parseBoolean(isGuest),
                "http://"
                + env.getProperty("server.address", "127.0.0.1")
                + ":"
                + env.getProperty("server.port", "8080")
                + "/");

//        JSONObject json = new JSONObject();
//        json.put("data", payload);
//        json.put("refId", "");
//        json.put("isGuest", Boolean.parseBoolean(isGuest));
//        json.put("msgId", timestamp);
//        json.put(
//                "callbackUrl",
//                "http://"
//                + env.getProperty("server.address", "127.0.0.1")
//                + ":"
//                + env.getProperty("server.port", "8080")
//                + "/");
//        System.out.println("json:" + json);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<String> entity = new HttpEntity<>(json.toString(), headers);
        RestTemplate restTemplate = new RestTemplate();
//        String response
//                = restTemplate.postForObject(
//                        "http://"
//                        + env.getProperty("backend.ip", "127.0.0.1")
//                        + ":"
//                        + env.getProperty("backend.port", "8080")
//                        + "/callback/postaback?"
//                        + queryParams,
//                        entity,
//                        String.class);
        ResponseEntity<String> response = restTemplate.postForEntity("http://"
                + env.getProperty("backend.ip", "127.0.0.1")
                + ":"
                + env.getProperty("backend.port", "8080")
                + "/callback/postaback?"
                + queryParams, data, String.class);
        LOG.info("got response : " + response);
    }

    private void sendUserDetails(String recipientId)
            throws MessengerApiException, MessengerIOException {
        final UserProfile userProfile = this.messenger.queryUserProfile(recipientId);
        sendTextMessage(
                recipientId,
                String.format(
                        "Your name is %s and you are %s", userProfile.firstName(), userProfile.gender()), true);
        LOG.info("User Profile Picture: {}", userProfile.profilePicture());
    }

    private void sendImageMessage(String recipientId)
            throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset
                = UrlRichMediaAsset.create(IMAGE, new URL(RESOURCE_URL + "/assets/rift.png"));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendRichMediaMessage(String recipientId, UrlRichMediaAsset richMediaAsset)
            throws MessengerApiException, MessengerIOException {
        final RichMediaMessage richMediaMessage = RichMediaMessage.create(richMediaAsset);
        final MessagePayload messagePayload
                = MessagePayload.create(recipientId, MessagingType.RESPONSE, richMediaMessage);
        this.messenger.send(messagePayload);
    }

    private void sendGifMessage(String recipientId)
            throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset
                = UrlRichMediaAsset.create(
                        IMAGE, new URL("https://media.giphy.com/media/11sBLVxNs7v6WA/giphy.gif"));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendAudioMessage(String recipientId)
            throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset
                = UrlRichMediaAsset.create(AUDIO, new URL(RESOURCE_URL + "/assets/sample.mp3"));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendVideoMessage(String recipientId)
            throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset
                = UrlRichMediaAsset.create(VIDEO, new URL(RESOURCE_URL + "/assets/allofus480.mov"));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendFileMessage(String recipientId)
            throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset
                = UrlRichMediaAsset.create(FILE, new URL(RESOURCE_URL + "/assets/test.txt"));
        sendRichMediaMessage(recipientId, richMediaAsset);
    }

    private void sendReceiptMessage(String recipientId)
            throws MessengerApiException, MessengerIOException, MalformedURLException {
        final String uniqueReceiptId = "order-" + Math.floor(Math.random() * 1000);

        final List<Item> items = new ArrayList<>();

        items.add(
                Item.create(
                        "Oculus Rift",
                        599.00f,
                        of("Includes: headset, sensor, remote"),
                        of(1),
                        of("USD"),
                        of(new URL(RESOURCE_URL + "/assets/riftsq.png"))));
        items.add(
                Item.create(
                        "Samsung Gear VR",
                        99.99f,
                        of("Frost White"),
                        of(1),
                        of("USD"),
                        of(new URL(RESOURCE_URL + "/assets/gearvrsq.png"))));

        final ReceiptTemplate receiptTemplate
                = ReceiptTemplate.create(
                        "Peter Chang",
                        uniqueReceiptId,
                        "Visa 1234",
                        "USD",
                        Summary.create(626.66f, of(698.99f), of(57.67f), of(20.00f)),
                        of(Address.create("1 Hacker Way", "Menlo Park", "94025", "CA", "US")),
                        of(items),
                        of(
                                Arrays.asList(
                                        Adjustment.create("New Customer Discount", -50f),
                                        Adjustment.create("$100 Off Coupon", -100f))),
                        of("The Boring Company"),
                        of(new URL("https://www.boringcompany.com/")),
                        of(true),
                        of(Instant.ofEpochMilli(1428444852L)));

        final TemplateMessage templateMessage = TemplateMessage.create(receiptTemplate);
        final MessagePayload messagePayload
                = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        this.messenger.send(messagePayload);
    }

    private void sendQuickReply(String recipientId)
            throws MessengerApiException, MessengerIOException {
        List<QuickReply> quickReplies = new ArrayList<>();

        quickReplies.add(
                TextQuickReply.create("Action", "DEVELOPER_DEFINED_PAYLOAD_FOR_PICKING_ACTION"));
        quickReplies.add(
                TextQuickReply.create("Comedy", "DEVELOPER_DEFINED_PAYLOAD_FOR_PICKING_COMEDY"));
        quickReplies.add(TextQuickReply.create("Drama", "DEVELOPER_DEFINED_PAYLOAD_FOR_PICKING_DRAMA"));
        //        quickReplies.add(LocationQuickReply.create());

        TextMessage message
                = TextMessage.create("What's your favorite movie genre?", of(quickReplies), empty());
        messenger.send(MessagePayload.create(recipientId, MessagingType.RESPONSE, message));
    }

    private void sendReadReceipt(String recipientId)
            throws MessengerApiException, MessengerIOException {
        this.messenger.send(SenderActionPayload.create(recipientId, SenderAction.MARK_SEEN));
    }

    private void sendTypingOn(String recipientId) throws MessengerApiException, MessengerIOException {
        this.messenger.send(SenderActionPayload.create(recipientId, SenderAction.TYPING_ON));
    }

    private void sendTypingOff(String recipientId)
            throws MessengerApiException, MessengerIOException {
        this.messenger.send(SenderActionPayload.create(recipientId, SenderAction.TYPING_OFF));
    }

    private void sendAccountLinking(String recipientId)
            throws MessengerApiException, MessengerIOException, MalformedURLException {
        // Mandatory https
        final LogInButton buttonIn
                = LogInButton.create(new URL("https://bfb700b9e513.ngrok.io/callback"));
        final LogOutButton buttonOut = LogOutButton.create();

        final List<Button> buttons = Arrays.asList(buttonIn, buttonOut);
        final ButtonTemplate buttonTemplate
                = ButtonTemplate.create("Log in to see an account linking callback", buttons);

        final TemplateMessage templateMessage = TemplateMessage.create(buttonTemplate);
        final MessagePayload messagePayload
                = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        this.messenger.send(messagePayload);
    }

    private void handleAttachmentMessageEvent(AttachmentMessageEvent event) {
        LOG.debug("Handling QuickReplyMessageEvent");
        final String senderId = event.senderId();
        LOG.debug("senderId: {}", senderId);
        for (Attachment attachment : event.attachments()) {
            if (attachment.isRichMediaAttachment()) {
                final RichMediaAttachment richMediaAttachment = attachment.asRichMediaAttachment();
                final RichMediaAttachment.Type type = richMediaAttachment.type();
                final URL url = richMediaAttachment.url();
                LOG.debug("Received rich media attachment of type '{}' with url: {}", type, url);
                final String text = String.format("Media %s received (url: %s)", type.name(), url);
                sendTextMessage(senderId, text, true);
            } else if (attachment.isLocationAttachment()) {
                final LocationAttachment locationAttachment = attachment.asLocationAttachment();
                final double longitude = locationAttachment.longitude();
                final double latitude = locationAttachment.latitude();
                LOG.debug("Received location information (long: {}, lat: {})", longitude, latitude);
                final String text
                        = String.format(
                                "Location received (long: %s, lat: %s)",
                                String.valueOf(longitude), String.valueOf(latitude));
                sendTextMessage(senderId, text, true);
            }
        }
    }

    private void handleAccountLinkingEvent(AccountLinkingEvent event) {
        LOG.debug("Handling AccountLinkingEvent");
        final String senderId = event.senderId();
        LOG.debug("senderId: {}", senderId);
        final AccountLinkingEvent.Status accountLinkingStatus = event.status();
        LOG.debug("accountLinkingStatus: {}", accountLinkingStatus);
        final String authorizationCode
                = event
                        .authorizationCode()
                        .orElse("Empty authorization code!!!"); // You can throw an Exception
        LOG.debug("authorizationCode: {}", authorizationCode);
        LOG.info(
                "Received account linking event for user '{}' with status '{}' and auth code '{}'",
                senderId,
                accountLinkingStatus,
                authorizationCode);
        sendTextMessage(senderId, "AccountLinking event tapped", true);
    }

    private void handleOptInEvent(OptInEvent event) {
        LOG.debug("Handling OptInEvent");
        final String senderId = event.senderId();
        LOG.debug("senderId: {}", senderId);
        final String recipientId = event.recipientId();
        LOG.debug("recipientId: {}", recipientId);
        final String passThroughParam = event.refPayload().orElse("empty payload");
        LOG.debug("passThroughParam: {}", passThroughParam);
        final Instant timestamp = event.timestamp();
        LOG.debug("timestamp: {}", timestamp);

        LOG.info(
                "Received authentication for user '{}' and page '{}' with pass through param '{}' at '{}'",
                senderId,
                recipientId,
                passThroughParam,
                timestamp);
        sendTextMessage(senderId, "Authentication successful", true);
    }

    private void handleMessageEchoEvent(MessageEchoEvent event) {
        LOG.debug("Handling MessageEchoEvent");
        final String senderId = event.senderId();
        LOG.debug("senderId: {}", senderId);
        final String recipientId = event.recipientId();
        LOG.debug("recipientId: {}", recipientId);
        final String messageId = event.messageId();
        LOG.debug("messageId: {}", messageId);
        final Instant timestamp = event.timestamp();
        LOG.debug("timestamp: {}", timestamp);

        LOG.info(
                "Received echo for message '{}' that has been sent to recipient '{}' by sender '{}' at '{}'",
                messageId,
                recipientId,
                senderId,
                timestamp);
        sendTextMessage(senderId, "MessageEchoEvent tapped", true);
    }

    private void handleMessageDeliveredEvent(MessageDeliveredEvent event) {
        LOG.debug("Handling MessageDeliveredEvent");
        final String senderId = event.senderId();
        LOG.debug("senderId: {}", senderId);
        final List<String> messageIds = event.messageIds().orElse(Collections.emptyList());
        final Instant watermark = event.watermark();
        LOG.debug("watermark: {}", watermark);

        messageIds.forEach(
                messageId -> {
                    LOG.info("Received delivery confirmation for message '{}'", messageId);
                });

        LOG.info("All messages before '{}' were delivered to user '{}'", watermark, senderId);
    }

    private void handleMessageReadEvent(MessageReadEvent event) {
        LOG.debug("Handling MessageReadEvent");
        final String senderId = event.senderId();
        LOG.debug("senderId: {}", senderId);
        final Instant watermark = event.watermark();
        LOG.debug("watermark: {}", watermark);

        LOG.info("All messages before '{}' were read by user '{}'", watermark, senderId);
    }

    private void handleFallbackEvent(Event event) {
        LOG.debug("Handling FallbackEvent");
        final String senderId = event.senderId();
        LOG.debug("senderId: {}", senderId);

        LOG.info("Received unsupported message from user '{}'", senderId);
    }

    private MessageResponse sendTextMessage(String recipientId, String text, boolean isGuest) {

        try {
            Recipient recipient;
            if (isGuest) { // if user is guest, use id in recipient
                recipient = IdRecipient.create(recipientId);
            } else { // if user is not guest, use user_ref in recipient
                recipient = UserRefRecipient.create(recipientId);
            }

            final NotificationType notificationType = NotificationType.REGULAR;
            final String metadata = "ZEE";

            final TextMessage textMessage = TextMessage.create(text, empty(), of(metadata));
            final MessagePayload messagePayload
                    = MessagePayload.create(
                            recipient, MessagingType.RESPONSE, textMessage, of(notificationType), empty());
            return this.messenger.send(messagePayload);
        } catch (MessengerApiException | MessengerIOException e) {
            handleSendException(e);
            return null;
        }
    }

    private MessageResponse sendTextMessageToUserRef(String userRefId, String text) {
        try {
            LOG.debug("Sending to user ref: {}", userRefId);

            final UserRefRecipient recipient = UserRefRecipient.create(userRefId);

            final NotificationType notificationType = NotificationType.REGULAR;
            final String metadata = "KALSYM";

            final TextMessage textMessage = TextMessage.create(text, empty(), of(metadata));
            final MessagePayload messagePayload
                    = MessagePayload.create(
                            recipient, MessagingType.RESPONSE, textMessage, of(notificationType), empty());
            return this.messenger.send(messagePayload);
        } catch (MessengerApiException | MessengerIOException e) {
            handleSendException(e);
            return null;
        }
    }

    private void handleSendException(Exception e) {
        LOG.error("Message could not be sent. An unexpected error occurred.", e);
    }

    private void handleReferrelEvent(Event event) {
        LOG.info("handleReferrelEvent. {}", event);
    }
}
