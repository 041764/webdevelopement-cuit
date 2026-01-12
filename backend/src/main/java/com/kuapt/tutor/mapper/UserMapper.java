package com.kuapt.tutor.mapper;

import com.kuapt.tutor.model.UserRecord;
import com.kuapt.tutor.model.UserType;
import com.kuapt.tutor.service.LookupService;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
  @Select(
      "SELECT id AS userId, user_type AS userType, user_no AS id, name, college_id AS collegeId, status "
          + "FROM \"user\" WHERE user_type = #{userType} AND user_no = #{userNo} LIMIT 1")
  UserRecord findByTypeAndNo(@Param("userType") UserType userType, @Param("userNo") String userNo);

  @Select(
      "SELECT id AS userId, user_type AS userType, user_no AS id, name, college_id AS collegeId, status "
          + "FROM \"user\" WHERE id = #{userId} LIMIT 1")
  UserRecord findById(@Param("userId") long userId);

  @Insert(
      "INSERT INTO \"user\"(user_type, user_no, name, college_id, status) "
          + "VALUES(#{userType}, #{userNo}, #{name}, #{collegeId}, 'ACTIVE')")
  int insertUser(
      @Param("userType") UserType userType,
      @Param("userNo") String userNo,
      @Param("name") String name,
      @Param("collegeId") Long collegeId);

  @Update(
      "UPDATE \"user\" SET name = #{name}, college_id = #{collegeId}, updated_at = CURRENT_TIMESTAMP "
          + "WHERE user_type = #{userType} AND user_no = #{userNo}")
  int updateUser(
      @Param("userType") UserType userType,
      @Param("userNo") String userNo,
      @Param("name") String name,
      @Param("collegeId") Long collegeId);

  @Select(
      "<script>"
          + "SELECT id, user_no AS userNo, name "
          + "FROM \"user\" "
          + "WHERE user_type = 'STUDENT' AND status = 'ACTIVE' "
          + "<if test='collegeId != null'> AND college_id = #{collegeId} </if>"
          + "ORDER BY user_no ASC"
          + "</script>")
  List<LookupService.StudentOption> listStudents(@Param("collegeId") Long collegeId);
}
