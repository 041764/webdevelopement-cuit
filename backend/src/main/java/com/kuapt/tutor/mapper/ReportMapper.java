package com.kuapt.tutor.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportMapper {
  record PlanCompletionRow(String scope, long doneCount, long totalCount, float completionRate) {}

  @Select(
      "SELECT (\"学院:\" || c.name) AS scope, "
          + "SUM(CASE WHEN pi.status = 'done' THEN 1 ELSE 0 END) AS doneCount, "
          + "COUNT(1) AS totalCount, "
          + "CASE WHEN COUNT(1) = 0 THEN 0.0 ELSE (1.0 * SUM(CASE WHEN pi.status = 'done' THEN 1 ELSE 0 END) / COUNT(1)) END AS completionRate "
          + "FROM plan_item pi "
          + "JOIN plan p ON p.id = pi.plan_id "
          + "LEFT JOIN \"class\" clz ON (p.owner_type = 'CLASS' AND p.owner_class_id = clz.id) "
          + "LEFT JOIN \"user\" u ON (p.owner_type = 'USER' AND p.owner_user_id = u.id) "
          + "JOIN college c ON c.id = COALESCE(clz.college_id, u.college_id) "
          + "WHERE p.term = #{term} AND (#{collegeId} IS NULL OR c.id = #{collegeId}) "
          + "GROUP BY c.id, c.name "
          + "ORDER BY c.name ASC")
  List<PlanCompletionRow> planCompletionByCollege(@Param("term") String term, @Param("collegeId") Long collegeId);

  @Select(
      "SELECT (\"班级:\" || clz.name) AS scope, "
          + "SUM(CASE WHEN pi.status = 'done' THEN 1 ELSE 0 END) AS doneCount, "
          + "COUNT(1) AS totalCount, "
          + "CASE WHEN COUNT(1) = 0 THEN 0.0 ELSE (1.0 * SUM(CASE WHEN pi.status = 'done' THEN 1 ELSE 0 END) / COUNT(1)) END AS completionRate "
          + "FROM plan_item pi "
          + "JOIN plan p ON p.id = pi.plan_id "
          + "JOIN \"class\" clz ON p.owner_type = 'CLASS' AND p.owner_class_id = clz.id "
          + "JOIN class_tutor ct ON ct.class_id = clz.id AND ct.tutor_user_id = #{tutorUserId} "
          + "WHERE p.term = #{term} "
          + "GROUP BY clz.id, clz.name "
          + "ORDER BY clz.name ASC")
  List<PlanCompletionRow> planCompletionByTutorClasses(@Param("tutorUserId") long tutorUserId, @Param("term") String term);

  record ActivityStatsRow(long activityId, String title, long appliedCount, long approvedCount) {}

  @Select(
      "SELECT a.id AS activityId, a.title AS title, "
          + "SUM(CASE WHEN s.status = 'APPLIED' THEN 1 ELSE 0 END) AS appliedCount, "
          + "SUM(CASE WHEN s.status = 'APPROVED' THEN 1 ELSE 0 END) AS approvedCount "
          + "FROM activity a "
          + "LEFT JOIN activity_signup s ON s.activity_id = a.id "
          + "WHERE a.term = #{term} "
          + "GROUP BY a.id, a.title "
          + "ORDER BY a.id ASC")
  List<ActivityStatsRow> activityStatsAll(@Param("term") String term);

  @Select(
      "SELECT a.id AS activityId, a.title AS title, "
          + "SUM(CASE WHEN s.status = 'APPLIED' THEN 1 ELSE 0 END) AS appliedCount, "
          + "SUM(CASE WHEN s.status = 'APPROVED' THEN 1 ELSE 0 END) AS approvedCount "
          + "FROM activity a "
          + "JOIN \"class\" clz ON clz.id = a.class_id "
          + "LEFT JOIN activity_signup s ON s.activity_id = a.id "
          + "WHERE a.term = #{term} AND clz.college_id = #{collegeId} "
          + "GROUP BY a.id, a.title "
          + "ORDER BY a.id ASC")
  List<ActivityStatsRow> activityStatsByCollege(@Param("term") String term, @Param("collegeId") long collegeId);

  @Select(
      "SELECT a.id AS activityId, a.title AS title, "
          + "SUM(CASE WHEN s.status = 'APPLIED' THEN 1 ELSE 0 END) AS appliedCount, "
          + "SUM(CASE WHEN s.status = 'APPROVED' THEN 1 ELSE 0 END) AS approvedCount "
          + "FROM activity a "
          + "JOIN class_tutor ct ON ct.class_id = a.class_id AND ct.tutor_user_id = #{tutorUserId} "
          + "LEFT JOIN activity_signup s ON s.activity_id = a.id "
          + "WHERE a.term = #{term} "
          + "GROUP BY a.id, a.title "
          + "ORDER BY a.id ASC")
  List<ActivityStatsRow> activityStatsByTutorClasses(@Param("term") String term, @Param("tutorUserId") long tutorUserId);
}
