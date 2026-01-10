package com.kuapt.tutor.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuapt.tutor.model.UserType;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {
  @TempDir static Path tempDir;

  private static final String TEST_PEPPER = "test-pepper";
  private static final String TEST_JWT_SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef";

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", () -> "jdbc:sqlite:" + tempDir.resolve("test.db"));
    r.add("app.jwt.secret", () -> TEST_JWT_SECRET);
    r.add("app.jwt.issuer", () -> "test-issuer");
    r.add("app.jwt.access-ttl", () -> "6h");
    r.add("app.jwt.refresh-ttl", () -> "12h");
    r.add("app.password.pepper", () -> TEST_PEPPER);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper om;
  @Autowired DataSource dataSource;

  private TestDb db;

  private long roleAdminSchoolId;
  private long roleAdminCollegeId;
  private long roleTutorId;

  @BeforeEach
  void setup() throws Exception {
    db = new TestDb(dataSource, TEST_PEPPER);
    db.wipeAll();
    long college1 = db.insertCollege("计算机学院");
    long college2 = db.insertCollege("外国语学院");

    roleAdminSchoolId = db.insertRole("ADMIN_SCHOOL", "校级管理员");
    roleAdminCollegeId = db.insertRole("ADMIN_COLLEGE", "院级管理员");
    roleTutorId = db.insertRole("TUTOR", "导师");

    // Users for tests
    long tutorUserId = db.insertUser(UserType.TEACHER, "1001", "Tutor A", college1);
    db.grantRole(tutorUserId, roleTutorId);
    db.upsertCredential(tutorUserId, "salt", "hash-tutor");

    long studentUserId = db.insertUser(UserType.STUDENT, "1001", "Student A", college1);
    db.upsertCredential(studentUserId, "salt", "hash-student");

    long adminCollegeUserId = db.insertUser(UserType.TEACHER, "2001", "Admin C1", college1);
    db.grantRole(adminCollegeUserId, roleAdminCollegeId);
    db.upsertCredential(adminCollegeUserId, "salt", "hash-admin-college");

    long otherTeacherUserId = db.insertUser(UserType.TEACHER, "3001", "Teacher C2", college2);
    db.upsertCredential(otherTeacherUserId, "salt", "hash-teacher2");

    long adminSchoolUserId = db.insertUser(UserType.TEACHER, "4001", "Admin School", null);
    db.grantRole(adminSchoolUserId, roleAdminSchoolId);
    db.upsertCredential(adminSchoolUserId, "salt", "hash-admin-school");
  }

  @Test
  void loginRequiresUserTypeToDisambiguate() throws Exception {
    // Same id exists for STUDENT and TEACHER; selecting wrong type should fail.
    mvc.perform(
            post("/auth/login")
                .contentType(APPLICATION_JSON)
                .content(
                    "{" +
                        "\"userType\":\"STUDENT\"," +
                        "\"id\":\"1001\"," +
                        "\"clientSalt\":\"salt\"," +
                        "\"clientHash\":\"hash-student\"" +
                        "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.refreshToken").isString());

    mvc.perform(
            post("/auth/login")
                .contentType(APPLICATION_JSON)
                .content(
                    "{" +
                        "\"userType\":\"TEACHER\"," +
                        "\"id\":\"1001\"," +
                        "\"clientSalt\":\"salt\"," +
                        "\"clientHash\":\"hash-student\"" +
                        "}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
  }

  @Test
  void refreshRotatesAndRevokesOldToken() throws Exception {
    TokenPair pair = login("STUDENT", "1001", "hash-student", "dev1");

    // refresh once -> ok
    TokenPair refreshed = refresh(pair.refreshToken(), "dev1");
    assertThat(refreshed.refreshToken()).isNotEqualTo(pair.refreshToken());

    // old refresh should be revoked
    mvc.perform(post("/auth/refresh")
            .contentType(APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + pair.refreshToken() + "\",\"deviceId\":\"dev1\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_TOKEN_REVOKED"));
  }

  @Test
  void logoutRevokesSpecificRefreshTokenButAllowsOtherDevice() throws Exception {
    TokenPair p1 = login("STUDENT", "1001", "hash-student", "dev1");
    TokenPair p2 = login("STUDENT", "1001", "hash-student", "dev2");

    // logout dev1 token
    mvc.perform(post("/auth/logout")
            .header("Authorization", "Bearer " + p1.accessToken())
            .contentType(APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + p1.refreshToken() + "\"}"))
        .andExpect(status().isNoContent());

    // dev1 refresh should fail
    mvc.perform(post("/auth/refresh")
            .contentType(APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + p1.refreshToken() + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_TOKEN_REVOKED"));

    // dev2 refresh still works
    mvc.perform(post("/auth/refresh")
            .contentType(APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + p2.refreshToken() + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.refreshToken").isString());
  }

  @Test
  void meReturnsCurrentUser() throws Exception {
    TokenPair pair = login("TEACHER", "1001", "hash-tutor", "dev1");

    mvc.perform(get("/auth/me").header("Authorization", "Bearer " + pair.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").isNumber())
        .andExpect(jsonPath("$.id").value("1001"))
        .andExpect(jsonPath("$.roles").isArray());
  }

  @Test
  void passwordResetPermissionMatrix() throws Exception {
    // tutor can reset student
    TokenPair tutor = login("TEACHER", "1001", "hash-tutor", "tutor-dev");
    long tutorUserId = meUserId(tutor.accessToken());

    TokenPair student = login("STUDENT", "1001", "hash-student", "s-dev");
    long studentUserId = meUserId(student.accessToken());

    mvc.perform(post("/users/" + studentUserId + "/password:reset")
            .header("Authorization", "Bearer " + tutor.accessToken())
            .contentType(APPLICATION_JSON)
            .content("{\"clientSalt\":\"s\",\"clientHash\":\"newhash\"}"))
        .andExpect(status().isNoContent());

    // tutor cannot reset teacher
    mvc.perform(post("/users/" + tutorUserId + "/password:reset")
            .header("Authorization", "Bearer " + tutor.accessToken())
            .contentType(APPLICATION_JSON)
            .content("{\"clientSalt\":\"s\",\"clientHash\":\"newhash\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));

    // college admin can reset teacher in same college, but not in other college
    TokenPair adminCollege = login("TEACHER", "2001", "hash-admin-college", "a-dev");

    // Reset tutor (same college) -> ok
    mvc.perform(post("/users/" + tutorUserId + "/password:reset")
            .header("Authorization", "Bearer " + adminCollege.accessToken())
            .contentType(APPLICATION_JSON)
            .content("{\"clientSalt\":\"s\",\"clientHash\":\"adminreset\"}"))
        .andExpect(status().isNoContent());

    // Reset teacher in other college -> forbidden
    TokenPair otherTeacher = login("TEACHER", "3001", "hash-teacher2", "t2");
    long otherTeacherId = meUserId(otherTeacher.accessToken());

    mvc.perform(post("/users/" + otherTeacherId + "/password:reset")
            .header("Authorization", "Bearer " + adminCollege.accessToken())
            .contentType(APPLICATION_JSON)
            .content("{\"clientSalt\":\"s\",\"clientHash\":\"adminreset\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
  }

  private TokenPair login(String userType, String id, String clientHash, String deviceId) throws Exception {
    MvcResult res = mvc.perform(
            post("/auth/login")
                .contentType(APPLICATION_JSON)
                .content(
                    "{" +
                        "\"userType\":\"" + userType + "\"," +
                        "\"id\":\"" + id + "\"," +
                        "\"clientSalt\":\"salt\"," +
                        "\"clientHash\":\"" + clientHash + "\"," +
                        "\"deviceId\":\"" + deviceId + "\"" +
                        "}"))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode n = om.readTree(res.getResponse().getContentAsString());
    return new TokenPair(
        n.get("accessToken").asText(),
        n.get("refreshToken").asText(),
        java.time.Instant.parse(n.get("accessExpiresAt").asText()),
        java.time.Instant.parse(n.get("refreshExpiresAt").asText()));
  }

  private TokenPair refresh(String refreshToken, String deviceId) throws Exception {
    MvcResult res = mvc.perform(
            post("/auth/refresh")
                .contentType(APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\",\"deviceId\":\"" + deviceId + "\"}"))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode n = om.readTree(res.getResponse().getContentAsString());
    return new TokenPair(
        n.get("accessToken").asText(),
        n.get("refreshToken").asText(),
        java.time.Instant.parse(n.get("accessExpiresAt").asText()),
        java.time.Instant.parse(n.get("refreshExpiresAt").asText()));
  }

  private long meUserId(String accessToken) throws Exception {
    MvcResult res = mvc.perform(get("/auth/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andReturn();
    JsonNode n = om.readTree(res.getResponse().getContentAsString());
    return n.get("userId").asLong();
  }
}
