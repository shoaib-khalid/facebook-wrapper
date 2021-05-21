package com.kalsym.facebook.wrapper.models;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author z33Sh
 */
@Getter
@Setter
@ToString
public class Conversation {

    @NotNull
    @Size(min = 2, max = 3)
    private List<String> recipientIds;

    @Size(min = 2, max = 1000)
    private String message;

    private String refId;

    @Size(min = 2, max = 1000)
    private String referenceId;
    
    @NotNull
    private boolean isGuest;

}
