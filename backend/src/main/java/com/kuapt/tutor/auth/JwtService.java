package com.kuapt.tutor.auth;

import com.kuapt.tutor.model.RoleCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public class JwtService {
  private final JwtProperties props;
  private final Clock clock;
  private final Key key;

  public JwtService(JwtProperties props, Clock clock) {
    this.props = props;
    this.clock = clock;
    this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
  }

  public TokenPair issueTokenPair(long userId, List<RoleCode> roles, String deviceId, RefreshTokenService refreshTokenService) {
    Instant now = clock.instant();
    Instant accessExp = now.plus(props.accessTtl());
    String access = Jwts.builder()
        .setIssuer(props.issuer())
        .setSubject(Long.toString(userId))
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(accessExp))
        .claim("roles", roles.stream().map(Enum::name).toList())
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();

    RefreshTokenService.RefreshIssueResult refresh = refreshTokenService.issue(userId, deviceId);
    return new TokenPair(access, refresh.refreshToken(), accessExp, refresh.expiresAt());
  }

  public Claims parseAndValidate(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }
}
