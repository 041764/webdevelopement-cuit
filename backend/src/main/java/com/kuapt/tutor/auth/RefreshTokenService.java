package com.kuapt.tutor.auth;

import com.kuapt.tutor.exception.ApiException;
import com.kuapt.tutor.mapper.RefreshTokenMapper;
import com.kuapt.tutor.model.RefreshTokenRecord;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import org.springframework.transaction.annotation.Transactional;

public class RefreshTokenService {
  public record RefreshIssueResult(String refreshToken, Instant expiresAt) {}

  private final JwtProperties props;
  private final RefreshTokenMapper mapper;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  public RefreshTokenService(JwtProperties props, RefreshTokenMapper mapper, Clock clock) {
    this.props = props;
    this.mapper = mapper;
    this.clock = clock;
  }

  @Transactional
  public RefreshIssueResult issue(long userId, String deviceId) {
    Instant now = clock.instant();
    Instant expiresAt = now.plus(props.refreshTtl());
    String raw = generateToken();
    String hash = sha256Base64(raw);
    mapper.insert(userId, hash, deviceId, now, expiresAt);
    return new RefreshIssueResult(raw, expiresAt);
  }

  @Transactional
  public RefreshTokenRecord requireValid(String rawToken) {
    Instant now = clock.instant();
    String hash = sha256Base64(rawToken);
    RefreshTokenRecord rec = mapper.findByTokenHash(hash);
    if (rec == null) {
      throw new ApiException(AuthErrorCode.AUTH_TOKEN_REVOKED, "refresh token not found");
    }
    if (rec.isRevoked()) {
      throw new ApiException(AuthErrorCode.AUTH_TOKEN_REVOKED, "refresh token revoked");
    }
    if (rec.isExpired(now)) {
      throw new ApiException(AuthErrorCode.AUTH_TOKEN_EXPIRED, "refresh token expired");
    }
    return rec;
  }

  @Transactional
  public void revoke(String rawToken) {
    Instant now = clock.instant();
    String hash = sha256Base64(rawToken);
    mapper.revokeByHash(hash, now);
  }

  private String generateToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String sha256Base64(String raw) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] out = digest.digest(raw.getBytes());
      return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
    } catch (Exception e) {
      throw new IllegalStateException("sha256 unavailable", e);
    }
  }
}
