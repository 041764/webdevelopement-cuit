package com.kuapt.tutor.mapper;

import com.kuapt.tutor.model.PlanOwnerType;
import com.kuapt.tutor.model.PlanRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PlanMapper {
  @Select(
      "SELECT id, owner_type AS ownerType, owner_user_id AS ownerUserId, owner_class_id AS ownerClassId, term, title, created_at AS createdAt "
          + "FROM plan WHERE id = #{planId} LIMIT 1")
  PlanRecord findById(@Param("planId") long planId);

  @Select(
      "<script>"
          + "SELECT COUNT(1) FROM ("
          + "  SELECT p.id "
          + "  FROM plan p "
          + "  WHERE p.owner_type = 'USER' AND p.owner_user_id = #{userId} "
          + "  <if test='term != null and term != \"\"'> AND p.term = #{term} </if>"
          + "  UNION ALL "
          + "  SELECT p.id "
          + "  FROM plan p "
          + "  JOIN class_student cs ON cs.class_id = p.owner_class_id AND cs.student_user_id = #{userId} "
          + "  WHERE p.owner_type = 'CLASS' "
          + "  <if test='term != null and term != \"\"'> AND p.term = #{term} </if>"
          + ") t"
          + "</script>")
  long countAccessibleForStudent(@Param("userId") long userId, @Param("term") String term);

  @Select(
      "<script>"
          + "SELECT id, owner_type AS ownerType, owner_user_id AS ownerUserId, owner_class_id AS ownerClassId, term, title, created_at AS createdAt "
          + "FROM ("
          + "  SELECT p.id, p.owner_type, p.owner_user_id, p.owner_class_id, p.term, p.title, p.created_at "
          + "  FROM plan p "
          + "  WHERE p.owner_type = 'USER' AND p.owner_user_id = #{userId} "
          + "  <if test='term != null and term != \"\"'> AND p.term = #{term} </if>"
          + "  UNION ALL "
          + "  SELECT p.id, p.owner_type, p.owner_user_id, p.owner_class_id, p.term, p.title, p.created_at "
          + "  FROM plan p "
          + "  JOIN class_student cs ON cs.class_id = p.owner_class_id AND cs.student_user_id = #{userId} "
          + "  WHERE p.owner_type = 'CLASS' "
          + "  <if test='term != null and term != \"\"'> AND p.term = #{term} </if>"
          + ") t "
          + "ORDER BY created_at DESC, id DESC "
          + "LIMIT #{limit} OFFSET #{offset}"
          + "</script>")
  List<PlanRecord> listAccessibleForStudent(
      @Param("userId") long userId, @Param("term") String term, @Param("limit") int limit, @Param("offset") int offset);

  @Select(
      "<script>"
          + "SELECT COUNT(1) FROM ("
          + "  SELECT p.id "
          + "  FROM plan p "
          + "  WHERE p.owner_type = 'USER' AND p.owner_user_id = #{userId} "
          + "  <if test='term != null and term != \"\"'> AND p.term = #{term} </if>"
          + "  UNION ALL "
          + "  SELECT p.id "
          + "  FROM plan p "
          + "  WHERE p.owner_type = 'CLASS' "
          + "  <if test='term != null and term != \"\"'> AND p.term = #{term} </if>"
          + ") t"
          + "</script>")
  long countAccessibleForAdminSchool(@Param("userId") long userId, @Param("term") String term);

  @Select(
      "<script>"
          + "SELECT id, owner_type AS ownerType, owner_user_id AS ownerUserId, owner_class_id AS ownerClassId, term, title, created_at AS createdAt "
          + "FROM ("
          + "  SELECT p.id, p.owner_type, p.owner_user_id, p.owner_class_id, p.term, p.title, p.created_at "
          + "  FROM plan p "
          + "  WHERE p.owner_type = 'USER' AND p.owner_user_id = #{userId} "
          + "  <if test='term != null and term != \"\"'> AND p.term = #{term} </if>"
          + "  UNION ALL "
          + "  SELECT p.id, p.owner_type, p.owner_user_id, p.owner_class_id, p.term, p.title, p.created_at "
          + "  FROM plan p "
          + "  WHERE p.owner_type = 'CLASS' "
          + "  <if test='term != null and term != \"\"'> AND p.term = #{term} </if>"
          + ") t "
          + "ORDER BY created_at DESC, id DESC "
          + "LIMIT #{limit} OFFSET #{offset}"
          + "</script>")
  List<PlanRecord> listAccessibleForAdminSchool(
      @Param("userId") long userId, @Param("term") String term, @Param("limit") int limit, @Param("offset") int offset);

  @Select(
      "<script>"
          + "SELECT COUNT(1) FROM ("
          + "  SELECT p.id "
          + "  FROM plan p "
          + "  WHERE p.owner_type = 'USER' AND p.owner_user_id = #{userId} "
          + "  <if test='term != null and term != \"\"'> AND p.term = #{term} </if>"
          + "  UNION ALL "
          + "  SELECT p.id "
          + "  FROM plan p "
          + "  JOIN \"class\" c ON c.id = p.owner_class_id "
          + "  WHERE p.owner_type = 'CLASS' "
          + "  <if test='includeTutor == false and includeCollege == false'> AND 0 </if>"
          + "  <if test='includeTutor'>"
          + "    AND ("
          + "      EXISTS (SELECT 1 FROM class_tutor ct WHERE ct.class_id = p.owner_class_id AND ct.tutor_user_id = #{userId})"
          + "      <if test='includeCollege'> OR c.college_id = #{collegeId} </if>"
          + "    )"
          + "  </if>"
          + "  <if test='includeTutor == false and includeCollege == true'>"
          + "    AND c.college_id = #{collegeId}"
          + "  </if>"
          + "  <if test='term != null and term != \"\"'> AND p.term = #{term} </if>"
          + ") t"
          + "</script>")
  long countAccessibleForTeacher(
      @Param("userId") long userId,
      @Param("collegeId") Long collegeId,
      @Param("includeTutor") boolean includeTutor,
      @Param("includeCollege") boolean includeCollege,
      @Param("term") String term);

  @Select(
      "<script>"
          + "SELECT id, owner_type AS ownerType, owner_user_id AS ownerUserId, owner_class_id AS ownerClassId, term, title, created_at AS createdAt "
          + "FROM ("
          + "  SELECT p.id, p.owner_type, p.owner_user_id, p.owner_class_id, p.term, p.title, p.created_at "
          + "  FROM plan p "
          + "  WHERE p.owner_type = 'USER' AND p.owner_user_id = #{userId} "
          + "  <if test='term != null and term != \"\"'> AND p.term = #{term} </if>"
          + "  UNION ALL "
          + "  SELECT p.id, p.owner_type, p.owner_user_id, p.owner_class_id, p.term, p.title, p.created_at "
          + "  FROM plan p "
          + "  JOIN \"class\" c ON c.id = p.owner_class_id "
          + "  WHERE p.owner_type = 'CLASS' "
          + "  <if test='includeTutor == false and includeCollege == false'> AND 0 </if>"
          + "  <if test='includeTutor'>"
          + "    AND ("
          + "      EXISTS (SELECT 1 FROM class_tutor ct WHERE ct.class_id = p.owner_class_id AND ct.tutor_user_id = #{userId})"
          + "      <if test='includeCollege'> OR c.college_id = #{collegeId} </if>"
          + "    )"
          + "  </if>"
          + "  <if test='includeTutor == false and includeCollege == true'>"
          + "    AND c.college_id = #{collegeId}"
          + "  </if>"
          + "  <if test='term != null and term != \"\"'> AND p.term = #{term} </if>"
          + ") t "
          + "ORDER BY created_at DESC, id DESC "
          + "LIMIT #{limit} OFFSET #{offset}"
          + "</script>")
  List<PlanRecord> listAccessibleForTeacher(
      @Param("userId") long userId,
      @Param("collegeId") Long collegeId,
      @Param("includeTutor") boolean includeTutor,
      @Param("includeCollege") boolean includeCollege,
      @Param("term") String term,
      @Param("limit") int limit,
      @Param("offset") int offset);

  @Insert(
      "INSERT INTO plan(owner_type, owner_user_id, owner_class_id, term, title) "
          + "VALUES(#{ownerType}, #{ownerUserId}, #{ownerClassId}, #{term}, #{title})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(PlanInsertParams params);

  final class PlanInsertParams {
    private Long id;
    private PlanOwnerType ownerType;
    private Long ownerUserId;
    private Long ownerClassId;
    private String term;
    private String title;

    public PlanInsertParams() {}

    public PlanInsertParams(Long id, PlanOwnerType ownerType, Long ownerUserId, Long ownerClassId, String term, String title) {
      this.id = id;
      this.ownerType = ownerType;
      this.ownerUserId = ownerUserId;
      this.ownerClassId = ownerClassId;
      this.term = term;
      this.title = title;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public PlanOwnerType ownerType() {
      return ownerType;
    }

    public Long ownerUserId() {
      return ownerUserId;
    }

    public Long ownerClassId() {
      return ownerClassId;
    }

    public String term() {
      return term;
    }

    public String title() {
      return title;
    }
  }
}

