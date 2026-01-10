package com.kuapt.tutor.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CollegeMapper {
  @Select("SELECT id FROM college WHERE name = #{name} LIMIT 1")
  Long findIdByName(@Param("name") String name);

  @Select("SELECT name FROM college WHERE id = #{collegeId} LIMIT 1")
  String findNameById(@Param("collegeId") long collegeId);

  @Insert("INSERT INTO college(name) VALUES(#{name})")
  int insert(@Param("name") String name);
}
