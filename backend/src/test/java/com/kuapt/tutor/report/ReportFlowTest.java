package com.kuapt.tutor.report;

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
class ReportFlowTest {
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
  private long roleAdminSchoolId;

  private long tutorAUserId;
  private long tutorBUserId;
  private long adminCollegeUserId;
  private long adminSchoolUserId;

  private long studentAUserId;
  private long studentBUserId;

  private long classAId;
  private long classBId;

  @BeforeEach
  void setup() throws Exception {
    db = new TestDb(dataSource, TEST_PEPPER);
    db.wipeAll();

    college1 = db.insertCollege("计算机学院");
    college2 = db.insertCollege("外国语学院");

    roleTutorId = db.insertRole("TUTOR", "导师");
    roleAdminCollegeId = db.insertRole("ADMIN_COLLEGE", "学院管理员");
    roleAdminSchoolId = db.insertRole("ADMIN_SCHOOL", "校级管理员");

    tutorAUserId = db.insertUser(UserType.TEACHER, "1001", "Tutor A", college1);
    db.grantRole(tutorAUserId, roleTutorId);
    db.upsertCredential(tutorAUserId, "salt", "hash-tutor-a");

    tutorBUserId = db.insertUser(UserType.TEACHER, "1002", "Tutor B", college2);
    db.grantRole(tutorBUserId, roleTutorId);
    db.upsertCredential(tutorBUserId, "salt", "hash-tutor-b");

    adminCollegeUserId = db.insertUser(UserType.TEACHER, "1100", "Admin College", college1);
    db.grantRole(adminCollegeUserId, roleAdminCollegeId);
    db.upsertCredential(adminCollegeUserId, "salt", "hash-admin-college");

    adminSchoolUserId = db.insertUser(UserType.TEACHER, "1200", "Admin School", null);
    db.grantRole(adminSchoolUserId, roleAdminSchoolId);
    db.upsertCredential(adminSchoolUserId, "salt", "hash-admin-school");

    studentAUserId = db.insertUser(UserType.STUDENT, "2001", "Student A", college1);
    db.upsertCredential(studentAUserId, "salt", "hash-student-a");

    studentBUserId = db.insertUser(UserType.STUDENT, "2002", "Student B", college2);
    db.upsertCredential(studentBUserId, "salt", "hash-student-b");

    classAId = db.insertClass("2026-02-23-1", "Class A", college1);
    db.assignTutor(classAId, tutorAUserId);
    db.enrollStudent(classAId, studentAUserId);

    classBId = db.insertClass("2026-02-23-1", "Class B", college2);
    db.assignTutor(classBId, tutorBUserId);
    db.enrollStudent(classBId, studentBUserId);

    // plans: one USER plan for studentA with 2 items (1 done); one CLASS plan for classA with 2 items (2 done)
    long studentPlanId = db.insertPlan("USER", studentAUserId, null, "2026-02-23-1", "SP");
    db.insertPlanItem(studentPlanId, "i1", "done");
    db.insertPlanItem(studentPlanId, "i2", "todo");

    long classPlanId = db.insertPlan("CLASS", null, classAId, "2026-02-23-1", "CP");
    db.insertPlanItem(classPlanId, "c1", "done");
    db.insertPlanItem(classPlanId, "c2", "done");

    // plans in other college
    long classPlanB = db.insertPlan("CLASS", null, classBId, "2026-02-23-1", "CPB");
    db.insertPlanItem(classPlanB, "b1", "todo");

    // activities
    long extraStudentAUserId = db.insertUser(UserType.STUDENT, "2999", "Student X", college1);
    db.upsertCredential(extraStudentAUserId, "salt", "hash-student-x");

    long actA = db.insertActivity(classAId, "2026-02-23-1", "A1", "PUBLISHED", tutorAUserId);
    db.insertActivitySignup(actA, studentAUserId, "APPROVED");
    db.insertActivitySignup(actA, extraStudentAUserId, "APPLIED");

    long actB = db.insertActivity(classBId, "2026-02-23-1", "B1", "PUBLISHED", tutorBUserId);
    db.insertActivitySignup(actB, studentBUserId, "APPLIED");
  }

  @Test
  void reportsRejectInvalidTerm_422() throws Exception {
    TokenPair adminSchool = login("TEACHER", "1200", "hash-admin-school", "as");

    mvc.perform(get("/reports/plan-completion").queryParam("term", "bad")
            .header("Authorization", "Bearer " + adminSchool.accessToken()))
        .andExpect(status().isUnprocessableEntity());

    mvc.perform(get("/reports/activity-stats").queryParam("term", "bad")
            .header("Authorization", "Bearer " + adminSchool.accessToken()))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void studentCannotAccessReports_403() throws Exception {
    TokenPair studentA = login("STUDENT", "2001", "hash-student-a", "s-a");

    mvc.perform(get("/reports/plan-completion").queryParam("term", "2026-02-23-1")
            .header("Authorization", "Bearer " + studentA.accessToken()))
        .andExpect(status().isForbidden());

    mvc.perform(get("/reports/activity-stats").queryParam("term", "2026-02-23-1")
            .header("Authorization", "Bearer " + studentA.accessToken()))
        .andExpect(status().isForbidden());
  }

  @Test
  void planCompletion_scopeEnforced_adminCollegeCannotQueryOtherCollege() throws Exception {
    TokenPair adminCollege = login("TEACHER", "1100", "hash-admin-college", "ac");

    mvc.perform(get("/reports/plan-completion")
            .queryParam("term", "2026-02-23-1")
            .queryParam("collegeId", String.valueOf(college2))
            .header("Authorization", "Bearer " + adminCollege.accessToken()))
        .andExpect(status().isForbidden());

    mvc.perform(get("/reports/plan-completion")
            .queryParam("term", "2026-02-23-1")
            .header("Authorization", "Bearer " + adminCollege.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].scope").value("学院:计算机学院"));
  }

  @Test
  void planCompletion_adminSchoolCanFilterCollegeId() throws Exception {
    TokenPair adminSchool = login("TEACHER", "1200", "hash-admin-school", "as");

    mvc.perform(get("/reports/plan-completion")
            .queryParam("term", "2026-02-23-1")
            .queryParam("collegeId", String.valueOf(college1))
            .header("Authorization", "Bearer " + adminSchool.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].scope").value("学院:计算机学院"))
        .andExpect(jsonPath("$.items[0].totalCount").value(4))
        .andExpect(jsonPath("$.items[0].doneCount").value(3));
  }

  @Test
  void activityStats_scopedForTutorAndCollegeAndSchool() throws Exception {
    TokenPair tutorA = login("TEACHER", "1001", "hash-tutor-a", "t-a");
    mvc.perform(get("/reports/activity-stats")
            .queryParam("term", "2026-02-23-1")
            .header("Authorization", "Bearer " + tutorA.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].title").value("A1"))
        .andExpect(jsonPath("$.items[0].approvedCount").value(1));

    TokenPair adminCollege = login("TEACHER", "1100", "hash-admin-college", "ac");
    mvc.perform(get("/reports/activity-stats")
            .queryParam("term", "2026-02-23-1")
            .header("Authorization", "Bearer " + adminCollege.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1));

    TokenPair adminSchool = login("TEACHER", "1200", "hash-admin-school", "as");
    mvc.perform(get("/reports/activity-stats")
            .queryParam("term", "2026-02-23-1")
            .header("Authorization", "Bearer " + adminSchool.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2));
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
