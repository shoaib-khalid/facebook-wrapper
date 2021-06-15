package com.kalsym.facebook.wrapper.schedular;

import static com.kalsym.facebook.wrapper.Application.agent_sessions;
import com.kalsym.facebook.wrapper.repository.FbWrapperSessionRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import com.kalsym.facebook.wrapper.models.FbWrapperSession;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author z33Sh
 */
@Configuration
public class SessionSchedular {

    private final static Logger LOG = LoggerFactory.getLogger("application");
    @Autowired
    private FbWrapperSessionRepository fbWrapperSessionRepository;
    private static boolean isRunning = false;

    @Value("${session.expire.qualify.minutes:10}")
    int sessionExpireQualifyMinutes;

    /**
     * 1- Starts on specific time and gets expired sessions from database 
     *
     */
    @Scheduled(cron = "${clear.expired.sessions.job:0 */10 * * * *}")
    public void clearExpiredSessions() {
        LOG.info("[SessionSchedular] Started clearing expired sessions");
        if (!isRunning && null != fbWrapperSessionRepository) {
            isRunning = true;
            try {
                TimeZone TZ;
                //default time zone
                Calendar cal = Calendar.getInstance();

                cal.add(Calendar.MINUTE, -sessionExpireQualifyMinutes);
                Timestamp ts= new Timestamp(cal.getTimeInMillis());
                List<FbWrapperSession> sessions = fbWrapperSessionRepository.findAllByLastMessageToAgentLessThan(ts);
                LOG.info("[SessionSchedular] Sessions to handle {} for date {}", sessions.size(), cal.getTime());
                for (FbWrapperSession session : sessions) {
                    LOG.info("[SessionSchedular] Processing started for {} and date {}", session.getSenderId(), session.getLastMessageToAgent());
                    // Delete from agent sessions
                    agent_sessions.remove(session.getSenderId());
                    // remove from database
                    fbWrapperSessionRepository.deleteById(session.getSenderId());
                    LOG.info("[SessionSchedular] Processing finished for {}", session.getSenderId());
                }
            } catch (Exception ex) {
                LOG.error("[SessionSchedular] Exception clearing expired sessions", ex);
            }
            isRunning = false;
        } else {
            LOG.info("[SessionSchedular] Already running");
        }
        LOG.info("[SessionSchedular] Finished clearing expired sessions");

    }

}
