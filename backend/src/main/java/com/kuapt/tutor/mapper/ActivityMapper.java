package com.kuapt.tutor.mapper;

import com.kuapt.tutor.model.ActivityRecord;
import com.kuapt.tutor.model.ActivityStatus;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityMapper {
  @Select(
      "SELECT a.id, a.class_id AS classId, c.name AS className, a.term, a.title, a.description, a.capacity, a.requires_review AS requiresReview, a.status, "
          + "a.created_by_user_id AS createdByUserId, a.created_at AS createdAt "
          + "FROM activity a JOIN \"class\" c ON c.id = a.class_id WHERE a.id = #{activityId} LIMIT 1")
  ActivityRecord findById(@Param("activityId") long activityId);

  @Select(
      "<script>"
          + "SELECT a.id, a.class_id AS classId, c.name AS className, a.term, a.title, a.description, a.capacity, "
          + "a.requires_review AS requiresReview, a.status, a.created_by_user_id AS createdByUserId, a.created_at AS createdAt "
          + "FROM activity a "
          + "JOIN \"class\" c ON c.id = a.class_id "
          + "JOIN class_student cs ON cs.class_id = a.class_id AND cs.student_user_id = #{studentUserId} "
          + "WHERE a.id = #{activityId} "
          + "LIMIT 1"
          + "</script>")
  ActivityRecord findForStudentById(@Param("activityId") long activityId, @Param("studentUserId") long studentUserId);

  @Select(
      "<script>"
          + "SELECT a.id, a.class_id AS classId, c.name AS className, a.term, a.title, a.description, a.capacity, "
          + "a.requires_review AS requiresReview, a.status, a.created_by_user_id AS createdByUserId, a.created_at AS createdAt "
          + "FROM activity a "
          + "JOIN \"class\" c ON c.id = a.class_id "
          + "WHERE a.id = #{activityId} "
          + "<if test='includeTutor == false and includeCollege == false'> AND 0 </if>"
          + "<if test='includeTutor'>"
          + " AND ("
          + "   EXISTS (SELECT 1 FROM class_tutor ct WHERE ct.class_id = a.class_id AND ct.tutor_user_id = #{teacherUserId})"
          + "   <if test='includeCollege'> OR c.college_id = #{collegeId} </if>"
          + " )"
          + "</if>"
          + "<if test='includeTutor == false and includeCollege == true'>"
          + " AND c.college_id = #{collegeId}"
          + "</if>"
          + " LIMIT 1"
          + "</script>")
  ActivityRecord findForTeacherById(
      @Param("activityId") long activityId,
      @Param("teacherUserId") long teacherUserId,
      @Param("collegeId") Long collegeId,
      @Param("includeTutor") boolean includeTutor,
      @Param("includeCollege") boolean includeCollege);

  @Select(
      "<script>"
          + "SELECT COUNT(1) FROM activity "
          + "WHERE 1=1 "
          + "<if test='term != null and term != \"\"'> AND term = #{term} </if>"
          + "<if test='status != null'> AND status = #{status} </if>"
          + "</script>")
  long countAll(@Param("term") String term, @Param("status") ActivityStatus status);

  @Select(
      "<script>"
          + "SELECT a.id, a.class_id AS classId, c.name AS className, a.term, a.title, a.description, a.capacity, a.requires_review AS requiresReview, a.status, "
          + "a.created_by_user_id AS createdByUserId, a.created_at AS createdAt "
          + "FROM activity a "
          + "JOIN \"class\" c ON c.id = a.class_id "
          + "WHERE 1=1 "
          + "<if test='term != null and term != \"\"'> AND a.term = #{term} </if>"
          + "<if test='status != null'> AND a.status = #{status} </if>"
          + "ORDER BY a.created_at DESC, a.id DESC "
          + "LIMIT #{limit} OFFSET #{offset}"
          + "</script>")
  List<ActivityRecord> listAll(@Param("term") String term, @Param("status") ActivityStatus status, @Param("limit") int limit, @Param("offset") int offset);

  @Select(
      "<script>"
          + "SELECT COUNT(1) "
          + "FROM activity a "
          + "JOIN class_student cs ON cs.class_id = a.class_id AND cs.student_user_id = #{studentUserId} "
          + "WHERE 1=1 "
          + "<if test='term != null and term != \"\"'> AND a.term = #{term} </if>"
          + "<if test='status != null'> AND a.status = #{status} </if>"
          + "</script>")
  long countForStudent(@Param("studentUserId") long studentUserId, @Param("term") String term, @Param("status") ActivityStatus status);

  @Select(
      "<script>"
          + "SELECT a.id, a.class_id AS classId, c.name AS className, a.term, a.title, a.description, a.capacity, a.requires_review AS requiresReview, a.status, "
          + "a.created_by_user_id AS createdByUserId, a.created_at AS createdAt "
          + "FROM activity a "
          + "JOIN \"class\" c ON c.id = a.class_id "
          + "JOIN class_student cs ON cs.class_id = a.class_id AND cs.student_user_id = #{studentUserId} "
          + "WHERE 1=1 "
          + "<if test='term != null and term != \"\"'> AND a.term = #{term} </if>"
          + "<if test='status != null'> AND a.status = #{status} </if>"
          + "ORDER BY a.created_at DESC, a.id DESC "
          + "LIMIT #{limit} OFFSET #{offset}"
          + "</script>")
  List<ActivityRecord> listForStudent(
      @Param("studentUserId") long studentUserId,
      @Param("term") String term,
      @Param("status") ActivityStatus status,
      @Param("limit") int limit,
      @Param("offset") int offset);

  @Select(
      "<script>"
          + "SELECT COUNT(1) "
          + "FROM activity a "
          + "JOIN \"class\" c ON c.id = a.class_id "
          + "WHERE 1=1 "
          + "<if test='includeTutor == false and includeCollege == false'> AND 0 </if>"
          + "<if test='includeTutor'>"
          + " AND ("
          + "   EXISTS (SELECT 1 FROM class_tutor ct WHERE ct.class_id = a.class_id AND ct.tutor_user_id = #{teacherUserId})"
          + "   <if test='includeCollege'> OR c.college_id = #{collegeId} </if>"
          + " )"
          + "</if>"
          + "<if test='includeTutor == false and includeCollege == true'>"
          + " AND c.college_id = #{collegeId}"
          + "</if>"
          + "<if test='term != null and term != \"\"'> AND a.term = #{term} </if>"
          + "<if test='status != null'> AND a.status = #{status} </if>"
          + "</script>")
  long countForTeacher(
      @Param("teacherUserId") long teacherUserId,
      @Param("collegeId") Long collegeId,
      @Param("includeTutor") boolean includeTutor,
      @Param("includeCollege") boolean includeCollege,
      @Param("term") String term,
      @Param("status") ActivityStatus status);

  @Select(
      "<script>"
          + "SELECT a.id, a.class_id AS classId, c.name AS className, a.term, a.title, a.description, a.capacity, a.requires_review AS requiresReview, a.status, "
          + "a.created_by_user_id AS createdByUserId, a.created_at AS createdAt "
          + "FROM activity a "
          + "JOIN \"class\" c ON c.id = a.class_id "
          + "WHERE 1=1 "
          + "<if test='includeTutor == false and includeCollege == false'> AND 0 </if>"
          + "<if test='includeTutor'>"
          + " AND ("
          + "   EXISTS (SELECT 1 FROM class_tutor ct WHERE ct.class_id = a.class_id AND ct.tutor_user_id = #{teacherUserId})"
          + "   <if test='includeCollege'> OR c.college_id = #{collegeId} </if>"
          + " )"
          + "</if>"
          + "<if test='includeTutor == false and includeCollege == true'>"
          + " AND c.college_id = #{collegeId}"
          + "</if>"
          + "<if test='term != null and term != \"\"'> AND a.term = #{term} </if>"
          + "<if test='status != null'> AND a.status = #{status} </if>"
          + "ORDER BY a.created_at DESC, a.id DESC "
          + "LIMIT #{limit} OFFSET #{offset}"
          + "</script>")
  List<ActivityRecord> listForTeacher(
      @Param("teacherUserId") long teacherUserId,
      @Param("collegeId") Long collegeId,
      @Param("includeTutor") boolean includeTutor,
      @Param("includeCollege") boolean includeCollege,
      @Param("term") String term,
      @Param("status") ActivityStatus status,
      @Param("limit") int limit,
      @Param("offset") int offset);

  @Update("UPDATE activity SET status = 'PUBLISHED' WHERE id = #{activityId} AND status = 'DRAFT'")
  int publishDraft(@Param("activityId") long activityId);

  @Insert(
      "INSERT INTO activity(class_id, term, title, description, capacity, requires_review, status, created_by_user_id) "
          + "VALUES(#{classId}, #{term}, #{title}, #{description}, #{capacity}, #{requiresReview}, 'DRAFT', #{createdByUserId})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(ActivityInsertParams params);

  final class ActivityInsertParams {
    private Long id;
    private long classId;
    private String term;
    private String title;
    private String description;
    private Integer capacity;
    private boolean requiresReview;
    private long createdByUserId;

    public ActivityInsertParams() {}

    public ActivityInsertParams(
        Long id,
        long classId,
        String term,
        String title,
        String description,
        Integer capacity,
        boolean requiresReview,
        long createdByUserId) {
      this.id = id;
      this.classId = classId;
      this.term = term;
      this.title = title;
      this.description = description;
      this.capacity = capacity;
      this.requiresReview = requiresReview;
      this.createdByUserId = createdByUserId;
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

    public long classId() {
      return classId;
    }

    public long getClassId() {
      return classId;
    }

    public String term() {
      return term;
    }

    public String getTerm() {
      return term;
    }

    public String title() {
      return title;
    }

    public String getTitle() {
      return title;
    }

    public String description() {
      return description;
    }

    public String getDescription() {
      return description;
    }

    public Integer capacity() {
      return capacity;
    }

    public Integer getCapacity() {
      return capacity;
    }

    public boolean requiresReview() {
      return requiresReview;
    }

    public boolean isRequiresReview() {
      return requiresReview;
    }

    public long createdByUserId() {
      return createdByUserId;
    }

    public long getCreatedByUserId() {
      return createdByUserId;
    }
  }
}
