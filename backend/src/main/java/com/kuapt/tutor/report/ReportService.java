package com.kuapt.tutor.report;

import com.kuapt.tutor.auth.AuthErrorCode;
import com.kuapt.tutor.exception.ApiException;
import com.kuapt.tutor.mapper.ReportMapper;
import com.kuapt.tutor.mapper.RoleMapper;
import com.kuapt.tutor.mapper.UserMapper;
import com.kuapt.tutor.model.RoleCode;
import com.kuapt.tutor.model.UserRecord;
import com.kuapt.tutor.model.UserType;
import com.kuapt.tutor.report.dto.ReportActivityStatsResponse;
import com.kuapt.tutor.report.dto.ReportPlanCompletionResponse;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
  private final ReportMapper reportMapper;
  private final UserMapper userMapper;
  private final RoleMapper roleMapper;

  public ReportService(ReportMapper reportMapper, UserMapper userMapper, RoleMapper roleMapper) {
    this.reportMapper = reportMapper;
    this.userMapper = userMapper;
    this.roleMapper = roleMapper;
  }

  public ReportPlanCompletionResponse planCompletion(Authentication authentication, String term, Long collegeId) {
    Requester requester = requireRequester(authentication);
    validateTerm(term);

    if (requester.user().userType() != UserType.TEACHER) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }

    ViewScope scope = viewScopeForTeacher(requester);
    if (scope.adminSchool()) {
      return new ReportPlanCompletionResponse(term, mapPlan(reportMapper.planCompletionByCollege(term, collegeId)));
    }

    if (scope.includeCollege()) {
      if (collegeId != null && collegeId.longValue() != scope.collegeId().longValue()) {
        throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
      }
      return new ReportPlanCompletionResponse(term, mapPlan(reportMapper.planCompletionByCollege(term, scope.collegeId())));
    }

    if (scope.includeTutor()) {
      return new ReportPlanCompletionResponse(term, mapPlan(reportMapper.planCompletionByTutorClasses(requester.userId(), term)));
    }

    throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
  }

  public ReportActivityStatsResponse activityStats(Authentication authentication, String term) {
    Requester requester = requireRequester(authentication);
    validateTerm(term);

    if (requester.user().userType() != UserType.TEACHER) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }

    ViewScope scope = viewScopeForTeacher(requester);
    if (scope.adminSchool()) {
      return new ReportActivityStatsResponse(term, mapActivity(reportMapper.activityStatsAll(term)));
    }

    if (scope.includeCollege()) {
      return new ReportActivityStatsResponse(term, mapActivity(reportMapper.activityStatsByCollege(term, scope.collegeId())));
    }

    if (scope.includeTutor()) {
      return new ReportActivityStatsResponse(term, mapActivity(reportMapper.activityStatsByTutorClasses(term, requester.userId())));
    }

    throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
  }

  private static List<ReportPlanCompletionResponse.Item> mapPlan(List<ReportMapper.PlanCompletionRow> rows) {
    return rows.stream().map(r -> new ReportPlanCompletionResponse.Item(r.scope(), r.doneCount(), r.totalCount(), r.completionRate())).toList();
  }

  private static List<ReportActivityStatsResponse.Item> mapActivity(List<ReportMapper.ActivityStatsRow> rows) {
    return rows.stream().map(r -> new ReportActivityStatsResponse.Item(r.activityId(), r.title(), r.appliedCount(), r.approvedCount())).toList();
  }

  private Requester requireRequester(Authentication authentication) {
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

  private static void validateTerm(String term) {
    if (term == null || !term.matches("\\d{4}-\\d{2}-\\d{2}-[12]")) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "term is invalid");
    }
  }

  private ViewScope viewScopeForTeacher(Requester requester) {
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

  private record ViewScope(boolean adminSchool, Long collegeId, boolean includeTutor, boolean includeCollege) {}

  private record Requester(long userId, UserRecord user, List<RoleCode> roles) {}
}
