package com.kuapt.tutor.auth;

import com.kuapt.tutor.mapper.LocalCredentialMapper;
import com.kuapt.tutor.mapper.RefreshTokenMapper;
import com.kuapt.tutor.mapper.RoleMapper;
import com.kuapt.tutor.mapper.UserMapper;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthBeans {
  @Bean
  public JwtService jwtService(JwtProperties props, Clock clock) {
    return new JwtService(props, clock);
  }

  @Bean
  public RefreshTokenService refreshTokenService(JwtProperties props, RefreshTokenMapper mapper, Clock clock) {
    return new RefreshTokenService(props, mapper, clock);
  }

  @Bean
  public AuthService authService(
      UserMapper userMapper,
      LocalCredentialMapper credentialMapper,
      RoleMapper roleMapper,
      PasswordProperties passwordProps,
      RefreshTokenService refreshTokenService,
      JwtService jwtService,
      Clock clock) {
    return new AuthService(userMapper, credentialMapper, roleMapper, passwordProps, refreshTokenService, jwtService, clock);
  }
}
