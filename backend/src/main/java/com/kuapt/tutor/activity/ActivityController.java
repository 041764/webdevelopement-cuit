package com.kuapt.tutor.activity;

import com.kuapt.tutor.activity.dto.ActivityCreateRequest;
import com.kuapt.tutor.activity.dto.ActivityRejectRequest;
import com.kuapt.tutor.activity.dto.ActivitySignupRequest;
import com.kuapt.tutor.activity.dto.PageActivity;
import com.kuapt.tutor.activity.dto.PageActivitySignup;
import com.kuapt.tutor.model.ActivityRecord;
import com.kuapt.tutor.model.ActivitySignupRecord;
import com.kuapt.tutor.model.ActivityStatus;
import com.kuapt.tutor.model.SignupStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/activities")
public class ActivityController {
  private final ActivityService activityService;

  public ActivityController(ActivityService activityService) {
    this.activityService = activityService;
  }

  @GetMapping
  public PageActivity list(
      Authentication authentication,
      @RequestParam(value = "page", required = false, defaultValue = "1") int page,
      @RequestParam(value = "size", required = false, defaultValue = "20") int size,
      @RequestParam(value = "term", required = false) String term,
      @RequestParam(value = "status", required = false) ActivityStatus status) {
    return activityService.list(authentication, page, size, term, status);
  }

  @PostMapping
  public ResponseEntity<ActivityRecord> create(Authentication authentication, @Valid @RequestBody ActivityCreateRequest req) {
    ActivityRecord created = activityService.create(authentication, req);
    return ResponseEntity.status(201).body(created);
  }

  @GetMapping("/{activityId}")
  public ActivityRecord get(Authentication authentication, @PathVariable("activityId") long activityId) {
    return activityService.get(authentication, activityId);
  }

  @PostMapping("/{activityId}/publish")
  public ResponseEntity<Void> publish(Authentication authentication, @PathVariable("activityId") long activityId) {
    activityService.publish(authentication, activityId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{activityId}/signups")
  public ResponseEntity<ActivitySignupRecord> signup(
      Authentication authentication,
      @PathVariable("activityId") long activityId,
      @RequestBody(required = false) ActivitySignupRequest req) {
    ActivitySignupRecord created = activityService.signup(authentication, activityId, req == null ? null : req.note());
    return ResponseEntity.status(201).body(created);
  }

  @GetMapping("/{activityId}/signups")
  public PageActivitySignup listSignups(
      Authentication authentication,
      @PathVariable("activityId") long activityId,
      @RequestParam(value = "page", required = false, defaultValue = "1") int page,
      @RequestParam(value = "size", required = false, defaultValue = "20") int size,
      @RequestParam(value = "status", required = false) SignupStatus status) {
    return activityService.listSignups(authentication, activityId, page, size, status);
  }

  @DeleteMapping("/{activityId}/signups/me")
  public ResponseEntity<Void> cancelMySignup(Authentication authentication, @PathVariable("activityId") long activityId) {
    activityService.cancelMySignup(authentication, activityId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{activityId}/signups/{signupId}/approve")
  public ResponseEntity<Void> approve(
      Authentication authentication, @PathVariable("activityId") long activityId, @PathVariable("signupId") long signupId) {
    activityService.approve(authentication, activityId, signupId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{activityId}/signups/{signupId}/reject")
  public ResponseEntity<Void> reject(
      Authentication authentication,
      @PathVariable("activityId") long activityId,
      @PathVariable("signupId") long signupId,
      @RequestBody(required = false) ActivityRejectRequest req) {
    activityService.reject(authentication, activityId, signupId, req == null ? null : req.reason());
    return ResponseEntity.noContent().build();
  }
}

