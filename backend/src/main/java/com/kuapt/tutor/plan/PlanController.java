package com.kuapt.tutor.plan;

import com.kuapt.tutor.model.PlanItemRecord;
import com.kuapt.tutor.model.PlanRecord;
import com.kuapt.tutor.plan.dto.PagePlan;
import com.kuapt.tutor.plan.dto.PlanCreateRequest;
import com.kuapt.tutor.plan.dto.PlanDetail;
import com.kuapt.tutor.plan.dto.PlanItemCreateRequest;
import com.kuapt.tutor.plan.dto.PlanItemUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/plans")
public class PlanController {
  private final PlanService planService;

  public PlanController(PlanService planService) {
    this.planService = planService;
  }

  @GetMapping
  public PagePlan list(
      Authentication authentication,
      @RequestParam(value = "page", required = false, defaultValue = "1") int page,
      @RequestParam(value = "size", required = false, defaultValue = "20") int size,
      @RequestParam(value = "term", required = false) String term) {
    return planService.list(authentication, page, size, term);
  }

  @PostMapping
  public ResponseEntity<PlanRecord> create(Authentication authentication, @Valid @RequestBody PlanCreateRequest req) {
    PlanRecord created = planService.create(authentication, req);
    return ResponseEntity.status(201).body(created);
  }

  @GetMapping("/{planId}")
  public PlanDetail get(Authentication authentication, @PathVariable("planId") long planId) {
    return planService.get(authentication, planId);
  }

  @PostMapping("/{planId}/items")
  public ResponseEntity<PlanItemRecord> addItem(
      Authentication authentication, @PathVariable("planId") long planId, @Valid @RequestBody PlanItemCreateRequest req) {
    PlanItemRecord created = planService.addItem(authentication, planId, req);
    return ResponseEntity.status(201).body(created);
  }

  @PatchMapping("/{planId}/items/{itemId}")
  public ResponseEntity<Void> updateItem(
      Authentication authentication,
      @PathVariable("planId") long planId,
      @PathVariable("itemId") long itemId,
      @RequestBody PlanItemUpdateRequest req) {
    planService.updateItem(authentication, planId, itemId, req);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{planId}/items/{itemId}")
  public ResponseEntity<Void> deleteItem(
      Authentication authentication, @PathVariable("planId") long planId, @PathVariable("itemId") long itemId) {
    planService.deleteItem(authentication, planId, itemId);
    return ResponseEntity.noContent().build();
  }
}

