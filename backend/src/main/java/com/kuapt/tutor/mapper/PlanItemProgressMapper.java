package com.kuapt.tutor.mapper;

import com.kuapt.tutor.model.PlanItemProgressRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PlanItemProgressMapper {
  @Select(
      "SELECT id, plan_item_id AS planItemId, percent, note, created_by_user_id AS createdByUserId, created_at AS createdAt "
          + "FROM plan_item_progress WHERE plan_item_id = #{itemId} ORDER BY created_at ASC, id ASC")
  List<PlanItemProgressRecord> listByItemId(@Param("itemId") long itemId);

  @Select(
      "SELECT id, plan_item_id AS planItemId, percent, note, created_by_user_id AS createdByUserId, created_at AS createdAt "
          + "FROM plan_item_progress WHERE id = #{progressId} LIMIT 1")
  PlanItemProgressRecord findById(@Param("progressId") long progressId);

  @Insert(
      "INSERT INTO plan_item_progress(plan_item_id, percent, note, created_by_user_id) "
          + "VALUES(#{planItemId}, #{percent}, #{note}, #{createdByUserId})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(PlanItemProgressInsertParams params);

  final class PlanItemProgressInsertParams {
    private Long id;
    private long planItemId;
    private int percent;
    private String note;
    private long createdByUserId;

    public PlanItemProgressInsertParams() {}

    public PlanItemProgressInsertParams(Long id, long planItemId, int percent, String note, long createdByUserId) {
      this.id = id;
      this.planItemId = planItemId;
      this.percent = percent;
      this.note = note;
      this.createdByUserId = createdByUserId;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public long planItemId() {
      return planItemId;
    }

    public int percent() {
      return percent;
    }

    public String note() {
      return note;
    }

    public long createdByUserId() {
      return createdByUserId;
    }
  }
}

