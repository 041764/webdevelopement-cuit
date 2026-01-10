package com.kuapt.tutor.mapper;

import com.kuapt.tutor.mapper.typehandler.InstantTypeHandler;
import com.kuapt.tutor.model.RefreshTokenRecord;
import java.time.Instant;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RefreshTokenMapper {
  @Insert(
      "INSERT INTO refresh_token(user_id, token_hash, device_id, issued_at, expires_at, revoked_at, created_at) "
          + "VALUES(#{userId}, #{tokenHash}, #{deviceId}, #{issuedAt,typeHandler=com.kuapt.tutor.mapper.typehandler.InstantTypeHandler}, #{expiresAt,typeHandler=com.kuapt.tutor.mapper.typehandler.InstantTypeHandler}, NULL, CURRENT_TIMESTAMP)")
  int insert(
      @Param("userId") long userId,
      @Param("tokenHash") String tokenHash,
      @Param("deviceId") String deviceId,
      @Param("issuedAt") Instant issuedAt,
      @Param("expiresAt") Instant expiresAt);

  @Select(
      "SELECT id, user_id AS userId, token_hash AS tokenHash, device_id AS deviceId, "
          + "issued_at AS issuedAt, expires_at AS expiresAt, revoked_at AS revokedAt "
          + "FROM refresh_token WHERE token_hash = #{tokenHash} LIMIT 1")
  @ConstructorArgs({
    @Arg(column = "id", javaType = long.class),
    @Arg(column = "userId", javaType = long.class),
    @Arg(column = "tokenHash", javaType = String.class),
    @Arg(column = "deviceId", javaType = String.class),
    @Arg(column = "issuedAt", javaType = Instant.class, typeHandler = InstantTypeHandler.class),
    @Arg(column = "expiresAt", javaType = Instant.class, typeHandler = InstantTypeHandler.class),
    @Arg(column = "revokedAt", javaType = Instant.class, typeHandler = InstantTypeHandler.class)
  })
  RefreshTokenRecord findByTokenHash(@Param("tokenHash") String tokenHash);

  @Update(
      "UPDATE refresh_token SET revoked_at = #{revokedAt,typeHandler=com.kuapt.tutor.mapper.typehandler.InstantTypeHandler} "
          + "WHERE token_hash = #{tokenHash} AND revoked_at IS NULL")
  int revokeByHash(@Param("tokenHash") String tokenHash, @Param("revokedAt") Instant revokedAt);
}
