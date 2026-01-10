package com.kuapt.tutor.auth;

import com.kuapt.tutor.auth.dto.LoginRequest;
import com.kuapt.tutor.auth.dto.LogoutRequest;
import com.kuapt.tutor.auth.dto.RefreshRequest;
import com.kuapt.tutor.exception.ApiException;
import com.kuapt.tutor.mapper.RoleMapper;
import com.kuapt.tutor.mapper.UserMapper;
import com.kuapt.tutor.model.UserRecord;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AuthService authService;
  private final UserMapper userMapper;
  private final RoleMapper roleMapper;

  public AuthController(AuthService authService, UserMapper userMapper, RoleMapper roleMapper) {
    this.authService = authService;
    this.userMapper = userMapper;
    this.roleMapper = roleMapper;
  }

  @PostMapping("/login")
  public TokenPair login(@Valid @RequestBody LoginRequest req) {
    return authService.login(req.userType(), req.id(), req.clientSalt(), req.clientHash(), req.deviceId());
  }

  @PostMapping("/refresh")
  public TokenPair refresh(@Valid @RequestBody RefreshRequest req) {
    return authService.refresh(req.refreshToken(), req.deviceId());
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest req) {
    authService.logout(req.refreshToken());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public Map<String, Object> me(Authentication authentication) {
    if (authentication == null || authentication.getPrincipal() == null) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }
    long userId = (long) authentication.getPrincipal();
    UserRecord user = userMapper.findById(userId);
    if (user == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "user not found");
    }
    var roles = roleMapper.listRoleCodes(userId);

    Map<String, Object> out = new HashMap<>();
    out.put("userId", user.userId());
    out.put("userType", user.userType());
    out.put("id", user.id());
    out.put("name", user.name());
    out.put("collegeId", user.collegeId());
    out.put("status", user.status());
    out.put("roles", roles);
    return out;
  }
}
