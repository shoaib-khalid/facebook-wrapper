package com.kalsym.facebook.wrapper.repository;

import com.kalsym.facebook.wrapper.models.FbWrapperSession;
import java.sql.Timestamp;
import java.util.Date;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 *
 * @author z33Sh
 */
public interface FbWrapperSessionRepository extends JpaRepository<FbWrapperSession, String>, JpaSpecificationExecutor<FbWrapperSession> {

    List<FbWrapperSession> findAllByLastMessageToAgentLessThan(Timestamp ts);

}
