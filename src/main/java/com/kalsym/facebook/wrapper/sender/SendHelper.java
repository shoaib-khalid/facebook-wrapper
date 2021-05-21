package com.kalsym.facebook.wrapper.sender;

import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
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
import static com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type.AUDIO;
import static com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type.FILE;
import static com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type.IMAGE;
import static com.github.messenger4j.send.message.richmedia.RichMediaAsset.Type.VIDEO;
import com.github.messenger4j.send.message.richmedia.UrlRichMediaAsset;
import com.github.messenger4j.send.message.template.ButtonTemplate;
import com.github.messenger4j.send.message.template.GenericTemplate;
import com.github.messenger4j.send.message.template.ReceiptTemplate;
import com.github.messenger4j.send.message.template.button.Button;
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
import com.github.messenger4j.spi.MessengerHttpClient;
import com.github.messenger4j.userprofile.UserProfile;
import com.kalsym.facebook.wrapper.config.ConfigReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author z33Sh
 */
public class SendHelper {

    private static final Logger LOG = LoggerFactory.getLogger("application");
    private static final String RESOURCE_URL = ConfigReader.environment.getProperty("resources_url", "https://raw.githubusercontent.com/fbsamples/messenger-platform-samples/master/node/public");

    /**
     *
     * @param messenger
     * @param recipientId
     * @return
     * @throws MessengerApiException
     * @throws MessengerIOException
     */
    public static MessageResponse sendReadReceipt(Messenger messenger, String recipientId)
            throws MessengerApiException, MessengerIOException {
        try {
            MessageResponse res = messenger.send(SenderActionPayload.create(recipientId, SenderAction.MARK_SEEN));
            return res;
        } catch (Exception e) {
            LOG.error("{} Message could not be sent. An unexpected error occurred. {}", recipientId, e);
            return null;
        }
    }

    /**
     *
     * @param messenger
     * @param recipientId
     * @return
     * @throws MessengerApiException
     * @throws MessengerIOException
     */
    public static MessageResponse sendQuickReply(Messenger messenger, String recipientId)
            throws MessengerApiException, MessengerIOException {
        try {
            List<QuickReply> quickReplies = new ArrayList<>();

            quickReplies.add(
                    TextQuickReply.create("Action", "DEVELOPER_DEFINED_PAYLOAD_FOR_PICKING_ACTION"));
            quickReplies.add(
                    TextQuickReply.create("Comedy", "DEVELOPER_DEFINED_PAYLOAD_FOR_PICKING_COMEDY"));
            quickReplies.add(TextQuickReply.create("Drama", "DEVELOPER_DEFINED_PAYLOAD_FOR_PICKING_DRAMA"));
            //        quickReplies.add(LocationQuickReply.create());

            TextMessage message
                    = TextMessage.create("What's your favorite movie genre?", of(quickReplies), empty());
            MessageResponse res = messenger.send(MessagePayload.create(recipientId, MessagingType.RESPONSE, message));
            return res;
        } catch (Exception e) {
            LOG.error("{} Message could not be sent. An unexpected error occurred. {}", recipientId, e);
            return null;
        }
    }

    /**
     * Sends @param text to recipient.
     *
     * @param messenger
     * @param recipientId
     * @param text
     * @param isGuest if isGuest is true,recipient will be created as
     * IdRecipient, else it will be created as UserRefRecipient before sending
     * message to messenger API
     * @param referenceToken
     * @return
     */
    public static MessengerHttpClient.HttpResponse sendTextMessage(Messenger messenger, String recipientId, String text, boolean isGuest, String referenceToken) {

        try {
            Recipient recipient;
            if (isGuest) { // if user is guest, use id in recipient
                recipient = IdRecipient.create(recipientId);
            } else { // if user is not guest, use user_ref in recipient
                recipient = UserRefRecipient.create(recipientId);
            }

            final NotificationType notificationType = NotificationType.REGULAR;
            final String metadata = "ZEE";

//            final TextMessage textMessage = TextMessage.create(text, empty(), of(metadata));
            final TextMessage textMessageWithGraph = TextMessage.create(text, empty(), of(metadata));

//            final MessagePayload messagePayload = MessagePayload.create(recipient, MessagingType.RESPONSE, textMessage, of(notificationType), empty());
            final MessagePayload messagePayloadWithGraph = MessagePayload.create(recipient, MessagingType.RESPONSE, textMessageWithGraph, of(notificationType), empty());
            GraphApiSender gSender = new GraphApiSender(referenceToken);
            MessengerHttpClient.HttpResponse response = gSender.sendMessage(messagePayloadWithGraph);
            return response;
//            return messenger.send(messagePayload);
        } catch (Exception e) {
            LOG.error("{} Message could not be sent. An unexpected error occurred. {}", recipientId, e);
            return new MessengerHttpClient.HttpResponse(-1, "Exception:" + e);
        }
    }

