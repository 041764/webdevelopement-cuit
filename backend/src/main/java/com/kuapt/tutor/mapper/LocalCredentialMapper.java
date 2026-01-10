package com.kuapt.tutor.mapper;

import com.kuapt.tutor.model.LocalCredentialRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LocalCredentialMapper {
  @Select(
      "SELECT user_id AS userId, client_salt AS clientSalt, client_hash AS clientHash, server_hash AS serverHash "
          + "FROM local_credential WHERE user_id = #{userId} LIMIT 1")
  LocalCredentialRecord findByUserId(@Param("userId") long userId);

  @Insert(
      "INSERT INTO local_credential(user_id, client_salt, client_hash, server_hash, created_at, updated_at) "
          + "VALUES(#{userId}, #{clientSalt}, #{clientHash}, #{serverHash}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
  int insert(
      @Param("userId") long userId,
      @Param("clientSalt") String clientSalt,
      @Param("clientHash") String clientHash,
      @Param("serverHash") String serverHash);

  @Update(
      "UPDATE local_credential SET client_salt=#{clientSalt}, client_hash=#{clientHash}, server_hash=#{serverHash}, updated_at=CURRENT_TIMESTAMP "
          + "WHERE user_id=#{userId}")
  int update(
      @Param("userId") long userId,
      @Param("clientSalt") String clientSalt,
      @Param("clientHash") String clientHash,
      @Param("serverHash") String serverHash);
}
