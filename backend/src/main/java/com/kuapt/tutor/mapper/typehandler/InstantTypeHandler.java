package com.kuapt.tutor.mapper.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(Instant.class)
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.TIMESTAMP, JdbcType.BIGINT, JdbcType.INTEGER})
public class InstantTypeHandler extends BaseTypeHandler<Instant> {
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Instant parameter, JdbcType jdbcType)
      throws SQLException {
    // Persist as ISO-8601 text for portability.
    ps.setString(i, parameter.toString());
  }

  @Override
  public Instant getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return parse(rs.getString(columnName));
  }

  @Override
  public Instant getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return parse(rs.getString(columnIndex));
  }

  @Override
  public Instant getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return parse(cs.getString(columnIndex));
  }

  private static Instant parse(String value) {
    if (value == null) {
      return null;
    }
    // Some JDBC paths return epoch-millis text for SQLite; accept both.
    if (value.chars().allMatch(Character::isDigit)) {
      return Instant.ofEpochMilli(Long.parseLong(value));
    }
    // SQLite CURRENT_TIMESTAMP is like "2026-01-10 19:00:52"; accept that too.
    if (value.length() == 19 && value.charAt(10) == ' ') {
      return Instant.parse(value.substring(0, 10) + "T" + value.substring(11) + "Z");
    }
    return Instant.parse(value);
  }
}
