package com.kuapt.tutor.report;

import com.kuapt.tutor.report.dto.ReportActivityStatsResponse;
import com.kuapt.tutor.report.dto.ReportPlanCompletionResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
public class ReportController {
  private final ReportService reportService;

  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @GetMapping("/plan-completion")
  public ReportPlanCompletionResponse planCompletion(
      Authentication authentication,
      @RequestParam(name = "term") String term,
      @RequestParam(name = "collegeId", required = false) Long collegeId) {
    return reportService.planCompletion(authentication, term, collegeId);
  }

  @GetMapping("/activity-stats")
  public ReportActivityStatsResponse activityStats(Authentication authentication, @RequestParam(name = "term") String term) {
    return reportService.activityStats(authentication, term);
  }
}
