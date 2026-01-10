package com.kuapt.tutor.evaluation;

import com.kuapt.tutor.auth.AuthErrorCode;
import com.kuapt.tutor.evaluation.dto.EvaluationCreateRequest;
import com.kuapt.tutor.evaluation.dto.PageEvaluationResponse;
import com.kuapt.tutor.exception.ApiException;
import com.kuapt.tutor.mapper.ClassStudentMapper;
import com.kuapt.tutor.mapper.EvaluationDetailMapper;
import com.kuapt.tutor.mapper.EvaluationMapper;
import com.kuapt.tutor.mapper.RoleMapper;
import com.kuapt.tutor.mapper.UserMapper;
import com.kuapt.tutor.model.EvaluationDetailItemRecord;
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
  private final RoleMapper roleMapper;
  private final ClassStudentMapper classStudentMapper;

  public EvaluationService(
      EvaluationMapper evaluationMapper,
      EvaluationDetailMapper detailMapper,
      UserMapper userMapper,
      RoleMapper roleMapper,
      ClassStudentMapper classStudentMapper) {
    this.evaluationMapper = evaluationMapper;
    this.detailMapper = detailMapper;
    this.userMapper = userMapper;
    this.roleMapper = roleMapper;
    this.classStudentMapper = classStudentMapper;
  }

  public PageEvaluationResponse list(Authentication authentication, int page, int size, String term) {
    Requester requester = requireRequester(authentication);
    PageSpec p = PageSpec.of(page, size);
    String normalizedTerm = normalizeOptionalTerm(term);

    if (requester.user().userType() == UserType.STUDENT) {
      long total = evaluationMapper.countForStudent(requester.userId(), normalizedTerm);
      List<EvaluationRecord> items = evaluationMapper.listForStudent(requester.userId(), normalizedTerm, p.size(), p.offset());
      return new PageEvaluationResponse(p.page(), p.size(), total, items);
    }

    ViewScope scope = viewScopeForTeacher(requester);
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
    Requester requester = requireRequester(authentication);
    if (requester.user().userType() != UserType.TEACHER) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }

    validateTerm(req.term());
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
    Requester requester = requireRequester(authentication);
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
    ViewScope scope = viewScopeForTeacher(requester);
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

  private static String normalizeOptionalTerm(String term) {
    if (term == null) {
      return null;
    }
    String t = term.trim();
    if (t.isEmpty()) {
      return null;
    }
    validateTerm(t);
    return t;
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

  private record PageSpec(int page, int size, int offset) {
    static PageSpec of(int page, int size) {
      if (page < 1) {
        throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "page is invalid");
      }
      if (size < 1 || size > 200) {
        throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "size is invalid");
      }
      return new PageSpec(page, size, Math.multiplyExact(page - 1, size));
    }
  }

  private record ViewScope(boolean adminSchool, Long collegeId, boolean includeTutor, boolean includeCollege) {}

  private record Requester(long userId, UserRecord user, List<RoleCode> roles) {}
}
