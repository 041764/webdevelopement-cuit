package com.kuapt.tutor.plan;

import com.kuapt.tutor.model.PlanItemProgressRecord;
import com.kuapt.tutor.plan.dto.PlanItemProgressCreateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/plan-items")
public class PlanItemProgressController {
  private final PlanService planService;

  public PlanItemProgressController(PlanService planService) {
    this.planService = planService;
  }

  @GetMapping("/{itemId}/progress")
  public List<PlanItemProgressRecord> list(Authentication authentication, @PathVariable("itemId") long itemId) {
    return planService.listProgress(authentication, itemId);
  }

  @PostMapping("/{itemId}/progress")
  public ResponseEntity<PlanItemProgressRecord> add(
      Authentication authentication, @PathVariable("itemId") long itemId, @Valid @RequestBody PlanItemProgressCreateRequest req) {
    PlanItemProgressRecord created = planService.addProgress(authentication, itemId, req);
    return ResponseEntity.status(201).body(created);
  }
}

