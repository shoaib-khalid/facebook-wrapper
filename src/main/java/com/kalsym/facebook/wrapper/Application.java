package com.kalsym.facebook.wrapper;

import com.github.messenger4j.Messenger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

  @Bean
  public Messenger messenger(
      @Value("${messenger.pageAccessToken}") String pageAccessToken,
      @Value("${messenger.appSecret}") final String appSecret,
      @Value("${messenger.verifyToken}") final String verifyToken) {
    return Messenger.create(pageAccessToken, appSecret, verifyToken);
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
    System.out.println(
        "                                               \n"
            + ",--. ,--.        ,--.                          \n"
            + "|  .'   / ,--,--.|  | ,---.,--. ,--.,--,--,--. \n"
            + "|  .   ' ' ,-.  ||  |(  .-' \\  '  / |        | \n"
            + "|  |\\   \\\\ '-'  ||  |.-'  `) \\   '  |  |  |  | \n"
            + "`--' '--' `--`--'`--'`----'.-'  /   `--`--`--' \n"
            + "                           `---'               ");
  }
}
