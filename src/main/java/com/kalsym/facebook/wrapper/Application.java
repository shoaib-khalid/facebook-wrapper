package com.kalsym.facebook.wrapper;

import com.github.messenger4j.Messenger;
import com.kalsym.facebook.wrapper.expiry.SelfExpiringHashMap;
import com.kalsym.facebook.wrapper.expiry.SelfExpiringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Application {

    @Value("${build.version:not-known}")
    String version;
    @Autowired
    Environment env;

    public static String handoverSessionExpiryMiliseconds;

    private static final Logger LOG = LoggerFactory.getLogger("application");
    // key = SenderId
    // value = roomId
    public static SelfExpiringMap<String, String> agent_sessions;

    @Bean
    public Messenger messenger(
            @Value("${messenger.pageAccessToken}") String pageAccessToken,
            @Value("${messenger.appSecret}") final String appSecret,
            @Value("${messenger.verifyToken}") final String verifyToken) {
        return Messenger.create(pageAccessToken, appSecret, verifyToken);
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println();
    }

    @Bean
    CommandLineRunner lookup(ApplicationContext context) {
        return args -> {
            VersionHolder.VERSION = version;

            LOG.info("[v{}][{}] {}", VersionHolder.VERSION, "", "\n"
                    + "                                               \n"
                    + ",--. ,--.        ,--.                          \n"
                    + "|  .'   / ,--,--.|  | ,---.,--. ,--.,--,--,--. \n"
                    + "|  .   ' ' ,-.  ||  |(  .-' \\  '  / |        | \n"
                    + "|  |\\   \\\\ '-'  ||  |.-'  `) \\   '  |  |  |  | \n"
                    + "`--' '--' `--`--'`--'`----'.-'  /   `--`--`--' \n"
                    + "                           `---'               "
                    + " :: com.kalsym ::              (v" + VersionHolder.VERSION + ")");
            handoverSessionExpiryMiliseconds = env.getProperty("handover.session.expiry.in.miliseconds", "432000000");
            long hashmapSize = Long.parseLong(handoverSessionExpiryMiliseconds);
            agent_sessions = new SelfExpiringHashMap<>(hashmapSize);
        };
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }
}
