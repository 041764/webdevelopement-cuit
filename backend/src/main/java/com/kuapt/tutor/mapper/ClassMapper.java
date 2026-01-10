package com.kuapt.tutor.mapper;

import com.kuapt.tutor.model.ClassRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ClassMapper {
  @Select("SELECT id, college_id AS collegeId FROM \"class\" WHERE id = #{classId} LIMIT 1")
  ClassRecord findById(@Param("classId") long classId);

  @Select("SELECT EXISTS(SELECT 1 FROM class_student WHERE class_id = #{classId} AND student_user_id = #{studentUserId})")
  boolean isStudentInClass(@Param("classId") long classId, @Param("studentUserId") long studentUserId);

  @Select("SELECT EXISTS(SELECT 1 FROM class_tutor WHERE class_id = #{classId} AND tutor_user_id = #{tutorUserId})")
  boolean isTutorOfClass(@Param("classId") long classId, @Param("tutorUserId") long tutorUserId);
}

