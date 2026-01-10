package com.kuapt.tutor.mapper;

import com.kuapt.tutor.model.RoleCode;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper {
  @Select(
      "SELECT r.code FROM user_role ur JOIN role r ON ur.role_id = r.id WHERE ur.user_id = #{userId} ORDER BY r.code")
  List<RoleCode> listRoleCodes(@Param("userId") long userId);
}
