package com.kuapt.tutor.evaluation;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuapt.tutor.auth.TestDb;
import com.kuapt.tutor.auth.TokenPair;
import com.kuapt.tutor.model.UserType;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EvaluationFlowTest {
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

  private long college1;
  private long college2;
  private long roleTutorId;
  private long roleAdminCollegeId;

  private long tutorAUserId;
  private long tutorBUserId;
  private long adminCollegeUserId;

  private long studentAUserId;
  private long studentBUserId;

  @BeforeEach
  void setup() throws Exception {
    db = new TestDb(dataSource, TEST_PEPPER);
    db.wipeAll();

    college1 = db.insertCollege("计算机学院");
    college2 = db.insertCollege("外国语学院");

    roleTutorId = db.insertRole("TUTOR", "导师");
    roleAdminCollegeId = db.insertRole("ADMIN_COLLEGE", "学院管理员");

    tutorAUserId = db.insertUser(UserType.TEACHER, "1001", "Tutor A", college1);
    db.grantRole(tutorAUserId, roleTutorId);
    db.upsertCredential(tutorAUserId, "salt", "hash-tutor-a");

    tutorBUserId = db.insertUser(UserType.TEACHER, "1002", "Tutor B", college2);
    db.grantRole(tutorBUserId, roleTutorId);
    db.upsertCredential(tutorBUserId, "salt", "hash-tutor-b");

    adminCollegeUserId = db.insertUser(UserType.TEACHER, "1100", "Admin College", college1);
    db.grantRole(adminCollegeUserId, roleAdminCollegeId);
    db.upsertCredential(adminCollegeUserId, "salt", "hash-admin-college");

    studentAUserId = db.insertUser(UserType.STUDENT, "2001", "Student A", college1);
    db.upsertCredential(studentAUserId, "salt", "hash-student-a");

    studentBUserId = db.insertUser(UserType.STUDENT, "2002", "Student B", college2);
    db.upsertCredential(studentBUserId, "salt", "hash-student-b");

    long classAId = db.insertClass("2026-02-23-1", "Class A", college1);
    db.assignTutor(classAId, tutorAUserId);
    db.enrollStudent(classAId, studentAUserId);

    long classBId = db.insertClass("2026-02-23-1", "Class B", college2);
    db.assignTutor(classBId, tutorBUserId);
    db.enrollStudent(classBId, studentBUserId);
  }

  @Test
  void studentCannotCreateEvaluation_403() throws Exception {
    TokenPair studentA = login("STUDENT", "2001", "hash-student-a", "s-a");

    mvc.perform(
            post("/evaluations")
                .header("Authorization", "Bearer " + studentA.accessToken())
                .contentType(APPLICATION_JSON)
                .content(
                    "{" +
                        "\"evaluateeUserId\":" + studentAUserId + "," +
                        "\"term\":\"2026-02-23-1\"," +
                        "\"scoreTotal\":80" +
                        "}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void tutorCannotEvaluateOutsideClass_403() throws Exception {
    TokenPair tutorA = login("TEACHER", "1001", "hash-tutor-a", "t-a");

    mvc.perform(
            post("/evaluations")
                .header("Authorization", "Bearer " + tutorA.accessToken())
                .contentType(APPLICATION_JSON)
                .content(
                    "{" +
                        "\"evaluateeUserId\":" + studentBUserId + "," +
                        "\"term\":\"2026-02-23-1\"," +
                        "\"scoreTotal\":80" +
                        "}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCollegeCannotReadOtherCollegeEvaluation_404() throws Exception {
    long evalId = db.insertEvaluation(tutorBUserId, studentBUserId, "2026-02-23-1", 90, null);
    db.insertEvaluationDetail(evalId, "attitude", 10, "ok");

    TokenPair adminCollege = login("TEACHER", "1100", "hash-admin-college", "ac");

    mvc.perform(get("/evaluations/" + evalId).header("Authorization", "Bearer " + adminCollege.accessToken()))
        .andExpect(status().isNotFound());
  }

  @Test
  void studentCanReadOnlyOwnEvaluation_otherIs404() throws Exception {
    long evalForA = db.insertEvaluation(tutorAUserId, studentAUserId, "2026-02-23-1", 88, "good");
    db.insertEvaluationDetail(evalForA, "attitude", 10, "ok");

    long evalForB = db.insertEvaluation(tutorBUserId, studentBUserId, "2026-02-23-1", 66, null);

    TokenPair studentA = login("STUDENT", "2001", "hash-student-a", "s-a");

    mvc.perform(get("/evaluations/" + evalForA).header("Authorization", "Bearer " + studentA.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value((int) evalForA))
        .andExpect(jsonPath("$.details[0].itemKey").value("attitude"));

    mvc.perform(get("/evaluations/" + evalForB).header("Authorization", "Bearer " + studentA.accessToken()))
        .andExpect(status().isNotFound());
  }

  @Test
  void listEvaluations_studentScopedBySelf_teacherScopedByRole() throws Exception {
    db.insertEvaluation(tutorAUserId, studentAUserId, "2026-02-23-1", 88, "good");
    db.insertEvaluation(tutorBUserId, studentBUserId, "2026-02-23-1", 66, null);

    TokenPair studentA = login("STUDENT", "2001", "hash-student-a", "s-a");
    mvc.perform(get("/evaluations").header("Authorization", "Bearer " + studentA.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1));

    TokenPair tutorA = login("TEACHER", "1001", "hash-tutor-a", "t-a");
    mvc.perform(get("/evaluations").header("Authorization", "Bearer " + tutorA.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1));

    TokenPair adminCollege = login("TEACHER", "1100", "hash-admin-college", "ac");
    mvc.perform(get("/evaluations").header("Authorization", "Bearer " + adminCollege.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1));
  }

  private TokenPair login(String userType, String id, String clientHash, String deviceId) throws Exception {
    String body =
        "{" +
            "\"userType\":\"" + userType + "\"," +
            "\"id\":\"" + id + "\"," +
            "\"clientSalt\":\"salt\"," +
            "\"clientHash\":\"" + clientHash + "\"," +
            "\"deviceId\":\"" + deviceId + "\"" +
            "}";

    String content =
        mvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return om.readValue(content, TokenPair.class);
  }
}
