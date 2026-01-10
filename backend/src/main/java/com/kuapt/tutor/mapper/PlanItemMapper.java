package com.kuapt.tutor.mapper;

import com.kuapt.tutor.model.PlanItemRecord;
import com.kuapt.tutor.model.PlanItemStatus;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PlanItemMapper {
  @Select(
      "SELECT id, plan_id AS planId, title, status, sort_order AS sortOrder, due_date AS dueDate, created_at AS createdAt, updated_at AS updatedAt "
          + "FROM plan_item WHERE id = #{itemId} LIMIT 1")
  PlanItemRecord findById(@Param("itemId") long itemId);

  @Select(
      "SELECT id, plan_id AS planId, title, status, sort_order AS sortOrder, due_date AS dueDate, created_at AS createdAt, updated_at AS updatedAt "
          + "FROM plan_item WHERE id = #{itemId} AND plan_id = #{planId} LIMIT 1")
  PlanItemRecord findByIdAndPlanId(@Param("itemId") long itemId, @Param("planId") long planId);

  @Select(
      "SELECT id, plan_id AS planId, title, status, sort_order AS sortOrder, due_date AS dueDate, created_at AS createdAt, updated_at AS updatedAt "
          + "FROM plan_item WHERE plan_id = #{planId} ORDER BY sort_order ASC, id ASC")
  List<PlanItemRecord> listByPlanId(@Param("planId") long planId);

  @Select("SELECT COALESCE(MAX(sort_order), 0) + 1 FROM plan_item WHERE plan_id = #{planId}")
  int nextSortOrder(@Param("planId") long planId);

  @Insert("INSERT INTO plan_item(plan_id, title, sort_order, due_date) VALUES(#{planId}, #{title}, #{sortOrder}, #{dueDate})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(PlanItemInsertParams params);

  @Update(
      "<script>"
          + "UPDATE plan_item "
          + "<set>"
          + "  <if test='title != null'> title = #{title}, </if>"
          + "  <if test='status != null'> status = #{status}, </if>"
          + "  <if test='sortOrder != null'> sort_order = #{sortOrder}, </if>"
          + "  <if test='dueDate != null'> due_date = #{dueDate}, </if>"
          + "  updated_at = CURRENT_TIMESTAMP"
          + "</set>"
          + "WHERE id = #{itemId} AND plan_id = #{planId}"
          + "</script>")
  int update(
      @Param("planId") long planId,
      @Param("itemId") long itemId,
      @Param("title") String title,
      @Param("status") PlanItemStatus status,
      @Param("sortOrder") Integer sortOrder,
      @Param("dueDate") String dueDate);

  @Delete("DELETE FROM plan_item WHERE id = #{itemId} AND plan_id = #{planId}")
  int deleteByIdAndPlanId(@Param("planId") long planId, @Param("itemId") long itemId);

  final class PlanItemInsertParams {
    private Long id;
    private long planId;
    private String title;
    private int sortOrder;
    private String dueDate;

    public PlanItemInsertParams() {}

    public PlanItemInsertParams(Long id, long planId, String title, int sortOrder, String dueDate) {
      this.id = id;
      this.planId = planId;
      this.title = title;
      this.sortOrder = sortOrder;
      this.dueDate = dueDate;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public long planId() {
      return planId;
    }

    public String title() {
      return title;
    }

    public int sortOrder() {
      return sortOrder;
    }

    public String dueDate() {
      return dueDate;
    }
  }
}

