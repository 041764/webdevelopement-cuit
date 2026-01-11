package com.kuapt.tutor.evaluation;

import com.kuapt.tutor.auth.AuthErrorCode;
import com.kuapt.tutor.evaluation.dto.EvaluationCreateRequest;
import com.kuapt.tutor.evaluation.dto.PageEvaluationResponse;
import com.kuapt.tutor.exception.ApiException;
import com.kuapt.tutor.mapper.ClassStudentMapper;
import com.kuapt.tutor.mapper.EvaluationDetailMapper;
import com.kuapt.tutor.mapper.EvaluationMapper;
import com.kuapt.tutor.mapper.UserMapper;
import com.kuapt.tutor.model.EvaluationDetailItemRecord;
import com.kuapt.tutor.service.PageSpec;
import com.kuapt.tutor.service.Requester;
import com.kuapt.tutor.service.ServiceAuth;
import com.kuapt.tutor.service.TermUtil;
import com.kuapt.tutor.service.ViewScope;
import com.kuapt.tutor.model.EvaluationRecord;
import com.kuapt.tutor.model.RoleCode;
import com.kuapt.tutor.model.UserRecord;
import com.kuapt.tutor.model.UserType;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationService {
  private final EvaluationMapper evaluationMapper;
  private final EvaluationDetailMapper detailMapper;
  private final UserMapper userMapper;
  private final ClassStudentMapper classStudentMapper;
  private final ServiceAuth serviceAuth;

  public EvaluationService(
      EvaluationMapper evaluationMapper,
      EvaluationDetailMapper detailMapper,
      UserMapper userMapper,
      ClassStudentMapper classStudentMapper,
      ServiceAuth serviceAuth) {
    this.evaluationMapper = evaluationMapper;
    this.detailMapper = detailMapper;
    this.userMapper = userMapper;
    this.classStudentMapper = classStudentMapper;
    this.serviceAuth = serviceAuth;
  }

  public PageEvaluationResponse list(Authentication authentication, int page, int size, String term) {
    Requester requester = serviceAuth.requireRequester(authentication);
    PageSpec p = PageSpec.of(page, size);
    String normalizedTerm = TermUtil.normalizeOptionalTerm(term);

    if (requester.user().userType() == UserType.STUDENT) {
      long total = evaluationMapper.countForStudent(requester.userId(), normalizedTerm);
      List<EvaluationRecord> items = evaluationMapper.listForStudent(requester.userId(), normalizedTerm, p.size(), p.offset());
      return new PageEvaluationResponse(p.page(), p.size(), total, items);
    }

    ViewScope scope = serviceAuth.viewScopeForTeacher(requester);
    if (scope.adminSchool()) {
      long total = evaluationMapper.countAll(normalizedTerm);
      List<EvaluationRecord> items = evaluationMapper.listAll(normalizedTerm, p.size(), p.offset());
      return new PageEvaluationResponse(p.page(), p.size(), total, items);
    }

    if (scope.includeCollege()) {
      long total = evaluationMapper.countForCollege(scope.collegeId(), normalizedTerm);
      List<EvaluationRecord> items = evaluationMapper.listForCollege(scope.collegeId(), normalizedTerm, p.size(), p.offset());
      return new PageEvaluationResponse(p.page(), p.size(), total, items);
    }

    if (scope.includeTutor()) {
      long total = evaluationMapper.countForTutor(requester.userId(), normalizedTerm);
      List<EvaluationRecord> items = evaluationMapper.listForTutor(requester.userId(), normalizedTerm, p.size(), p.offset());
      return new PageEvaluationResponse(p.page(), p.size(), total, items);
    }

    throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
  }

  @Transactional
  public EvaluationRecord create(Authentication authentication, EvaluationCreateRequest req) {
    Requester requester = serviceAuth.requireRequester(authentication);
    if (requester.user().userType() != UserType.TEACHER) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }

    TermUtil.validateTerm(req.term());
    long evaluateeUserId = requirePositive(req.evaluateeUserId(), "evaluateeUserId is invalid");

    UserRecord student = userMapper.findById(evaluateeUserId);
    if (student == null || student.userType() != UserType.STUDENT) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "evaluateeUserId is invalid");
    }

    assertCanEvaluate(requester, student);

    var p = new EvaluationMapper.EvaluationInsertParams(null, requester.userId(), evaluateeUserId, req.term(), req.scoreTotal(), req.comment());
    evaluationMapper.insert(p);
    Long createdId = p.getId();
    if (createdId == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "evaluation not found");
    }

    List<com.kuapt.tutor.evaluation.dto.EvaluationDetailItem> details = req.details();
    if (details != null) {
      for (var d : details) {
        detailMapper.insert(createdId, d.itemKey(), d.score(), d.comment());
      }
    }

    EvaluationRecord created = evaluationMapper.findById(createdId);
    if (created == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "evaluation not found");
    }
    return created;
  }

  @Transactional(readOnly = true)
  public EvaluationRecord requireAccessibleEvaluation(Authentication authentication, long evaluationId) {
    Requester requester = serviceAuth.requireRequester(authentication);
    EvaluationRecord eval = evaluationMapper.findById(evaluationId);
    if (eval == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "evaluation not found");
    }

    if (requester.user().userType() == UserType.STUDENT) {
      if (eval.evaluateeUserId() != requester.userId()) {
        throw new ApiException(AuthErrorCode.NOT_FOUND, "evaluation not found");
      }
      return eval;
    }

    UserRecord student = userMapper.findById(eval.evaluateeUserId());
    if (student == null || student.userType() != UserType.STUDENT) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "evaluation not found");
    }

    if (canReadTeacher(requester, student)) {
      return eval;
    }

    throw new ApiException(AuthErrorCode.NOT_FOUND, "evaluation not found");
  }

  public List<EvaluationDetailItemRecord> listDetails(long evaluationId) {
    return detailMapper.listByEvaluationId(evaluationId);
  }

  private boolean canReadTeacher(Requester requester, UserRecord student) {
    ViewScope scope = serviceAuth.viewScopeForTeacher(requester);
    if (scope.adminSchool()) {
      return true;
    }
    if (scope.includeCollege()) {
      Long collegeId = student.collegeId();
      return collegeId != null && collegeId.longValue() == scope.collegeId();
    }
    if (scope.includeTutor()) {
      return isTutorOfStudent(requester.userId(), student.userId());
    }
    return false;
  }

  private void assertCanEvaluate(Requester requester, UserRecord student) {
    if (canReadTeacher(requester, student)) {
      return;
    }
    throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
  }

  private boolean isTutorOfStudent(long tutorUserId, long studentUserId) {
    return classStudentMapper.isTutorOfStudent(tutorUserId, studentUserId);
  }

  private static long requirePositive(Long v, String msg) {
    if (v == null || v.longValue() <= 0) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, msg);
    }
    return v;
  }

}
