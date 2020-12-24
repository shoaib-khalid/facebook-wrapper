package com.kalsym.facebook.wrapper.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author z33Sh
 */
@Getter
@Setter
@ToString
//@AllArgsConstructor
public class RequestPayload {

    private String data;
    private String referral;
    private String msgId;
    private Boolean isGuest;
    private String callbackUrl;

    public RequestPayload(String data, String referral, String msgId, Boolean isGuest, String callbackUrl) {
        this.data = data;
        this.referral = referral;
        this.msgId = msgId;
        this.isGuest = isGuest;
        this.callbackUrl = callbackUrl;
    }

}
