package com.kuapt.tutor.model;

import java.time.Instant;

public record RefreshTokenRecord(
    long id,
    long userId,
    String tokenHash,
    String deviceId,
    Instant issuedAt,
    Instant expiresAt,
    Instant revokedAt) {
  public boolean isRevoked() {
    return revokedAt != null;
  }

  public boolean isExpired(Instant now) {
    return expiresAt.isBefore(now) || expiresAt.equals(now);
  }
}
