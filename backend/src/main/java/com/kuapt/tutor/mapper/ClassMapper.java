package com.kuapt.tutor.mapper;

import com.kuapt.tutor.model.ClassRecord;
import com.kuapt.tutor.service.LookupService;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ClassMapper {
  @Select("SELECT id, college_id AS collegeId FROM \"class\" WHERE id = #{classId} LIMIT 1")
  ClassRecord findById(@Param("classId") long classId);

  @Select("SELECT id FROM \"class\" WHERE name = #{name} AND college_id = #{collegeId} LIMIT 1")
  Long findIdByNameAndCollege(@Param("name") String name, @Param("collegeId") long collegeId);

  @Insert("INSERT INTO \"class\"(term, name, college_id, created_at) VALUES(#{term}, #{name}, #{collegeId}, CURRENT_TIMESTAMP)")
  int insert(@Param("term") String term, @Param("name") String name, @Param("collegeId") long collegeId);

  @Select("SELECT EXISTS(SELECT 1 FROM class_student WHERE class_id = #{classId} AND student_user_id = #{studentUserId})")
  boolean isStudentInClass(@Param("classId") long classId, @Param("studentUserId") long studentUserId);

  @Select("SELECT EXISTS(SELECT 1 FROM class_tutor WHERE class_id = #{classId} AND tutor_user_id = #{tutorUserId})")
  boolean isTutorOfClass(@Param("classId") long classId, @Param("tutorUserId") long tutorUserId);

  @Select(
      "<script>"
          + "SELECT c.id, c.name, col.name AS collegeName "
          + "FROM \"class\" c "
          + "LEFT JOIN college col ON c.college_id = col.id "
          + "WHERE 1=1 "
          + "<if test='collegeId != null'> AND c.college_id = #{collegeId} </if>"
          + "<if test='term != null and term != \"\"'> AND c.term = #{term} </if>"
          + "ORDER BY col.name ASC, c.name ASC"
          + "</script>")
  List<LookupService.ClassOption> listClassOptions(@Param("collegeId") Long collegeId, @Param("term") String term);
}

