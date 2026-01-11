package com.kuapt.tutor.plan;

import com.kuapt.tutor.auth.AuthErrorCode;
import com.kuapt.tutor.exception.ApiException;
import com.kuapt.tutor.mapper.ClassMapper;
import com.kuapt.tutor.mapper.PlanItemMapper;
import com.kuapt.tutor.mapper.PlanItemProgressMapper;
import com.kuapt.tutor.mapper.PlanMapper;
import com.kuapt.tutor.model.ClassRecord;
import com.kuapt.tutor.service.PageSpec;
import com.kuapt.tutor.service.Requester;
import com.kuapt.tutor.service.ServiceAuth;
import com.kuapt.tutor.service.TermUtil;
import com.kuapt.tutor.service.ViewScope;
import com.kuapt.tutor.model.PlanItemProgressRecord;
import com.kuapt.tutor.model.PlanItemRecord;
import com.kuapt.tutor.model.PlanItemStatus;
import com.kuapt.tutor.model.PlanOwnerType;
import com.kuapt.tutor.model.PlanRecord;
import com.kuapt.tutor.model.RoleCode;
import com.kuapt.tutor.model.UserRecord;
import com.kuapt.tutor.model.UserType;
import com.kuapt.tutor.plan.dto.PagePlan;
import com.kuapt.tutor.plan.dto.PlanCreateRequest;
import com.kuapt.tutor.plan.dto.PlanDetail;
import com.kuapt.tutor.plan.dto.PlanItemCreateRequest;
import com.kuapt.tutor.plan.dto.PlanItemProgressCreateRequest;
import com.kuapt.tutor.plan.dto.PlanItemUpdateRequest;
import com.kuapt.tutor.plan.dto.PlanProgress;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanService {
  private final PlanMapper planMapper;
  private final PlanItemMapper planItemMapper;
  private final PlanItemProgressMapper progressMapper;
  private final ClassMapper classMapper;
  private final ServiceAuth serviceAuth;

  public PlanService(
      PlanMapper planMapper,
      PlanItemMapper planItemMapper,
      PlanItemProgressMapper progressMapper,
      ClassMapper classMapper,
      ServiceAuth serviceAuth) {
    this.planMapper = planMapper;
    this.planItemMapper = planItemMapper;
    this.progressMapper = progressMapper;
    this.classMapper = classMapper;
    this.serviceAuth = serviceAuth;
  }

  public PagePlan list(Authentication authentication, int page, int size, String term) {
    Requester requester = serviceAuth.requireRequester(authentication);
    PageSpec p = PageSpec.of(page, size);
    String normalizedTerm = TermUtil.normalizeOptionalTerm(term);

    if (requester.user().userType() == UserType.STUDENT) {
      long total = planMapper.countAccessibleForStudent(requester.userId(), normalizedTerm);
      List<PlanRecord> items = planMapper.listAccessibleForStudent(requester.userId(), normalizedTerm, p.size(), p.offset());
      return new PagePlan(p.page(), p.size(), total, items);
    }

    ViewScope scope = serviceAuth.viewScopeForTeacher(requester);
    if (scope.adminSchool()) {
      long total = planMapper.countAccessibleForAdminSchool(requester.userId(), normalizedTerm);
      List<PlanRecord> items = planMapper.listAccessibleForAdminSchool(requester.userId(), normalizedTerm, p.size(), p.offset());
      return new PagePlan(p.page(), p.size(), total, items);
    }

    long total = planMapper.countAccessibleForTeacher(requester.userId(), scope.collegeId(), scope.includeTutor(), scope.includeCollege(), normalizedTerm);
    List<PlanRecord> items =
        planMapper.listAccessibleForTeacher(
            requester.userId(), scope.collegeId(), scope.includeTutor(), scope.includeCollege(), normalizedTerm, p.size(), p.offset());
    return new PagePlan(p.page(), p.size(), total, items);
  }

  @Transactional
  public PlanRecord create(Authentication authentication, PlanCreateRequest req) {
    Requester requester = serviceAuth.requireRequester(authentication);
    TermUtil.validateTerm(req.term());

    if (req.ownerType() == PlanOwnerType.USER) {
      PlanMapper.PlanInsertParams p = new PlanMapper.PlanInsertParams(null, PlanOwnerType.USER, requester.userId(), null, req.term(), req.title());
      planMapper.insert(p);
      return requirePlanById(p.getId(), "plan not found");
    }

    if (requester.user().userType() != UserType.TEACHER) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }
    Long classId = req.ownerClassId();
    if (classId == null || classId.longValue() <= 0) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "ownerClassId is invalid");
    }
    ClassRecord clazz = classMapper.findById(classId);
    if (clazz == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "class not found");
    }
    assertCanManageClass(requester, clazz);

    PlanMapper.PlanInsertParams p = new PlanMapper.PlanInsertParams(null, PlanOwnerType.CLASS, null, classId, req.term(), req.title());
    planMapper.insert(p);
    return requirePlanById(p.getId(), "plan not found");
  }

  public PlanDetail get(Authentication authentication, long planId) {
    Requester requester = serviceAuth.requireRequester(authentication);
    PlanRecord plan = planMapper.findById(planId);
    if (plan == null || !canViewPlan(requester, plan)) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "plan not found");
    }

    List<PlanItemRecord> items = planItemMapper.listByPlanId(planId);
    PlanProgress progress = computeProgress(planId, items);
    return new PlanDetail(
        plan.id(), plan.ownerType(), plan.ownerUserId(), plan.ownerClassId(), plan.term(), plan.title(), plan.createdAt(), items, progress);
  }

  @Transactional
  public PlanItemRecord addItem(Authentication authentication, long planId, PlanItemCreateRequest req) {
    Requester requester = serviceAuth.requireRequester(authentication);
    PlanRecord plan = planMapper.findById(planId);
    if (plan == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "plan not found");
    }
    if (!canModifyPlan(requester, plan)) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }

    int sortOrder = planItemMapper.nextSortOrder(planId);
    PlanItemMapper.PlanItemInsertParams p = new PlanItemMapper.PlanItemInsertParams(null, planId, req.title(), sortOrder, req.dueDate());
    planItemMapper.insert(p);
    Long createdId = p.getId();
    if (createdId == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "plan item not found");
    }
    PlanItemRecord created = planItemMapper.findByIdAndPlanId(createdId, planId);
    if (created == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "plan item not found");
    }
    return created;
  }

  @Transactional
  public void updateItem(Authentication authentication, long planId, long itemId, PlanItemUpdateRequest req) {
    Requester requester = serviceAuth.requireRequester(authentication);
    PlanRecord plan = planMapper.findById(planId);
    if (plan == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "plan not found");
    }
    if (!canModifyPlan(requester, plan)) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }

    int updated = planItemMapper.update(planId, itemId, req.title(), req.status(), req.sortOrder(), req.dueDate());
    if (updated == 1) {
      return;
    }
    throw new ApiException(AuthErrorCode.NOT_FOUND, "plan item not found");
  }

  @Transactional
  public void deleteItem(Authentication authentication, long planId, long itemId) {
    Requester requester = serviceAuth.requireRequester(authentication);
    PlanRecord plan = planMapper.findById(planId);
    if (plan == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "plan not found");
    }
    if (!canModifyPlan(requester, plan)) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }

    int deleted = planItemMapper.deleteByIdAndPlanId(planId, itemId);
    if (deleted == 1) {
      return;
    }
    throw new ApiException(AuthErrorCode.NOT_FOUND, "plan item not found");
  }

  public List<PlanItemProgressRecord> listProgress(Authentication authentication, long itemId) {
    Requester requester = serviceAuth.requireRequester(authentication);
    PlanItemRecord item = planItemMapper.findById(itemId);
    if (item == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "plan item not found");
    }
    PlanRecord plan = planMapper.findById(item.planId());
    if (plan == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "plan not found");
    }
    if (!canViewPlanProgress(requester, plan)) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }
    return progressMapper.listByItemId(itemId);
  }

  @Transactional
  public PlanItemProgressRecord addProgress(Authentication authentication, long itemId, PlanItemProgressCreateRequest req) {
    Requester requester = serviceAuth.requireRequester(authentication);
    PlanItemRecord item = planItemMapper.findById(itemId);
    if (item == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "plan item not found");
    }
    PlanRecord plan = planMapper.findById(item.planId());
    if (plan == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "plan not found");
    }
    if (!canAppendProgress(requester, plan)) {
      throw new ApiException(AuthErrorCode.AUTH_FORBIDDEN, "forbidden");
    }

    PlanItemProgressMapper.PlanItemProgressInsertParams p =
        new PlanItemProgressMapper.PlanItemProgressInsertParams(null, itemId, req.percent(), req.note(), requester.userId());
    progressMapper.insert(p);

    Long createdId = p.getId();
    if (createdId == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "plan item progress not found");
    }
    PlanItemProgressRecord created = progressMapper.findById(createdId);
    if (created == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, "plan item progress not found");
    }
    return created;
  }

  private static PlanProgress computeProgress(long planId, List<PlanItemRecord> items) {
    int total = items.size();
    int done = 0;
    for (PlanItemRecord item : items) {
      if (item.status() == PlanItemStatus.done) {
        done++;
      }
    }
    float rate = total == 0 ? 0f : (float) done / (float) total;
    return new PlanProgress(planId, done, total, rate, Instant.now().toString());
  }

  private PlanRecord requirePlanById(Long planId, String notFoundMessage) {
    if (planId == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, notFoundMessage);
    }
    PlanRecord created = planMapper.findById(planId);
    if (created == null) {
      throw new ApiException(AuthErrorCode.NOT_FOUND, notFoundMessage);
    }
    return created;
  }

  private boolean canViewPlan(Requester requester, PlanRecord plan) {
    if (plan.ownerType() == PlanOwnerType.USER) {
      return plan.ownerUserId() != null && plan.ownerUserId().longValue() == requester.userId();
    }
    Long classId = plan.ownerClassId();
    return classId != null && canViewClassPlan(requester, classId.longValue());
  }

  private boolean canModifyPlan(Requester requester, PlanRecord plan) {
    if (plan.ownerType() == PlanOwnerType.USER) {
      return plan.ownerUserId() != null && plan.ownerUserId().longValue() == requester.userId();
    }
    Long classId = plan.ownerClassId();
    if (classId == null) {
      return false;
    }
    ClassRecord clazz = classMapper.findById(classId.longValue());
    if (clazz == null) {
      return false;
    }
    List<RoleCode> roles = requester.roles();
    boolean isAdminSchool = roles.contains(RoleCode.ADMIN_SCHOOL);
    boolean isAdminCollege = roles.contains(RoleCode.ADMIN_COLLEGE);
    boolean isTutor = roles.contains(RoleCode.TUTOR);
    if (isAdminSchool) {
      return true;
    }
    if (isAdminCollege) {
      Long collegeId = requester.user().collegeId();
      if (collegeId != null && clazz.collegeId() == collegeId.longValue()) {
        return true;
      }
    }
    return isTutor && classMapper.isTutorOfClass(clazz.id(), requester.userId());
  }

  private boolean canViewPlanProgress(Requester requester, PlanRecord plan) {
    // /plan-items/{itemId}/progress returns 403 for unauthorized (openapi has 403), unlike /plans/{planId} which hides with 404.
    return canViewPlan(requester, plan);
  }

  private boolean canAppendProgress(Requester requester, PlanRecord plan) {
    if (plan.ownerType() == PlanOwnerType.USER) {
      return plan.ownerUserId() != null && plan.ownerUserId().longValue() == requester.userId();
    }
    Long classId = plan.ownerClassId();
    return classId != null && canViewClassPlan(requester, classId.longValue());
  }

  private boolean canViewClassPlan(Requester requester, long classId) {
    List<RoleCode> roles = requester.roles();
    boolean isAdminSchool = roles.contains(RoleCode.ADMIN_SCHOOL);
    boolean isAdminCollege = roles.contains(RoleCode.ADMIN_COLLEGE);
    boolean isTutor = roles.contains(RoleCode.TUTOR);

    if (isAdminSchool) {
      return true;
    }

    ClassRecord clazz = classMapper.findById(classId);
    if (clazz == null) {
      return false;
    }

    if (isAdminCollege) {
      Long collegeId = requester.user().collegeId();
      if (collegeId != null && clazz.collegeId() == collegeId.longValue()) {
        return true;
      }
    }
    if (isTutor && classMapper.isTutorOfClass(clazz.id(), requester.userId())) {
      return true;
    }
    return requester.user().userType() == UserType.STUDENT && classMapper.isStudentInClass(clazz.id(), requester.userId());
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

}
