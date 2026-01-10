package com.kuapt.tutor.auth;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, PasswordProperties.class})
public class AuthConfig {
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
