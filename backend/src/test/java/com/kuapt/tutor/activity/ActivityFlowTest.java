package com.kuapt.tutor.activity;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ActivityFlowTest {
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

  private long tutorAUserId;
  private long tutorBUserId;
  private long student1UserId;
  private long student2UserId;
  private long student3UserId;
  private long classAId;
  private long classBId;

  @BeforeEach
  void setup() throws Exception {
    db = new TestDb(dataSource, TEST_PEPPER);
    db.wipeAll();

    long college1 = db.insertCollege("计算机学院");

    long roleTutorId = db.insertRole("TUTOR", "导师");

    tutorAUserId = db.insertUser(UserType.TEACHER, "1001", "Tutor A", college1);
    db.grantRole(tutorAUserId, roleTutorId);
    db.upsertCredential(tutorAUserId, "salt", "hash-tutor-a");

    tutorBUserId = db.insertUser(UserType.TEACHER, "1002", "Tutor B", college1);
    db.grantRole(tutorBUserId, roleTutorId);
    db.upsertCredential(tutorBUserId, "salt", "hash-tutor-b");

    student1UserId = db.insertUser(UserType.STUDENT, "2001", "Student 1", college1);
    db.upsertCredential(student1UserId, "salt", "hash-student-1");

    student2UserId = db.insertUser(UserType.STUDENT, "2002", "Student 2", college1);
    db.upsertCredential(student2UserId, "salt", "hash-student-2");

    student3UserId = db.insertUser(UserType.STUDENT, "2003", "Student 3", college1);
    db.upsertCredential(student3UserId, "salt", "hash-student-3");

    classAId = db.insertClass("2026-02-23-1", "Class A", college1);
    db.assignTutor(classAId, tutorAUserId);
    db.enrollStudent(classAId, student1UserId);
    db.enrollStudent(classAId, student2UserId);

    classBId = db.insertClass("2026-02-23-1", "Class B", college1);
    db.assignTutor(classBId, tutorBUserId);
    db.enrollStudent(classBId, student3UserId);
  }

  @Test
  void tutorCreatePublishStudentAppliedThenApproveOk() throws Exception {
    TokenPair tutorA = login("TEACHER", "1001", "hash-tutor-a", "tutor-a");
    long activityId = createActivity(tutorA.accessToken(), classAId, true, 10);
    publish(tutorA.accessToken(), activityId);

    TokenPair student1 = login("STUDENT", "2001", "hash-student-1", "s1");
    long signupId = signup(student1.accessToken(), activityId, "note").get("id").asLong();

    mvc.perform(post("/activities/" + activityId + "/signups/" + signupId + "/approve")
            .header("Authorization", "Bearer " + tutorA.accessToken()))
        .andExpect(status().isNoContent());

    mvc.perform(get("/activities/" + activityId + "/signups")
            .param("page", "1")
            .param("size", "20")
            .param("status", "APPROVED")
            .header("Authorization", "Bearer " + tutorA.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.items[0].status").value("APPROVED"));
  }

  @Test
  void requiresReviewFalseCapacityOneSecondSignupConflict() throws Exception {
    TokenPair tutorA = login("TEACHER", "1001", "hash-tutor-a", "tutor-a");
    long activityId = createActivity(tutorA.accessToken(), classAId, false, 1);
    publish(tutorA.accessToken(), activityId);

    TokenPair student1 = login("STUDENT", "2001", "hash-student-1", "s1");
    mvc.perform(post("/activities/" + activityId + "/signups")
            .header("Authorization", "Bearer " + student1.accessToken())
            .contentType(APPLICATION_JSON)
            .content("{\"note\":\"a\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("APPROVED"));

    TokenPair student2 = login("STUDENT", "2002", "hash-student-2", "s2");
    mvc.perform(post("/activities/" + activityId + "/signups")
            .header("Authorization", "Bearer " + student2.accessToken())
            .contentType(APPLICATION_JSON)
            .content("{\"note\":\"b\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void studentNotInClassCannotSignupForbidden() throws Exception {
    TokenPair tutorA = login("TEACHER", "1001", "hash-tutor-a", "tutor-a");
    long activityId = createActivity(tutorA.accessToken(), classAId, true, null);
    publish(tutorA.accessToken(), activityId);

    TokenPair student3 = login("STUDENT", "2003", "hash-student-3", "s3");
    mvc.perform(post("/activities/" + activityId + "/signups")
            .header("Authorization", "Bearer " + student3.accessToken())
            .contentType(APPLICATION_JSON)
            .content("{\"note\":\"x\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
  }

  @Test
  void tutorCannotPublishOrApproveOtherClassActivityForbidden() throws Exception {
    TokenPair tutorB = login("TEACHER", "1002", "hash-tutor-b", "tutor-b");
    long activityId = createActivity(tutorB.accessToken(), classBId, true, null);

    TokenPair tutorA = login("TEACHER", "1001", "hash-tutor-a", "tutor-a");
    mvc.perform(post("/activities/" + activityId + "/publish")
            .header("Authorization", "Bearer " + tutorA.accessToken()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));

    publish(tutorB.accessToken(), activityId);

    TokenPair student3 = login("STUDENT", "2003", "hash-student-3", "s3");
    long signupId = signup(student3.accessToken(), activityId, "note").get("id").asLong();

    mvc.perform(post("/activities/" + activityId + "/signups/" + signupId + "/approve")
            .header("Authorization", "Bearer " + tutorA.accessToken()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
  }

  @Test
  void cancelSignupMeAndMissingReturns404() throws Exception {
    TokenPair tutorA = login("TEACHER", "1001", "hash-tutor-a", "tutor-a");
    long activityId = createActivity(tutorA.accessToken(), classAId, true, null);
    publish(tutorA.accessToken(), activityId);

    TokenPair student1 = login("STUDENT", "2001", "hash-student-1", "s1");
    signup(student1.accessToken(), activityId, "note");

    mvc.perform(delete("/activities/" + activityId + "/signups/me")
            .header("Authorization", "Bearer " + student1.accessToken()))
        .andExpect(status().isNoContent());

    mvc.perform(get("/activities/" + activityId + "/signups")
            .param("status", "CANCELED")
            .header("Authorization", "Bearer " + tutorA.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.items[0].status").value("CANCELED"));

    TokenPair student2 = login("STUDENT", "2002", "hash-student-2", "s2");
    mvc.perform(delete("/activities/" + activityId + "/signups/me")
            .header("Authorization", "Bearer " + student2.accessToken()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void capacityFullApproveConflict() throws Exception {
    TokenPair tutorA = login("TEACHER", "1001", "hash-tutor-a", "tutor-a");
    long activityId = createActivity(tutorA.accessToken(), classAId, true, 1);
    publish(tutorA.accessToken(), activityId);

    TokenPair student1 = login("STUDENT", "2001", "hash-student-1", "s1");
    TokenPair student2 = login("STUDENT", "2002", "hash-student-2", "s2");
    long signup1 = signup(student1.accessToken(), activityId, "a").get("id").asLong();
    long signup2 = signup(student2.accessToken(), activityId, "b").get("id").asLong();

    mvc.perform(post("/activities/" + activityId + "/signups/" + signup1 + "/approve")
            .header("Authorization", "Bearer " + tutorA.accessToken()))
        .andExpect(status().isNoContent());

    mvc.perform(post("/activities/" + activityId + "/signups/" + signup2 + "/approve")
            .header("Authorization", "Bearer " + tutorA.accessToken()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  private long createActivity(String accessToken, long classId, boolean requiresReview, Integer capacity) throws Exception {
    String cap = capacity == null ? "null" : capacity.toString();
    MvcResult res =
        mvc.perform(post("/activities")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(APPLICATION_JSON)
                .content(
                    "{"
                        + "\"classId\":"
                        + classId
                        + ","
                        + "\"term\":\"2026-02-23-1\","
                        + "\"title\":\"T\","
                        + "\"description\":\"D\","
                        + "\"capacity\":"
                        + cap
                        + ","
                        + "\"requiresReview\":"
                        + requiresReview
                        + "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn();
    return om.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  private void publish(String accessToken, long activityId) throws Exception {
    mvc.perform(post("/activities/" + activityId + "/publish")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isNoContent());
  }

  private JsonNode signup(String accessToken, long activityId, String note) throws Exception {
    MvcResult res =
        mvc.perform(post("/activities/" + activityId + "/signups")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(APPLICATION_JSON)
                .content("{\"note\":\"" + note + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return om.readTree(res.getResponse().getContentAsString());
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
}

