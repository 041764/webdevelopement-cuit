package com.kuapt.tutor.mapper;

import com.kuapt.tutor.model.ActivitySignupRecord;
import com.kuapt.tutor.model.SignupStatus;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivitySignupMapper {
  @Select(
      "SELECT id, activity_id AS activityId, user_id AS userId, status, created_at AS createdAt, reviewed_at AS reviewedAt, reviewed_by_user_id AS reviewedByUserId "
          + "FROM activity_signup WHERE activity_id = #{activityId} AND user_id = #{userId} LIMIT 1")
  ActivitySignupRecord findByActivityIdAndUserId(@Param("activityId") long activityId, @Param("userId") long userId);

  @Select(
      "SELECT id, activity_id AS activityId, user_id AS userId, status, created_at AS createdAt, reviewed_at AS reviewedAt, reviewed_by_user_id AS reviewedByUserId "
          + "FROM activity_signup WHERE id = #{signupId} AND activity_id = #{activityId} LIMIT 1")
  ActivitySignupRecord findByIdAndActivityId(@Param("signupId") long signupId, @Param("activityId") long activityId);

  @Select(
      "<script>"
          + "SELECT COUNT(1) FROM activity_signup "
          + "WHERE activity_id = #{activityId} "
          + "<if test='status != null'> AND status = #{status} </if>"
          + "</script>")
  long countByActivity(@Param("activityId") long activityId, @Param("status") SignupStatus status);

  @Select(
      "<script>"
          + "SELECT id, activity_id AS activityId, user_id AS userId, status, created_at AS createdAt, reviewed_at AS reviewedAt, reviewed_by_user_id AS reviewedByUserId "
          + "FROM activity_signup "
          + "WHERE activity_id = #{activityId} "
          + "<if test='status != null'> AND status = #{status} </if>"
          + "ORDER BY created_at DESC, id DESC "
          + "LIMIT #{limit} OFFSET #{offset}"
          + "</script>")
  List<ActivitySignupRecord> listByActivity(
      @Param("activityId") long activityId,
      @Param("status") SignupStatus status,
      @Param("limit") int limit,
      @Param("offset") int offset);

  @Select("SELECT COUNT(1) FROM activity_signup WHERE activity_id = #{activityId} AND status = 'APPROVED'")
  long countApproved(@Param("activityId") long activityId);

  @Update(
      "UPDATE activity_signup SET status = 'CANCELED' "
          + "WHERE activity_id = #{activityId} AND user_id = #{userId}")
  int cancel(@Param("activityId") long activityId, @Param("userId") long userId);

  @Update(
      "UPDATE activity_signup SET status = #{status}, note = #{note}, created_at = CURRENT_TIMESTAMP, reviewed_at = NULL, reviewed_by_user_id = NULL "
          + "WHERE activity_id = #{activityId} AND user_id = #{userId} AND status = 'CANCELED'")
  int resignFromCanceled(
      @Param("activityId") long activityId,
      @Param("userId") long userId,
      @Param("status") SignupStatus status,
      @Param("note") String note);

  @Update(
      "UPDATE activity_signup "
          + "SET status = 'APPROVED', reviewed_at = CURRENT_TIMESTAMP, reviewed_by_user_id = #{reviewedByUserId} "
          + "WHERE id = #{signupId} "
          + "  AND activity_id = #{activityId} "
          + "  AND status = 'APPLIED' "
          + "  AND ("
          + "    (SELECT capacity FROM activity WHERE id = #{activityId}) IS NULL "
          + "    OR (SELECT COUNT(1) FROM activity_signup WHERE activity_id = #{activityId} AND status = 'APPROVED')"
          + "       < (SELECT capacity FROM activity WHERE id = #{activityId})"
          + "  )")
  int approveAppliedWithCapacity(
      @Param("activityId") long activityId, @Param("signupId") long signupId, @Param("reviewedByUserId") long reviewedByUserId);

  @Update(
      "UPDATE activity_signup "
          + "SET status = 'REJECTED', reviewed_at = CURRENT_TIMESTAMP, reviewed_by_user_id = #{reviewedByUserId}, note = COALESCE(#{reason}, note) "
          + "WHERE id = #{signupId} AND activity_id = #{activityId} AND status = 'APPLIED'")
  int rejectApplied(
      @Param("activityId") long activityId,
      @Param("signupId") long signupId,
      @Param("reviewedByUserId") long reviewedByUserId,
      @Param("reason") String reason);

  @Insert(
      "INSERT INTO activity_signup(activity_id, user_id, status, note, created_at, reviewed_at, reviewed_by_user_id) "
          + "VALUES(#{activityId}, #{userId}, #{status}, #{note}, CURRENT_TIMESTAMP, NULL, NULL)")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(ActivitySignupInsertParams params);

  final class ActivitySignupInsertParams {
    private Long id;
    private long activityId;
    private long userId;
    private SignupStatus status;
    private String note;

    public ActivitySignupInsertParams() {}

    public ActivitySignupInsertParams(Long id, long activityId, long userId, SignupStatus status, String note) {
      this.id = id;
      this.activityId = activityId;
      this.userId = userId;
      this.status = status;
      this.note = note;
    }

    public Long id() {
      return id;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public long activityId() {
      return activityId;
    }

    public long getActivityId() {
      return activityId;
    }

    public long userId() {
      return userId;
    }

    public long getUserId() {
      return userId;
    }

    public SignupStatus status() {
      return status;
    }

    public SignupStatus getStatus() {
      return status;
    }

    public String note() {
      return note;
    }

    public String getNote() {
      return note;
    }
  }
}
