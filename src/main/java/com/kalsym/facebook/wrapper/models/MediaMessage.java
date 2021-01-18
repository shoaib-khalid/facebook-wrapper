package com.kalsym.facebook.wrapper.models;

import com.kalsym.facebook.wrapper.enums.MediaType;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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
public class MediaMessage {

    @NotNull
    @Size(min = 2, max = 3)
    private List<String> recipientIds;

    @NotNull
    @Size(min = 2, max = 1000)
    private String url;

    @NotNull
    private MediaType type;

    private String refId;
    @NotNull
    private boolean isGuest;
}
