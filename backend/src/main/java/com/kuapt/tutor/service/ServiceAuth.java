package com.kuapt.tutor.service;

import com.kuapt.tutor.auth.AuthErrorCode;
import com.kuapt.tutor.exception.ApiException;
import com.kuapt.tutor.mapper.RoleMapper;
import com.kuapt.tutor.mapper.UserMapper;
import com.kuapt.tutor.model.RoleCode;
import com.kuapt.tutor.model.UserRecord;
import com.kuapt.tutor.model.UserType;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class ServiceAuth {
  private final UserMapper userMapper;
  private final RoleMapper roleMapper;

  public ServiceAuth(UserMapper userMapper, RoleMapper roleMapper) {
    this.userMapper = userMapper;
    this.roleMapper = roleMapper;
  }

  public Requester requireRequester(Authentication authentication) {
    if (authentication == null || authentication.getPrincipal() == null) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }
    long userId = (long) authentication.getPrincipal();
    UserRecord u = userMapper.findById(userId);
    if (u == null) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }
    List<RoleCode> roles = roleMapper.listRoleCodes(userId);
    return new Requester(userId, u, roles);
  }

  public ViewScope viewScopeForTeacher(Requester requester) {
    List<RoleCode> roles = requester.roles();
    boolean isAdminSchool = roles.contains(RoleCode.ADMIN_SCHOOL);
    boolean isAdminCollege = roles.contains(RoleCode.ADMIN_COLLEGE);
    boolean isTutor = roles.contains(RoleCode.TUTOR);
    if (isAdminSchool) {
      return new ViewScope(true, null, false, false);
    }
    Long collegeId = null;
    boolean includeCollege = false;
    if (isAdminCollege) {
      if (requester.user().collegeId() == null) {
        throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
      }
      collegeId = requester.user().collegeId();
      includeCollege = true;
    }
    return new ViewScope(false, collegeId, isTutor, includeCollege);
  }

  public void requireTeacher(Requester requester) {
    if (requester.user().userType() != UserType.TEACHER) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }
  }

  public void requireStudent(Requester requester) {
    if (requester.user().userType() != UserType.STUDENT) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }
  }
}