    /**
     *
     * @param messenger
     * @param recipientId
     * @return
     * @throws MessengerApiException
     * @throws MessengerIOException
     * @throws MalformedURLException
     */
    public static MessageResponse sendAccountLinking(Messenger messenger, String recipientId)
            throws MessengerApiException, MessengerIOException, MalformedURLException {

        try {
            final LogInButton buttonIn
                    = LogInButton.create(new URL("https://bfb700b9e513.ngrok.io/callback")); // Mandatory https
            final LogOutButton buttonOut = LogOutButton.create();

            final List<Button> buttons = Arrays.asList(buttonIn, buttonOut);
            final ButtonTemplate buttonTemplate
                    = ButtonTemplate.create("Log in to see an account linking callback", buttons);

            final TemplateMessage templateMessage = TemplateMessage.create(buttonTemplate);
            final MessagePayload messagePayload
                    = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
            return messenger.send(messagePayload);
        } catch (Exception e) {
            LOG.error("{} Message could not be sent. An unexpected error occurred. {}", recipientId, e);
            return null;
        }
    }

    public static MessengerHttpClient.HttpResponse sendGifMessage(Messenger messenger, String recipientId, String mediaUrl, String referenceToken)
            throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset
                = UrlRichMediaAsset.create(
                        IMAGE, new URL(mediaUrl));
        return sendRichMediaMessage(messenger, recipientId, richMediaAsset, referenceToken);
    }

    public static MessengerHttpClient.HttpResponse sendAudioMessage(Messenger messenger, String recipientId, String mediaUrl, String referenceToken)
            throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset
                = UrlRichMediaAsset.create(AUDIO, new URL(mediaUrl));
        return sendRichMediaMessage(messenger, recipientId, richMediaAsset, referenceToken);
    }

    public static MessengerHttpClient.HttpResponse sendVideoMessage(Messenger messenger, String recipientId, String mediaUrl, String referenceToken)
            throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset
                = UrlRichMediaAsset.create(VIDEO, new URL(mediaUrl));
        return sendRichMediaMessage(messenger, recipientId, richMediaAsset, referenceToken);
    }

    public static MessengerHttpClient.HttpResponse sendFileMessage(Messenger messenger, String recipientId, String mediaUrl, String referenceToken)
            throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset
                = UrlRichMediaAsset.create(FILE, new URL(mediaUrl));
        return sendRichMediaMessage(messenger, recipientId, richMediaAsset, referenceToken);
    }

