package com.kuapt.tutor.controller;

import com.kuapt.tutor.auth.AuthErrorCode;
import com.kuapt.tutor.exception.ApiException;
import com.kuapt.tutor.mapper.RoleMapper;
import com.kuapt.tutor.mapper.UserMapper;
import com.kuapt.tutor.model.RoleCode;
import com.kuapt.tutor.model.UserRecord;
import com.kuapt.tutor.service.LookupService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lookup")
public class LookupController {
  private final LookupService lookupService;
  private final UserMapper userMapper;
  private final RoleMapper roleMapper;

  public LookupController(LookupService lookupService, UserMapper userMapper, RoleMapper roleMapper) {
    this.lookupService = lookupService;
    this.userMapper = userMapper;
    this.roleMapper = roleMapper;
  }

  @GetMapping("/students")
  public List<LookupService.StudentOption> listStudents(
      Authentication authentication,
      @RequestParam(required = false) String term) {
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
    
    if (!(isAdminSchool || isAdminCollege || isTutor)) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }
    
    Long collegeId = isAdminSchool ? null : requester.collegeId();
    return lookupService.listStudents(collegeId);
  }

  @GetMapping("/classes")
  public List<LookupService.ClassOption> listClasses(
      Authentication authentication,
      @RequestParam(required = false) String term) {
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
    
    if (!(isAdminSchool || isAdminCollege || isTutor)) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }
    
    Long collegeId = isAdminSchool ? null : requester.collegeId();
    return lookupService.listClasses(collegeId, term);
  }

  @GetMapping("/colleges")
  public List<LookupService.CollegeOption> listColleges(Authentication authentication) {
    if (authentication == null || authentication.getPrincipal() == null) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }
    long requesterUserId = (long) authentication.getPrincipal();
    UserRecord requester = userMapper.findById(requesterUserId);
    if (requester == null) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }
    
    return lookupService.listColleges();
  }
}
