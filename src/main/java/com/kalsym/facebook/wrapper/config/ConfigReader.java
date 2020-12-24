package com.kalsym.facebook.wrapper.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;

/**
 * Custom Environment class to read properties in normal java classes where @AutoWired not works,
 * like Utilities.java
 *
 * @author ZEESHAN
 */
@Configuration
@PropertySource("classpath:application.properties")
public class ConfigReader implements EnvironmentAware {

  private static final Logger LOG = LoggerFactory.getLogger("application");
  public static Environment environment;

  @Override
  public void setEnvironment(Environment environment) {
    ConfigReader.environment = environment;
  }

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  public static int getPropertyAsInt(String key, int defaultVal) {
    int propVal;
    try {
      propVal = Integer.parseInt(environment.getProperty(key));
      return propVal;
    } catch (NumberFormatException nfe) {
      LOG.info("Function: getPropertyAsInt(?), Config not found:" + key);
      return defaultVal;
    }
  }
}
