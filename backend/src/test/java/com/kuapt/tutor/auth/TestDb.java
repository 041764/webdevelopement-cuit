package com.kuapt.tutor.auth;

import com.kuapt.tutor.model.UserType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import javax.sql.DataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class TestDb {
  private final DataSource dataSource;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  private final String pepper;

  public TestDb(DataSource dataSource, String pepper) {
    this.dataSource = dataSource;
    this.pepper = pepper;
  }

  public void wipeAll() throws Exception {
    try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
      st.executeUpdate("DELETE FROM user_role");
      st.executeUpdate("DELETE FROM refresh_token");
      st.executeUpdate("DELETE FROM local_credential");
      st.executeUpdate("DELETE FROM activity_signup");
      st.executeUpdate("DELETE FROM activity");
      st.executeUpdate("DELETE FROM class_student");
      st.executeUpdate("DELETE FROM class_tutor");
      st.executeUpdate("DELETE FROM \"class\"");
      st.executeUpdate("DELETE FROM evaluation_detail");
      st.executeUpdate("DELETE FROM evaluation");
      st.executeUpdate("DELETE FROM plan_item_progress");
      st.executeUpdate("DELETE FROM plan_item");
      st.executeUpdate("DELETE FROM plan");
      st.executeUpdate("DELETE FROM role");
      st.executeUpdate("DELETE FROM \"user\"");
      st.executeUpdate("DELETE FROM college");
    }
  }

  public long insertCollege(String name) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("INSERT INTO college(name) VALUES(?)", Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, name);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  public long insertRole(String code, String name) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("INSERT INTO role(code, name) VALUES(?, ?)", Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, code);
      ps.setString(2, name);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  public long insertUser(UserType type, String userNo, String name, Long collegeId) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO \"user\"(user_type, user_no, name, college_id, status) VALUES(?, ?, ?, ?, 'ACTIVE')",
                Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, type.name());
      ps.setString(2, userNo);
      ps.setString(3, name);
      if (collegeId == null) {
        ps.setNull(4, java.sql.Types.INTEGER);
      } else {
        ps.setLong(4, collegeId);
      }
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  public void grantRole(long userId, long roleId) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("INSERT INTO user_role(user_id, role_id) VALUES(?, ?)");) {
      ps.setLong(1, userId);
      ps.setLong(2, roleId);
      ps.executeUpdate();
    }
  }

  public void upsertCredential(long userId, String clientSalt, String clientHash) throws Exception {
    String serverHash = encoder.encode(pepper + ":" + clientHash);
    try (Connection c = dataSource.getConnection()) {
      try (PreparedStatement del = c.prepareStatement("DELETE FROM local_credential WHERE user_id=?")) {
        del.setLong(1, userId);
        del.executeUpdate();
      }
      try (PreparedStatement ins =
          c.prepareStatement(
              "INSERT INTO local_credential(user_id, client_salt, client_hash, server_hash, created_at, updated_at) VALUES(?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")) {
        ins.setLong(1, userId);
        ins.setString(2, clientSalt);
        ins.setString(3, clientHash);
        ins.setString(4, serverHash);
        ins.executeUpdate();
      }
    }
  }

  public void insertRefreshTokenHash(long userId, String tokenHash, String deviceId, Instant issuedAt, Instant expiresAt) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO refresh_token(user_id, token_hash, device_id, issued_at, expires_at, revoked_at, created_at) VALUES(?, ?, ?, ?, ?, NULL, CURRENT_TIMESTAMP)")) {
      ps.setLong(1, userId);
      ps.setString(2, tokenHash);
      ps.setString(3, deviceId);
      ps.setString(4, issuedAt.toString());
      ps.setString(5, expiresAt.toString());
      ps.executeUpdate();
    }
  }

  public Long findCollegeIdByName(String name) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT id FROM college WHERE name = ? LIMIT 1")) {
      ps.setString(1, name);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        return rs.getLong(1);
      }
    }
  }

  public long countUsersByTypeAndNo(UserType type, String userNo) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT COUNT(1) FROM \"user\" WHERE user_type = ? AND user_no = ?")) {
      ps.setString(1, type.name());
      ps.setString(2, userNo);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  public String findUserName(UserType type, String userNo) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("SELECT name FROM \"user\" WHERE user_type = ? AND user_no = ? LIMIT 1")) {
      ps.setString(1, type.name());
      ps.setString(2, userNo);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        return rs.getString(1);
      }
    }
  }

  public Long findUserCollegeId(UserType type, String userNo) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement("SELECT college_id FROM \"user\" WHERE user_type = ? AND user_no = ? LIMIT 1")) {
      ps.setString(1, type.name());
      ps.setString(2, userNo);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        long v = rs.getLong(1);
        if (rs.wasNull()) {
          return null;
        }
        return v;
      }
    }
  }

  public long insertClass(String term, String name, long collegeId) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO \"class\"(term, name, college_id, created_at) VALUES(?, ?, ?, CURRENT_TIMESTAMP)",
                Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, term);
      ps.setString(2, name);
      ps.setLong(3, collegeId);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  public void assignTutor(long classId, long tutorUserId) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO class_tutor(class_id, tutor_user_id, assigned_at) VALUES(?, ?, CURRENT_TIMESTAMP)")) {
      ps.setLong(1, classId);
      ps.setLong(2, tutorUserId);
      ps.executeUpdate();
    }
  }

  public void enrollStudent(long classId, long studentUserId) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO class_student(class_id, student_user_id, joined_at) VALUES(?, ?, CURRENT_TIMESTAMP)")) {
      ps.setLong(1, classId);
      ps.setLong(2, studentUserId);
      ps.executeUpdate();
    }
  }

  public long insertEvaluation(long evaluatorUserId, long evaluateeUserId, String term, int scoreTotal, String comment)
      throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO evaluation(evaluator_user_id, evaluatee_user_id, term, score_total, comment, created_at) VALUES(?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                Statement.RETURN_GENERATED_KEYS)) {
      ps.setLong(1, evaluatorUserId);
      ps.setLong(2, evaluateeUserId);
      ps.setString(3, term);
      ps.setInt(4, scoreTotal);
      ps.setString(5, comment);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  public void insertEvaluationDetail(long evaluationId, String itemKey, int score, String comment) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO evaluation_detail(evaluation_id, item_key, score, comment) VALUES(?, ?, ?, ?)")) {
      ps.setLong(1, evaluationId);
      ps.setString(2, itemKey);
      ps.setInt(3, score);
      ps.setString(4, comment);
      ps.executeUpdate();
    }
  }

  public long insertPlan(String ownerType, Long ownerUserId, Long ownerClassId, String term, String title) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO plan(owner_type, owner_user_id, owner_class_id, term, title, created_at) VALUES(?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, ownerType);
      if (ownerUserId == null) {
        ps.setNull(2, java.sql.Types.INTEGER);
      } else {
        ps.setLong(2, ownerUserId);
      }
      if (ownerClassId == null) {
        ps.setNull(3, java.sql.Types.INTEGER);
      } else {
        ps.setLong(3, ownerClassId);
      }
      ps.setString(4, term);
      ps.setString(5, title);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  public long insertPlanItem(long planId, String title, String status) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO plan_item(plan_id, title, status, sort_order, created_at, updated_at) VALUES(?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                Statement.RETURN_GENERATED_KEYS)) {
      ps.setLong(1, planId);
      ps.setString(2, title);
      ps.setString(3, status);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  public long insertActivity(long classId, String term, String title, String status, long createdByUserId) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO activity(class_id, term, title, description, capacity, requires_review, status, created_by_user_id, created_at) VALUES(?, ?, ?, NULL, NULL, 0, ?, ?, CURRENT_TIMESTAMP)",
                Statement.RETURN_GENERATED_KEYS)) {
      ps.setLong(1, classId);
      ps.setString(2, term);
      ps.setString(3, title);
      ps.setString(4, status);
      ps.setLong(5, createdByUserId);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  public void insertActivitySignup(long activityId, long userId, String status) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO activity_signup(activity_id, user_id, status, note, created_at, reviewed_at, reviewed_by_user_id) VALUES(?, ?, ?, NULL, CURRENT_TIMESTAMP, NULL, NULL)")) {
      ps.setLong(1, activityId);
      ps.setLong(2, userId);
      ps.setString(3, status);
      ps.executeUpdate();
    }
  }
}

