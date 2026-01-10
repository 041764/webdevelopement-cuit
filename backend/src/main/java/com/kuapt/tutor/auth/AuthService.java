package com.kuapt.tutor.auth;

import com.kuapt.tutor.exception.ApiException;
import com.kuapt.tutor.mapper.LocalCredentialMapper;
import com.kuapt.tutor.mapper.RoleMapper;
import com.kuapt.tutor.mapper.UserMapper;
import com.kuapt.tutor.model.LocalCredentialRecord;
import com.kuapt.tutor.model.RoleCode;
import com.kuapt.tutor.model.UserRecord;
import com.kuapt.tutor.model.UserType;
import java.time.Clock;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

public class AuthService {
  private final UserMapper userMapper;
  private final LocalCredentialMapper credentialMapper;
  private final RoleMapper roleMapper;
  private final PasswordProperties passwordProps;
  private final RefreshTokenService refreshTokenService;
  private final JwtService jwtService;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final Clock clock;

  public AuthService(
      UserMapper userMapper,
      LocalCredentialMapper credentialMapper,
      RoleMapper roleMapper,
      PasswordProperties passwordProps,
      RefreshTokenService refreshTokenService,
      JwtService jwtService,
      Clock clock) {
    this.userMapper = userMapper;
    this.credentialMapper = credentialMapper;
    this.roleMapper = roleMapper;
    this.passwordProps = passwordProps;
    this.refreshTokenService = refreshTokenService;
    this.jwtService = jwtService;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public TokenPair login(UserType userType, String userNo, String clientSalt, String clientHash, String deviceId) {
    UserRecord user = userMapper.findByTypeAndNo(userType, userNo);
    if (user == null) {
      throw new ApiException(AuthErrorCode.AUTH_INVALID_CREDENTIALS, "invalid credentials");
    }
    LocalCredentialRecord cred = credentialMapper.findByUserId(user.userId());
    if (cred == null || cred.serverHash() == null) {
      throw new ApiException(AuthErrorCode.AUTH_INVALID_CREDENTIALS, "invalid credentials");
    }

    String combined = passwordProps.pepper() + ":" + clientHash;
    if (!passwordEncoder.matches(combined, cred.serverHash())) {
      throw new ApiException(AuthErrorCode.AUTH_INVALID_CREDENTIALS, "invalid credentials");
    }

    List<RoleCode> roles = roleMapper.listRoleCodes(user.userId());
    return jwtService.issueTokenPair(user.userId(), roles, deviceId, refreshTokenService);
  }

  @Transactional
  public TokenPair refresh(String refreshToken, String deviceId) {
    var rec = refreshTokenService.requireValid(refreshToken);
    // rotate refresh token: revoke old, issue new
    refreshTokenService.revoke(refreshToken);
    List<RoleCode> roles = roleMapper.listRoleCodes(rec.userId());
    return jwtService.issueTokenPair(rec.userId(), roles, deviceId, refreshTokenService);
  }

  @Transactional
  public void logout(String refreshToken) {
    refreshTokenService.revoke(refreshToken);
  }

  @Transactional
  public void resetPassword(long targetUserId, String clientSalt, String clientHash) {
    // 权限由 Controller/Security 层约束，本方法只做写入。
    String combined = passwordProps.pepper() + ":" + clientHash;
    String serverHash = passwordEncoder.encode(combined);

    LocalCredentialRecord existing = credentialMapper.findByUserId(targetUserId);
    if (existing == null) {
      credentialMapper.insert(targetUserId, clientSalt, clientHash, serverHash);
    } else {
      credentialMapper.update(targetUserId, clientSalt, clientHash, serverHash);
    }
  }
}
