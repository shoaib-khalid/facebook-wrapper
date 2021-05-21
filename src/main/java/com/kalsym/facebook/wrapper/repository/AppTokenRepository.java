package com.kalsym.facebook.wrapper.repository;

import com.kalsym.facebook.wrapper.models.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;


/**
 *
 * @author z33Sh
 */
public interface AppTokenRepository extends JpaRepository<AppToken, String> {

    @Procedure(name = "getAppToken")
    String getAppToken(String appId);
}
