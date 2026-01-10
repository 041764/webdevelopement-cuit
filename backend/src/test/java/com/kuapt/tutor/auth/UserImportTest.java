package com.kuapt.tutor.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuapt.tutor.model.UserType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class UserImportTest {
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

    long tutorUserId = db.insertUser(UserType.TEACHER, "1001", "Tutor A", college1);
    db.grantRole(tutorUserId, roleTutorId);
    db.upsertCredential(tutorUserId, "salt", "hash-tutor");

    long studentUserId = db.insertUser(UserType.STUDENT, "1001", "Student A", college1);
    db.upsertCredential(studentUserId, "salt", "hash-student");

    long adminCollegeUserId = db.insertUser(UserType.TEACHER, "2001", "Admin C1", college1);
    db.grantRole(adminCollegeUserId, roleAdminCollegeId);
    db.upsertCredential(adminCollegeUserId, "salt", "hash-admin-college");

    long adminSchoolUserId = db.insertUser(UserType.TEACHER, "4001", "Admin School", null);
    db.grantRole(adminSchoolUserId, roleAdminSchoolId);
    db.upsertCredential(adminSchoolUserId, "salt", "hash-admin-school");

    // Exists only as TEACHER; importing STUDENT with same user_no must not conflict.
    db.insertUser(UserType.TEACHER, "5001", "Teacher Only", college2);
  }

  @Test
  void tutorImportStudentCreateAndUpdateAndNoConflictAcrossUserType() throws Exception {
    TokenPair tutor = login("TEACHER", "1001", "hash-tutor", "tutor-dev");

    MockMultipartFile file =
        csv(
            "id,name,collegeName\n"
                + "1001,Student Updated,计算机学院\n"
                + "5001,Student New,计算机学院\n");

    mvc.perform(
            multipart("/users/import")
                .file(file)
                .param("userType", "STUDENT")
                .header("Authorization", "Bearer " + tutor.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(1))
        .andExpect(jsonPath("$.updated").value(1))
        .andExpect(jsonPath("$.failed").value(0))
        .andExpect(jsonPath("$.failures").isArray());

    assertThat(db.countUsersByTypeAndNo(UserType.TEACHER, "5001")).isEqualTo(1);
    assertThat(db.countUsersByTypeAndNo(UserType.STUDENT, "5001")).isEqualTo(1);
    assertThat(db.findUserName(UserType.STUDENT, "1001")).isEqualTo("Student Updated");
  }

  @Test
  void tutorImportTeacherForbidden() throws Exception {
    TokenPair tutor = login("TEACHER", "1001", "hash-tutor", "tutor-dev");

    mvc.perform(
            multipart("/users/import")
                .file(csv("1001,Teacher X,计算机学院\n"))
                .param("userType", "TEACHER")
                .header("Authorization", "Bearer " + tutor.accessToken()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
  }

  @Test
  void adminCollegeImportTeacherSameCollegeOkCrossCollegeRowFails() throws Exception {
    TokenPair adminCollege = login("TEACHER", "2001", "hash-admin-college", "a-dev");

    MockMultipartFile file =
        csv(
            "id,name,collegeName\n"
                + "1001,Tutor Renamed,计算机学院\n"
                + "6001,Teacher New,计算机学院\n"
                + "3001,Teacher Other,外国语学院\n");

    mvc.perform(
            multipart("/users/import")
                .file(file)
                .param("userType", "TEACHER")
                .header("Authorization", "Bearer " + adminCollege.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(1))
        .andExpect(jsonPath("$.updated").value(1))
        .andExpect(jsonPath("$.failed").value(1))
        .andExpect(jsonPath("$.failures[0].row").value(4))
        .andExpect(jsonPath("$.failures[0].reason").value(org.hamcrest.Matchers.containsString("mismatch")));

    assertThat(db.findUserName(UserType.TEACHER, "1001")).isEqualTo("Tutor Renamed");
    assertThat(db.countUsersByTypeAndNo(UserType.TEACHER, "6001")).isEqualTo(1);
    assertThat(db.countUsersByTypeAndNo(UserType.TEACHER, "3001")).isEqualTo(0);
  }

  @Test
  void adminSchoolImportCanAutoCreateCollege() throws Exception {
    TokenPair adminSchool = login("TEACHER", "4001", "hash-admin-school", "s-dev");

    mvc.perform(
            multipart("/users/import")
                .file(csv("8001,Teacher New,新学院\n"))
                .param("userType", "TEACHER")
                .header("Authorization", "Bearer " + adminSchool.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(1))
        .andExpect(jsonPath("$.updated").value(0))
        .andExpect(jsonPath("$.failed").value(0));

    Long collegeId = db.findCollegeIdByName("新学院");
    assertThat(collegeId).isNotNull();
    assertThat(db.findUserCollegeId(UserType.TEACHER, "8001")).isEqualTo(collegeId);
  }

  @Test
  void csvHeaderOrNoHeaderAndMissingOrBlankFieldsGoToFailures() throws Exception {
    TokenPair tutor = login("TEACHER", "1001", "hash-tutor", "tutor-dev");

    mvc.perform(
            multipart("/users/import")
                .file(
                    csv(
                        "id,name,collegeName\n"
                            + "9001,Ok,计算机学院\n"
                            + "9002,,计算机学院\n"
                            + "9003,Ok,\n"
                            + "9004,Ok\n"))
                .param("userType", "STUDENT")
                .header("Authorization", "Bearer " + tutor.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(1))
        .andExpect(jsonPath("$.updated").value(0))
        .andExpect(jsonPath("$.failed").value(3))
        .andExpect(jsonPath("$.failures[0].row").value(3))
        .andExpect(jsonPath("$.failures[1].row").value(4))
        .andExpect(jsonPath("$.failures[2].row").value(5));

    mvc.perform(
            multipart("/users/import")
                .file(
                    csv(
                        "9101,Ok,计算机学院\n"
                            + ",NoId,计算机学院\n"
                            + "9103,Ok,外国语学院\n"))
                .param("userType", "STUDENT")
                .header("Authorization", "Bearer " + tutor.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(1))
        .andExpect(jsonPath("$.updated").value(0))
        .andExpect(jsonPath("$.failed").value(2))
        .andExpect(jsonPath("$.failures[0].row").value(2))
        .andExpect(jsonPath("$.failures[1].row").value(3));
  }

  private TokenPair login(String userType, String id, String clientHash, String deviceId) throws Exception {
    MvcResult res =
        mvc.perform(
                post("/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(
                        "{"
                            + "\"userType\":\""
                            + userType
                            + "\","
                            + "\"id\":\""
                            + id
                            + "\","
                            + "\"clientSalt\":\"salt\","
                            + "\"clientHash\":\""
                            + clientHash
                            + "\","
                            + "\"deviceId\":\""
                            + deviceId
                            + "\""
                            + "}"))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode n = om.readTree(res.getResponse().getContentAsString());
    return new TokenPair(
        n.get("accessToken").asText(),
        n.get("refreshToken").asText(),
        java.time.Instant.parse(n.get("accessExpiresAt").asText()),
        java.time.Instant.parse(n.get("refreshExpiresAt").asText()));
  }

  private static MockMultipartFile csv(String content) {
    return new MockMultipartFile("file", "users.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
  }
}

