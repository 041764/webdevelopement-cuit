package com.kuapt.tutor.activity;

import com.kuapt.tutor.activity.dto.ActivityCreateRequest;
import com.kuapt.tutor.activity.dto.PageActivity;
import com.kuapt.tutor.activity.dto.PageActivitySignup;
import com.kuapt.tutor.auth.AuthErrorCode;
import com.kuapt.tutor.exception.ApiException;
import com.kuapt.tutor.mapper.ActivityMapper;
import com.kuapt.tutor.mapper.ActivitySignupMapper;
import com.kuapt.tutor.mapper.ClassMapper;
import com.kuapt.tutor.mapper.RoleMapper;
import com.kuapt.tutor.mapper.UserMapper;
import com.kuapt.tutor.model.ActivityRecord;
import com.kuapt.tutor.model.ActivitySignupRecord;
import com.kuapt.tutor.model.ActivityStatus;
import com.kuapt.tutor.model.ClassRecord;
import com.kuapt.tutor.model.RoleCode;
import com.kuapt.tutor.model.SignupStatus;
import com.kuapt.tutor.model.UserRecord;
import com.kuapt.tutor.model.UserType;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

@Service
public class ActivityService {
  private final ActivityMapper activityMapper;
  private final ActivitySignupMapper signupMapper;
  private final ClassMapper classMapper;
  private final UserMapper userMapper;
  private final RoleMapper roleMapper;

  public ActivityService(
      ActivityMapper activityMapper,
      ActivitySignupMapper signupMapper,
      ClassMapper classMapper,
      UserMapper userMapper,
      RoleMapper roleMapper) {
    this.activityMapper = activityMapper;
    this.signupMapper = signupMapper;
    this.classMapper = classMapper;
    this.userMapper = userMapper;
    this.roleMapper = roleMapper;
  }

  public PageActivity list(Authentication authentication, int page, int size, String term, ActivityStatus status) {
    Requester requester = requireRequester(authentication);
    PageSpec p = PageSpec.of(page, size);

    if (requester.user().userType() == UserType.STUDENT) {
      long total = activityMapper.countForStudent(requester.userId(), term, status);
      List<ActivityRecord> items = activityMapper.listForStudent(requester.userId(), term, status, p.size(), p.offset());
      return new PageActivity(p.page(), p.size(), total, items);
    }

    ViewScope scope = viewScopeForTeacher(requester);
    if (scope.adminSchool()) {
      long total = activityMapper.countAll(term, status);
      List<ActivityRecord> items = activityMapper.listAll(term, status, p.size(), p.offset());
      return new PageActivity(p.page(), p.size(), total, items);
    }
    if (!scope.includeCollege() && !scope.includeTutor()) {
      return new PageActivity(p.page(), p.size(), 0, List.of());
    }

    long total = activityMapper.countForTeacher(requester.userId(), scope.collegeId(), scope.includeTutor(), scope.includeCollege(), term, status);
    List<ActivityRecord> items =
        activityMapper.listForTeacher(requester.userId(), scope.collegeId(), scope.includeTutor(), scope.includeCollege(), term, status, p.size(), p.offset());
    return new PageActivity(p.page(), p.size(), total, items);
  }

