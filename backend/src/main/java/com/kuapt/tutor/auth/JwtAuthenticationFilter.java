package com.kuapt.tutor.auth;

import com.kuapt.tutor.mapper.RoleMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final RoleMapper roleMapper;

  public JwtAuthenticationFilter(JwtService jwtService, RoleMapper roleMapper) {
    this.jwtService = jwtService;
    this.roleMapper = roleMapper;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = header.substring("Bearer ".length()).trim();
    try {
      Claims claims = jwtService.parseAndValidate(token);
      long userId = Long.parseLong(claims.getSubject());
      List<SimpleGrantedAuthority> authorities = roleMapper.listRoleCodes(userId).stream()
          .map(rc -> new SimpleGrantedAuthority("ROLE_" + rc.name()))
          .toList();
      var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
      SecurityContextHolder.getContext().setAuthentication(auth);
    } catch (JwtException | IllegalArgumentException e) {
      // Ignore invalid token; endpoint-level auth will reject if needed.
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }
}
