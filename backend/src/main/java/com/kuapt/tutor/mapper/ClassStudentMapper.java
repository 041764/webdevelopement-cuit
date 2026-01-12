package com.kuapt.tutor.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ClassStudentMapper {
  @Select(
      "SELECT EXISTS(" +
          "SELECT 1 FROM class_student cs " +
          "JOIN class_tutor ct ON ct.class_id = cs.class_id " +
          "WHERE cs.student_user_id = #{studentUserId} AND ct.tutor_user_id = #{tutorUserId}" +
          ")")
  boolean isTutorOfStudent(@Param("tutorUserId") long tutorUserId, @Param("studentUserId") long studentUserId);

  @Select("SELECT EXISTS(SELECT 1 FROM class_student WHERE class_id = #{classId} AND student_user_id = #{studentUserId})")
  boolean exists(@Param("classId") long classId, @Param("studentUserId") long studentUserId);

  @Insert("INSERT OR IGNORE INTO class_student(class_id, student_user_id, joined_at) VALUES(#{classId}, #{studentUserId}, CURRENT_TIMESTAMP)")
  int insertIfNotExists(@Param("classId") long classId, @Param("studentUserId") long studentUserId);
}