  @Transactional
  public ActivityRecord create(Authentication authentication, ActivityCreateRequest req) {
    Requester requester = requireRequester(authentication);
    requireTeacher(requester);

    validateTerm(req.term());
    if (req.classId() <= 0) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "classId must be positive");
    }

    ClassRecord clazz = classMapper.findById(req.classId());
    if (clazz == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "class not found");
    }
    assertCanManageClass(requester, clazz);

    ActivityMapper.ActivityInsertParams p =
        new ActivityMapper.ActivityInsertParams(
            null,
            req.classId(),
            req.term(),
            req.title(),
            req.description(),
            req.capacity(),
            req.requiresReview(),
            requester.userId());
    activityMapper.insert(p);

    Long createdId = p.getId();
    if (createdId == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "activity not found");
    }
    ActivityRecord created = activityMapper.findById(createdId);
    if (created == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "activity not found");
    }
    return created;
  }

  public ActivityRecord get(Authentication authentication, long activityId) {
    Requester requester = requireRequester(authentication);
    ActivityRecord a = accessibleActivityOrNull(requester, activityId);
    if (a == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "activity not found");
    }
    return a;
  }

  @Transactional
  public void publish(Authentication authentication, long activityId) {
    Requester requester = requireRequester(authentication);
    requireTeacher(requester);

    ActivityRecord a = activityMapper.findById(activityId);
    if (a == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "activity not found");
    }

    ClassRecord clazz = classMapper.findById(a.classId());
    if (clazz == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "class not found");
    }
    assertCanManageClass(requester, clazz);

    int updated = activityMapper.publishDraft(activityId);
    if (updated == 0) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "invalid status transition");
    }
  }

  @Transactional
  public ActivitySignupRecord signup(Authentication authentication, long activityId, String note) {
    Requester requester = requireRequester(authentication);
    requireStudent(requester);

    ActivityRecord a = activityMapper.findById(activityId);
    if (a == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "activity not found");
    }
    if (a.status() != ActivityStatus.PUBLISHED) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "activity not published");
    }
    if (!classMapper.isStudentInClass(a.classId(), requester.userId())) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }

    ActivitySignupRecord existing = signupMapper.findByActivityIdAndUserId(activityId, requester.userId());
    if (existing != null && existing.status() != SignupStatus.CANCELED) {
      throw new ApiException(AuthErrorCode.CONFLICT, "conflict");
    }

    SignupStatus targetStatus = a.requiresReview() ? SignupStatus.APPLIED : SignupStatus.APPROVED;
    if (targetStatus == SignupStatus.APPROVED) {
      assertCapacityHasSlot(activityId, a.capacity());
    }

    if (existing != null) {
      signupMapper.resignFromCanceled(activityId, requester.userId(), targetStatus, note);
      ActivitySignupRecord updated = signupMapper.findByActivityIdAndUserId(activityId, requester.userId());
      if (updated == null) {
        throw new ApiException(AuthErrorCode.NOT_FOUND, "signup not found");
      }
      return updated;
    }

    ActivitySignupMapper.ActivitySignupInsertParams p =
        new ActivitySignupMapper.ActivitySignupInsertParams(null, activityId, requester.userId(), targetStatus, note);
    signupMapper.insert(p);
    Long createdId = p.getId();
    if (createdId == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "signup not found");
    }
    ActivitySignupRecord created = signupMapper.findByIdAndActivityId(createdId, activityId);
    if (created == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "signup not found");
    }
    return created;
  }

  public PageActivitySignup listSignups(Authentication authentication, long activityId, int page, int size, SignupStatus status) {
    Requester requester = requireRequester(authentication);
    requireTeacher(requester);
    PageSpec p = PageSpec.of(page, size);

    ActivityRecord a = activityMapper.findById(activityId);
    if (a == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "activity not found");
    }

    ClassRecord clazz = classMapper.findById(a.classId());
    if (clazz == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "class not found");
    }
    assertCanManageClass(requester, clazz);

    long total = signupMapper.countByActivity(activityId, status);
    List<ActivitySignupRecord> items = signupMapper.listByActivity(activityId, status, p.size(), p.offset());
    return new PageActivitySignup(p.page(), p.size(), total, items);
  }

  @Transactional
  public void cancelMySignup(Authentication authentication, long activityId) {
    Requester requester = requireRequester(authentication);
    requireStudent(requester);

    ActivitySignupRecord existing = signupMapper.findByActivityIdAndUserId(activityId, requester.userId());
    if (existing == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "signup not found");
    }
    signupMapper.cancel(activityId, requester.userId());
  }

  @Transactional
  public void approve(Authentication authentication, long activityId, long signupId) {
    Requester requester = requireRequester(authentication);
    requireTeacher(requester);

    ActivityRecord a = activityMapper.findById(activityId);
    if (a == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "activity not found");
    }

    ClassRecord clazz = classMapper.findById(a.classId());
    if (clazz == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "class not found");
    }
    assertCanManageClass(requester, clazz);

    int updated = signupMapper.approveAppliedWithCapacity(activityId, signupId, requester.userId());
    if (updated == 1) {
      return;
    }

    ActivitySignupRecord signup = signupMapper.findByIdAndActivityId(signupId, activityId);
    if (signup == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "signup not found");
    }
    if (signup.status() != SignupStatus.APPLIED) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "invalid status transition");
    }
    assertCapacityHasSlot(activityId, a.capacity());
    throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "invalid approve");
  }

  @Transactional
  public void reject(Authentication authentication, long activityId, long signupId, String reason) {
    Requester requester = requireRequester(authentication);
    requireTeacher(requester);

    ActivityRecord a = activityMapper.findById(activityId);
    if (a == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "activity not found");
    }

    ClassRecord clazz = classMapper.findById(a.classId());
    if (clazz == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "class not found");
    }
    assertCanManageClass(requester, clazz);

    int updated = signupMapper.rejectApplied(activityId, signupId, requester.userId(), reason);
    if (updated == 1) {
      return;
    }

    ActivitySignupRecord signup = signupMapper.findByIdAndActivityId(signupId, activityId);
    if (signup == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "signup not found");
    }
    if (signup.status() != SignupStatus.APPLIED) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "invalid status transition");
    }
    throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "invalid reject");
  }

  private ActivityRecord accessibleActivityOrNull(Requester requester, long activityId) {
    if (requester.user().userType() == UserType.STUDENT) {
      return activityMapper.findForStudentById(activityId, requester.userId());
    }

    ViewScope scope = viewScopeForTeacher(requester);
    if (scope.adminSchool()) {
      return activityMapper.findById(activityId);
    }
    if (!scope.includeTutor() && !scope.includeCollege()) {
      return null;
    }
    return activityMapper.findForTeacherById(activityId, requester.userId(), scope.collegeId(), scope.includeTutor(), scope.includeCollege());
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

  private void assertCanManageClass(Requester requester, ClassRecord clazz) {
    List<RoleCode> roles = requester.roles();
    boolean isAdminSchool = roles.contains(RoleCode.ADMIN_SCHOOL);
    boolean isAdminCollege = roles.contains(RoleCode.ADMIN_COLLEGE);
    boolean isTutor = roles.contains(RoleCode.TUTOR);

    if (isAdminSchool) {
      return;
    }
    if (isAdminCollege) {
      Long collegeId = requester.user().collegeId();
      if (collegeId != null && clazz.collegeId() == collegeId.longValue()) {
        return;
      }
    }
    if (isTutor && classMapper.isTutorOfClass(clazz.id(), requester.userId())) {
      return;
    }
    throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
  }

  private void assertCapacityHasSlot(long activityId, Integer capacity) {
    if (capacity == null) {
      return;
    }
    long approvedCount = signupMapper.countApproved(activityId);
    if (approvedCount >= capacity.longValue()) {
      throw new ApiException(AuthErrorCode.CONFLICT, "conflict");
    }
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

  private void requireTeacher(Requester requester) {
    if (requester.user().userType() != UserType.TEACHER) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }
  }

  private void requireStudent(Requester requester) {
    if (requester.user().userType() != UserType.STUDENT) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }
  }

  private static void validateTerm(String term) {
    if (term == null || !term.matches("\\d{4}-\\d{2}-\\d{2}-[12]")) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "term is invalid");
    }
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
