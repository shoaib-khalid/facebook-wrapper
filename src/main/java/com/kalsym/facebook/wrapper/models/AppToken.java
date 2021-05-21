package com.kalsym.facebook.wrapper.models;

import java.io.Serializable;
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
@Table(name = "app_token")
@Getter
@Setter

public class AppToken implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "appId")
    private String appId;

    @Column(name = "userId")
    private String userId;
    
    @Column(name = "token")
    private String token;

    @Column(name = "created")
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date created;

}
