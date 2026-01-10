package com.kuapt.tutor.evaluation;

import com.kuapt.tutor.evaluation.dto.EvaluationCreateRequest;
import com.kuapt.tutor.evaluation.dto.EvaluationDetailResponse;
import com.kuapt.tutor.evaluation.dto.PageEvaluationResponse;
import com.kuapt.tutor.model.EvaluationRecord;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/evaluations")
public class EvaluationController {
  private final EvaluationService evaluationService;

  public EvaluationController(EvaluationService evaluationService) {
    this.evaluationService = evaluationService;
  }

  @GetMapping
  public PageEvaluationResponse list(
      Authentication authentication,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "term", required = false) String term) {
    return evaluationService.list(authentication, page, size, term);
  }

  @PostMapping
  public ResponseEntity<EvaluationRecord> create(Authentication authentication, @Valid @RequestBody EvaluationCreateRequest req) {
    EvaluationRecord created = evaluationService.create(authentication, req);
    return ResponseEntity.status(201).body(created);
  }

  @GetMapping("/{evaluationId}")
  public EvaluationDetailResponse get(Authentication authentication, @PathVariable long evaluationId) {
    EvaluationRecord eval = evaluationService.requireAccessibleEvaluation(authentication, evaluationId);
    return EvaluationDetailResponse.of(eval, evaluationService.listDetails(evaluationId));
  }
}
