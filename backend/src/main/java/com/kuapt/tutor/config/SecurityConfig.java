package com.kuapt.tutor.config;

import com.kuapt.tutor.auth.JwtAuthenticationFilter;
import com.kuapt.tutor.auth.JwtService;
import com.kuapt.tutor.mapper.RoleMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService, RoleMapper roleMapper)
      throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(new JwtAuthenticationFilter(jwtService, roleMapper), UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(eh -> eh.authenticationEntryPoint((request, response, authException) -> response.sendError(401)))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/health",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/auth/login",
                "/auth/refresh")
            .permitAll()
            .anyRequest()
            .authenticated())
        .build();
  }
}
