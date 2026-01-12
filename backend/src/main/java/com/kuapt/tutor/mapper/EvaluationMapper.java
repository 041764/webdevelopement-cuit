package com.kuapt.tutor.mapper;

import com.kuapt.tutor.model.EvaluationRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EvaluationMapper {
  @Select(
      "SELECT e.id, e.evaluator_user_id AS evaluatorUserId, u1.user_no AS evaluatorUserNo, u1.name AS evaluatorUserName, "
          + "e.evaluatee_user_id AS evaluateeUserId, u2.user_no AS evaluateeUserNo, u2.name AS evaluateeUserName, "
          + "e.term, e.score_total AS scoreTotal, e.comment, e.created_at AS createdAt "
          + "FROM evaluation e "
          + "JOIN \"user\" u1 ON u1.id = e.evaluator_user_id "
          + "JOIN \"user\" u2 ON u2.id = e.evaluatee_user_id "
          + "WHERE e.id = #{id} LIMIT 1")
  EvaluationRecord findById(@Param("id") long id);

  @Select(
      "SELECT e.id, e.evaluator_user_id AS evaluatorUserId, u1.user_no AS evaluatorUserNo, u1.name AS evaluatorUserName, "
          + "e.evaluatee_user_id AS evaluateeUserId, u2.user_no AS evaluateeUserNo, u2.name AS evaluateeUserName, "
          + "e.term, e.score_total AS scoreTotal, e.comment, e.created_at AS createdAt "
          + "FROM evaluation e "
          + "JOIN \"user\" u1 ON u1.id = e.evaluator_user_id "
          + "JOIN \"user\" u2 ON u2.id = e.evaluatee_user_id "
          + "WHERE e.evaluatee_user_id = #{userId} "
          + "AND (#{term} IS NULL OR e.term = #{term}) "
          + "ORDER BY e.id DESC LIMIT #{limit} OFFSET #{offset}")
  List<EvaluationRecord> listForStudent(
      @Param("userId") long userId, @Param("term") String term, @Param("limit") int limit, @Param("offset") int offset);

  @Select(
      "SELECT COUNT(1) FROM evaluation WHERE evaluatee_user_id = #{userId} AND (#{term} IS NULL OR term = #{term})")
  long countForStudent(@Param("userId") long userId, @Param("term") String term);

  @Select(
      "SELECT e.id, e.evaluator_user_id AS evaluatorUserId, u1.user_no AS evaluatorUserNo, u1.name AS evaluatorUserName, "
          + "e.evaluatee_user_id AS evaluateeUserId, u2.user_no AS evaluateeUserNo, u2.name AS evaluateeUserName, "
          + "e.term, e.score_total AS scoreTotal, e.comment, e.created_at AS createdAt "
          + "FROM evaluation e "
          + "JOIN \"user\" u1 ON u1.id = e.evaluator_user_id "
          + "JOIN \"user\" u2 ON u2.id = e.evaluatee_user_id "
          + "WHERE u2.college_id = #{collegeId} AND (#{term} IS NULL OR e.term = #{term}) "
          + "ORDER BY e.id DESC LIMIT #{limit} OFFSET #{offset}")
  List<EvaluationRecord> listForCollege(
      @Param("collegeId") long collegeId, @Param("term") String term, @Param("limit") int limit, @Param("offset") int offset);

  @Select(
      "SELECT COUNT(1) FROM evaluation e JOIN \"user\" stu ON stu.id = e.evaluatee_user_id "
          + "WHERE stu.college_id = #{collegeId} AND (#{term} IS NULL OR e.term = #{term})")
  long countForCollege(@Param("collegeId") long collegeId, @Param("term") String term);

  @Select(
      "SELECT e.id, e.evaluator_user_id AS evaluatorUserId, u1.user_no AS evaluatorUserNo, u1.name AS evaluatorUserName, "
          + "e.evaluatee_user_id AS evaluateeUserId, u2.user_no AS evaluateeUserNo, u2.name AS evaluateeUserName, "
          + "e.term, e.score_total AS scoreTotal, e.comment, e.created_at AS createdAt "
          + "FROM evaluation e "
          + "JOIN \"user\" u1 ON u1.id = e.evaluator_user_id "
          + "JOIN \"user\" u2 ON u2.id = e.evaluatee_user_id "
          + "WHERE EXISTS (" +
          "  SELECT 1 FROM class_student cs " +
          "  JOIN class_tutor ct ON ct.class_id = cs.class_id " +
          "  WHERE cs.student_user_id = e.evaluatee_user_id AND ct.tutor_user_id = #{tutorUserId}" +
          ") "
          + "AND (#{term} IS NULL OR e.term = #{term}) "
          + "ORDER BY e.id DESC LIMIT #{limit} OFFSET #{offset}")
  List<EvaluationRecord> listForTutor(
      @Param("tutorUserId") long tutorUserId, @Param("term") String term, @Param("limit") int limit, @Param("offset") int offset);

  @Select(
      "SELECT COUNT(1) FROM evaluation e "
          + "WHERE EXISTS (" +
          "  SELECT 1 FROM class_student cs " +
          "  JOIN class_tutor ct ON ct.class_id = cs.class_id " +
          "  WHERE cs.student_user_id = e.evaluatee_user_id AND ct.tutor_user_id = #{tutorUserId}" +
          ") "
          + "AND (#{term} IS NULL OR e.term = #{term})")
  long countForTutor(@Param("tutorUserId") long tutorUserId, @Param("term") String term);

  @Select(
      "SELECT e.id, e.evaluator_user_id AS evaluatorUserId, u1.user_no AS evaluatorUserNo, u1.name AS evaluatorUserName, "
          + "e.evaluatee_user_id AS evaluateeUserId, u2.user_no AS evaluateeUserNo, u2.name AS evaluateeUserName, "
          + "e.term, e.score_total AS scoreTotal, e.comment, e.created_at AS createdAt "
          + "FROM evaluation e "
          + "JOIN \"user\" u1 ON u1.id = e.evaluator_user_id "
          + "JOIN \"user\" u2 ON u2.id = e.evaluatee_user_id "
          + "WHERE (#{term} IS NULL OR e.term = #{term}) ORDER BY e.id DESC LIMIT #{limit} OFFSET #{offset}")
  List<EvaluationRecord> listAll(@Param("term") String term, @Param("limit") int limit, @Param("offset") int offset);

  @Select("SELECT COUNT(1) FROM evaluation WHERE (#{term} IS NULL OR term = #{term})")
  long countAll(@Param("term") String term);

  class EvaluationInsertParams {
    private Long id;
    private final long evaluatorUserId;
    private final long evaluateeUserId;
    private final String term;
    private final int scoreTotal;
    private final String comment;

    public EvaluationInsertParams(Long id, long evaluatorUserId, long evaluateeUserId, String term, int scoreTotal, String comment) {
      this.id = id;
      this.evaluatorUserId = evaluatorUserId;
      this.evaluateeUserId = evaluateeUserId;
      this.term = term;
      this.scoreTotal = scoreTotal;
      this.comment = comment;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public long getEvaluatorUserId() {
      return evaluatorUserId;
    }

    public long getEvaluateeUserId() {
      return evaluateeUserId;
    }

    public String getTerm() {
      return term;
    }

    public int getScoreTotal() {
      return scoreTotal;
    }

    public String getComment() {
      return comment;
    }
  }

  @Insert(
      "INSERT INTO evaluation(evaluator_user_id, evaluatee_user_id, term, score_total, comment) "
          + "VALUES(#{evaluatorUserId}, #{evaluateeUserId}, #{term}, #{scoreTotal}, #{comment})")
  @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(EvaluationInsertParams p);
}