//    public static MessageResponse sendUserDetails(Messenger messenger, String recipientId)
//            throws MessengerApiException, MessengerIOException {
//        final UserProfile userProfile = messenger.queryUserProfile(recipientId);
//        LOG.info("User Profile Picture: {}", userProfile.profilePicture());
//
//        return sendTextMessage(messenger, recipientId, String.format("Your name is %s and you are %s", userProfile.firstName(), userProfile.gender()), true);
//
//    }
    public static MessengerHttpClient.HttpResponse sendImageMessage(Messenger messenger, String recipientId, String mediaUrl, String referenceToken)
            throws MessengerApiException, MessengerIOException, MalformedURLException {
        final UrlRichMediaAsset richMediaAsset
                = UrlRichMediaAsset.create(IMAGE, new URL(mediaUrl));

        return sendRichMediaMessage(messenger, recipientId, richMediaAsset, referenceToken);
    }

    public static MessengerHttpClient.HttpResponse sendRichMediaMessage(Messenger messenger, String recipientId, UrlRichMediaAsset richMediaAsset, String referenceToken)
            throws MessengerApiException, MessengerIOException {
        final RichMediaMessage richMediaMessage = RichMediaMessage.create(richMediaAsset);
        final MessagePayload messagePayload
                = MessagePayload.create(recipientId, MessagingType.RESPONSE, richMediaMessage);
        GraphApiSender gSender = new GraphApiSender(referenceToken);
        MessengerHttpClient.HttpResponse response = null;
        try {
            response = gSender.sendMessage(messagePayload);
        } catch (IOException ex) {
            return new MessengerHttpClient.HttpResponse(-1, "Exception:" + ex);
        }
        return response;
//        return messenger.send(messagePayload);
    }

    public static MessageResponse sendTypingOn(Messenger messenger, String recipientId) throws MessengerApiException, MessengerIOException {
        return messenger.send(SenderActionPayload.create(recipientId, SenderAction.TYPING_ON));
    }

    public static MessageResponse sendTypingOff(Messenger messenger, String recipientId)
            throws MessengerApiException, MessengerIOException {
        return messenger.send(SenderActionPayload.create(recipientId, SenderAction.TYPING_OFF));
    }

    public static MessengerHttpClient.HttpResponse sendMenuMessage(Messenger messenger,
            Recipient recipientId, String title, String subTitle, String url, List<Button> buttons, String referenceToken)
            throws MessengerApiException, MessengerIOException, MalformedURLException {

        final List<Element> elements = new ArrayList<>();
        if (null == url) {
            url = "http://kalsym.com";
        }
        elements.add(Element.create(title, of(subTitle), of(new URL(url)), empty(), of(buttons)));

        final GenericTemplate genericTemplate = GenericTemplate.create(elements);
        final TemplateMessage templateMessage = TemplateMessage.create(genericTemplate);
        final MessagePayload messagePayload
                = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        GraphApiSender gSender = new GraphApiSender(referenceToken);
        MessengerHttpClient.HttpResponse response;
        try {
            response = gSender.sendMessage(messagePayload);
        } catch (IOException ex) {
            return new MessengerHttpClient.HttpResponse(-1, "Exception:" + ex);
        }
        return response;
//        return messenger.send(messagePayload);
    }

    public static MessageResponse sendGenericMessage(Messenger messenger, String recipientId)
            throws MessengerApiException, MessengerIOException, MalformedURLException {
        List<Button> riftButtons = new ArrayList<>();
        riftButtons.add(UrlButton.create("Open Web URL", new URL("https://www.oculus.com/en-us/rift/")));
        riftButtons.add(UrlButton.create("Open Web URL", new URL("https://www.oculus.com/en-us/rift/")));
        riftButtons.add(PostbackButton.create("Call Postback", "Payload for first bubble"));
        List<Button> touchButtons = new ArrayList<>();
        touchButtons.add(UrlButton.create("Open Web URL", new URL("https://www.oculus.com/en-us/touch/")));
        touchButtons.add(PostbackButton.create("Call Postback", "Payload for second bubble"));
        final List<Element> elements = new ArrayList<>();
        elements.add(Element.create("rift", of("Next-generation virtual reality"), of(new URL("https://www.oculus.com/en-us/rift/")), empty(), of(riftButtons)));
        elements.add(Element.create("touch", of("Your Hands, Now in VR"), of(new URL("https://www.oculus.com/en-us/touch/")), empty(), of(touchButtons)));
        final GenericTemplate genericTemplate = GenericTemplate.create(elements);
        final TemplateMessage templateMessage = TemplateMessage.create(genericTemplate);
        final MessagePayload messagePayload
                = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        return messenger.send(messagePayload);
    }

    public static MessageResponse sendButtonMessage(Messenger messenger, Recipient recipientId, String title, List<Button> buttons)
            throws MessengerApiException, MessengerIOException, MalformedURLException {

        final ButtonTemplate buttonTemplate = ButtonTemplate.create(title, buttons);
        final TemplateMessage templateMessage = TemplateMessage.create(buttonTemplate);
        final MessagePayload messagePayload
                = MessagePayload.create(recipientId, MessagingType.RESPONSE, templateMessage);
        return messenger.send(messagePayload);
    }

    public static MessageResponse sendReceiptMessage(Messenger messenger, String recipientId)
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
        return messenger.send(messagePayload);
    }

}
