package com.kuapt.tutor.user;

import com.kuapt.tutor.auth.AuthErrorCode;
import com.kuapt.tutor.exception.ApiException;
import com.kuapt.tutor.mapper.CollegeMapper;
import com.kuapt.tutor.mapper.RoleMapper;
import com.kuapt.tutor.mapper.UserMapper;
import com.kuapt.tutor.model.RoleCode;
import com.kuapt.tutor.model.UserRecord;
import com.kuapt.tutor.model.UserType;
import com.kuapt.tutor.user.dto.UserImportResponse;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
public class UserImportController {
  private final UserMapper userMapper;
  private final RoleMapper roleMapper;
  private final CollegeMapper collegeMapper;
  private final UserImportService userImportService;

  public UserImportController(
      UserMapper userMapper, RoleMapper roleMapper, CollegeMapper collegeMapper, UserImportService userImportService) {
    this.userMapper = userMapper;
    this.roleMapper = roleMapper;
    this.collegeMapper = collegeMapper;
    this.userImportService = userImportService;
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public UserImportResponse importUsers(
      Authentication authentication, @RequestParam("userType") UserType userType, @RequestParam("file") MultipartFile file) {
    if (authentication == null || authentication.getPrincipal() == null) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }

    long requesterUserId = (long) authentication.getPrincipal();
    UserRecord requester = userMapper.findById(requesterUserId);
    if (requester == null) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }

    List<RoleCode> roles = roleMapper.listRoleCodes(requesterUserId);
    boolean isAdminSchool = roles.contains(RoleCode.ADMIN_SCHOOL);
    boolean isAdminCollege = roles.contains(RoleCode.ADMIN_COLLEGE);
    boolean isTutor = roles.contains(RoleCode.TUTOR);

    if (userType == UserType.STUDENT) {
      if (!(isAdminSchool || isAdminCollege || isTutor)) {
        throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
      }
    } else {
      if (!(isAdminSchool || isAdminCollege)) {
        throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
      }
    }

    String restrictedCollegeName = null;
    if (!isAdminSchool && (isAdminCollege || isTutor)) {
      if (requester.collegeId() == null) {
        throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
      }
      restrictedCollegeName = collegeMapper.findNameById(requester.collegeId());
      if (restrictedCollegeName == null || restrictedCollegeName.isBlank()) {
        throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
      }
    }

    return userImportService.importUsers(userType, file, restrictedCollegeName);
  }
}
