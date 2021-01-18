package com.kalsym.facebook.wrapper.callback.handlers;

import com.github.messenger4j.webhook.event.AttachmentMessageEvent;
import com.github.messenger4j.webhook.event.attachment.Attachment;
import com.github.messenger4j.webhook.event.attachment.LocationAttachment;
import com.github.messenger4j.webhook.event.attachment.RichMediaAttachment;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author z33Sh
 */
public class AttachmentMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger("application");

    /**
     *
     * @param event
     * @return
     */
    public static String handleAttachmentMessageEvent(AttachmentMessageEvent event) {
        String senderId = "";
        try {
            LOG.debug("Handling QuickReplyMessageEvent");
            senderId = event.senderId();
            LOG.debug("senderId: {}", senderId);
            for (Attachment attachment : event.attachments()) {
                if (attachment.isRichMediaAttachment()) {
                    final RichMediaAttachment richMediaAttachment = attachment.asRichMediaAttachment();
                    final RichMediaAttachment.Type type = richMediaAttachment.type();
                    final URL url = richMediaAttachment.url();
                    LOG.debug("Received rich media attachment of type '{}' with url: {}", type, url);
                    final String text = String.format("Media %s received (url: %s)", type.name(), url);
//                    sendTextMessage(senderId, text, true);
                } else if (attachment.isLocationAttachment()) {
                    final LocationAttachment locationAttachment = attachment.asLocationAttachment();
                    final double longitude = locationAttachment.longitude();
                    final double latitude = locationAttachment.latitude();
                    LOG.debug("Received location information (long: {}, lat: {})", longitude, latitude);
                    final String text = String.format("Location received (long: %s, lat: %s)", String.valueOf(longitude), String.valueOf(latitude));
//                    sendTextMessage(senderId, text, true);
                }
            }
            return "SUCCESS";
        } catch (Exception ex) {
            LOG.error("{} Error: {} ", senderId, ex);
            return "EXCEPTION";
        }
    }
}
