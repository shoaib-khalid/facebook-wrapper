package com.kalsym.facebook.wrapper.models;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Sarosh
 */
@Getter
@Setter
@ToString
public class PushMessage {

    private List<String> recipientIds;

    @NotNull
    @Size(min = 2, max = 80)
    private String title;

    @Size(min = 2, max = 80)
    private String subTitle;

    private String url;

    @Size(min = 2, max = 10)
    private String urlType;

    @Size(max = 3)
    private List<MenuItem> menuItems;
    private String message;
    @Size(min = 2, max = 1000)
    private String refId;

    @Size(min = 2, max = 1000)
    private String referenceId;

    @NotNull
    private boolean isGuest;

}
