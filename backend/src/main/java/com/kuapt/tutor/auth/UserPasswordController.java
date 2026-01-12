package com.kuapt.tutor.auth;

import com.kuapt.tutor.auth.dto.PasswordResetByNoRequest;
import com.kuapt.tutor.auth.dto.PasswordResetRequest;
import com.kuapt.tutor.exception.ApiException;
import com.kuapt.tutor.mapper.RoleMapper;
import com.kuapt.tutor.mapper.UserMapper;
import com.kuapt.tutor.model.RoleCode;
import com.kuapt.tutor.model.UserRecord;
import com.kuapt.tutor.model.UserType;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserPasswordController {
  private final AuthService authService;
  private final UserMapper userMapper;
  private final RoleMapper roleMapper;

  public UserPasswordController(AuthService authService, UserMapper userMapper, RoleMapper roleMapper) {
    this.authService = authService;
    this.userMapper = userMapper;
    this.roleMapper = roleMapper;
  }

  @PostMapping("/password:reset-by-no")
  public ResponseEntity<Void> resetPasswordByNo(
      Authentication authentication,
      @Valid @RequestBody PasswordResetByNoRequest req) {
    UserRecord target = userMapper.findByTypeAndNo(req.userType(), req.userNo());
    if (target == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "user not found");
    }
    return doResetPassword(authentication, target, req.clientSalt(), req.clientHash());
  }

  @PostMapping("/{userId}/password:reset")
  public ResponseEntity<Void> resetPassword(
      Authentication authentication,
      @PathVariable("userId") long userId,
      @Valid @RequestBody PasswordResetRequest req) {
    UserRecord target = userMapper.findById(userId);
    if (target == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "user not found");
    }
    return doResetPassword(authentication, target, req.clientSalt(), req.clientHash());
  }

  private ResponseEntity<Void> doResetPassword(Authentication authentication, UserRecord target, String clientSalt, String clientHash) {
    if (authentication == null || authentication.getPrincipal() == null) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }

    long requesterUserId = (long) authentication.getPrincipal();
    UserRecord requester = userMapper.findById(requesterUserId);
    if (requester == null) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }

    List<RoleCode> requesterRoles = roleMapper.listRoleCodes(requesterUserId);
    boolean isAdminSchool = requesterRoles.contains(RoleCode.ADMIN_SCHOOL);
    boolean isAdminCollege = requesterRoles.contains(RoleCode.ADMIN_COLLEGE);
    boolean isTutor = requesterRoles.contains(RoleCode.TUTOR);

    if (target.userType() == UserType.STUDENT) {
      if (!(isAdminSchool || isAdminCollege || isTutor)) {
        throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
      }
      if (isAdminCollege && !sameCollege(requester, target)) {
        throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
      }
    } else {
      // TEACHER password reset only by ADMIN_*.
      if (!(isAdminSchool || isAdminCollege)) {
        throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
      }
      if (isAdminCollege && !sameCollege(requester, target)) {
        throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
      }
    }

    authService.resetPassword(target.userId(), clientSalt, clientHash);
    return ResponseEntity.noContent().build();
  }

  private static boolean sameCollege(UserRecord a, UserRecord b) {
    return a.collegeId() != null && b.collegeId() != null && a.collegeId().equals(b.collegeId());
  }
}
