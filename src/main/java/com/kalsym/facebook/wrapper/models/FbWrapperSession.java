package com.kalsym.facebook.wrapper.models;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author z33Sh
 */
@Entity
@Table(name = "fb_wrapper_session")
@Getter
@Setter
public class FbWrapperSession implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "senderId")
    private String senderId;

    @Column(name = "lastMessageToAgent")
//    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Timestamp lastMessageToAgent;

    public FbWrapperSession() {
    }

        public FbWrapperSession(String senderId, Timestamp lastMessageToAgen) {
        this.senderId = senderId;
        this.lastMessageToAgent = lastMessageToAgen;
    }
}
