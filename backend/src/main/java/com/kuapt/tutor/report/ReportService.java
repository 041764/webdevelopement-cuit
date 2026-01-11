package com.kuapt.tutor.report;

import com.kuapt.tutor.auth.AuthErrorCode;
import com.kuapt.tutor.exception.ApiException;
import com.kuapt.tutor.mapper.ReportMapper;
import com.kuapt.tutor.model.UserType;
import com.kuapt.tutor.service.Requester;
import com.kuapt.tutor.service.ServiceAuth;
import com.kuapt.tutor.service.TermUtil;
import com.kuapt.tutor.service.ViewScope;
import com.kuapt.tutor.report.dto.ReportActivityStatsResponse;
import com.kuapt.tutor.report.dto.ReportPlanCompletionResponse;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
  private final ReportMapper reportMapper;
  private final ServiceAuth serviceAuth;

  public ReportService(ReportMapper reportMapper, ServiceAuth serviceAuth) {
    this.reportMapper = reportMapper;
    this.serviceAuth = serviceAuth;
  }

  public ReportPlanCompletionResponse planCompletion(Authentication authentication, String term, Long collegeId) {
    Requester requester = serviceAuth.requireRequester(authentication);
    TermUtil.validateTerm(term);

    if (requester.user().userType() != UserType.TEACHER) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }

    ViewScope scope = serviceAuth.viewScopeForTeacher(requester);
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
    Requester requester = serviceAuth.requireRequester(authentication);
    TermUtil.validateTerm(term);

    if (requester.user().userType() != UserType.TEACHER) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }

    ViewScope scope = serviceAuth.viewScopeForTeacher(requester);
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

}
