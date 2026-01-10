package com.kuapt.tutor.plan;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class PlanFlowTest {
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
  private long studentAUserId;
  private long studentBUserId;
  private long classAId;
  private long classBId;
  private long roleTutorId;

  @BeforeEach
  void setup() throws Exception {
    db = new TestDb(dataSource, TEST_PEPPER);
    db.wipeAll();

    long college1 = db.insertCollege("计算机学院");

    roleTutorId = db.insertRole("TUTOR", "导师");

    tutorAUserId = db.insertUser(UserType.TEACHER, "1001", "Tutor A", college1);
    db.grantRole(tutorAUserId, roleTutorId);
    db.upsertCredential(tutorAUserId, "salt", "hash-tutor-a");

    tutorBUserId = db.insertUser(UserType.TEACHER, "1002", "Tutor B", college1);
    db.grantRole(tutorBUserId, roleTutorId);
    db.upsertCredential(tutorBUserId, "salt", "hash-tutor-b");

    studentAUserId = db.insertUser(UserType.STUDENT, "2001", "Student A", college1);
    db.upsertCredential(studentAUserId, "salt", "hash-student-a");

    studentBUserId = db.insertUser(UserType.STUDENT, "2002", "Student B", college1);
    db.upsertCredential(studentBUserId, "salt", "hash-student-b");

    classAId = db.insertClass("2026-02-23-1", "Class A", college1);
    db.assignTutor(classAId, tutorAUserId);
    db.enrollStudent(classAId, studentAUserId);

    classBId = db.insertClass("2026-02-23-1", "Class B", college1);
    db.assignTutor(classBId, tutorBUserId);
    db.enrollStudent(classBId, studentBUserId);
  }

  @Test
  void a_studentPersonalPlanCreateItemPatchDoneProgressComputed() throws Exception {
    TokenPair studentA = login("STUDENT", "2001", "hash-student-a", "s-a");

    long planId =
        createPlan(
            studentA.accessToken(),
            "{"
                + "\"ownerType\":\"USER\","
                + "\"term\":\"2026-02-23-1\","
                + "\"title\":\"P\""
                + "}");

    long itemId = createItem(studentA.accessToken(), planId, "{\"title\":\"I\"}");

    mvc.perform(
            patch("/plans/" + planId + "/items/" + itemId)
                .header("Authorization", "Bearer " + studentA.accessToken())
                .contentType(APPLICATION_JSON)
                .content("{\"status\":\"done\"}"))
        .andExpect(status().isNoContent());

    mvc.perform(get("/plans/" + planId).header("Authorization", "Bearer " + studentA.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].status").value("done"))
        .andExpect(jsonPath("$.progress.planId").value((int) planId))
        .andExpect(jsonPath("$.progress.totalCount").value(1))
        .andExpect(jsonPath("$.progress.doneCount").value(1))
        .andExpect(jsonPath("$.progress.completionRate").value(1.0));
  }

  @Test
  void b_tutorCreateClassPlanStudentCanReadButCannotCreateItems() throws Exception {
    TokenPair tutorA = login("TEACHER", "1001", "hash-tutor-a", "t-a");
    long planId =
        createPlan(
            tutorA.accessToken(),
            "{"
                + "\"ownerType\":\"CLASS\","
                + "\"ownerClassId\":"
                + classAId
                + ","
                + "\"term\":\"2026-02-23-1\","
                + "\"title\":\"CP\""
                + "}");

    TokenPair studentA = login("STUDENT", "2001", "hash-student-a", "s-a");

    mvc.perform(get("/plans").header("Authorization", "Bearer " + studentA.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.items[0].id").value((int) planId))
        .andExpect(jsonPath("$.items[0].ownerType").value("CLASS"));

    mvc.perform(get("/plans/" + planId).header("Authorization", "Bearer " + studentA.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value((int) planId));

    mvc.perform(
            post("/plans/" + planId + "/items")
                .header("Authorization", "Bearer " + studentA.accessToken())
                .contentType(APPLICATION_JSON)
                .content("{\"title\":\"X\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
  }

  @Test
  void c_otherTutorCannotCreateOrModifyOtherClassPlan() throws Exception {
    TokenPair otherTutor = login("TEACHER", "1002", "hash-tutor-b", "t-b");

    mvc.perform(
            post("/plans")
                .header("Authorization", "Bearer " + otherTutor.accessToken())
                .contentType(APPLICATION_JSON)
                .content(
                    "{"
                        + "\"ownerType\":\"CLASS\","
                        + "\"ownerClassId\":"
                        + classAId
                        + ","
                        + "\"term\":\"2026-02-23-1\","
                        + "\"title\":\"CP\""
                        + "}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));

    TokenPair tutorA = login("TEACHER", "1001", "hash-tutor-a", "t-a");
    long planId =
        createPlan(
            tutorA.accessToken(),
            "{"
                + "\"ownerType\":\"CLASS\","
                + "\"ownerClassId\":"
                + classAId
                + ","
                + "\"term\":\"2026-02-23-1\","
                + "\"title\":\"CP\""
                + "}");

    mvc.perform(
            post("/plans/" + planId + "/items")
                .header("Authorization", "Bearer " + otherTutor.accessToken())
                .contentType(APPLICATION_JSON)
                .content("{\"title\":\"X\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
  }

  @Test
  void d_studentCannotCreateClassPlan() throws Exception {
    TokenPair studentA = login("STUDENT", "2001", "hash-student-a", "s-a");

    mvc.perform(
            post("/plans")
                .header("Authorization", "Bearer " + studentA.accessToken())
                .contentType(APPLICATION_JSON)
                .content(
                    "{"
                        + "\"ownerType\":\"CLASS\","
                        + "\"ownerClassId\":"
                        + classAId
                        + ","
                        + "\"term\":\"2026-02-23-1\","
                        + "\"title\":\"CP\""
                        + "}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
  }

  @Test
  void e_itemProgressPersonalOkClassOkCrossClassForbidden() throws Exception {
    TokenPair studentA = login("STUDENT", "2001", "hash-student-a", "s-a");
    long studentAId = meUserId(studentA.accessToken());

    long personalPlanId =
        createPlan(
            studentA.accessToken(),
            "{"
                + "\"ownerType\":\"USER\","
                + "\"term\":\"2026-02-23-1\","
                + "\"title\":\"P\""
                + "}");
    long personalItemId = createItem(studentA.accessToken(), personalPlanId, "{\"title\":\"I\"}");

    mvc.perform(
            post("/plan-items/" + personalItemId + "/progress")
                .header("Authorization", "Bearer " + studentA.accessToken())
                .contentType(APPLICATION_JSON)
                .content("{\"percent\":50,\"note\":\"n\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.planItemId").value((int) personalItemId))
        .andExpect(jsonPath("$.percent").value(50))
        .andExpect(jsonPath("$.createdByUserId").value((int) studentAId));

    TokenPair tutorA = login("TEACHER", "1001", "hash-tutor-a", "t-a");
    long classPlanId =
        createPlan(
            tutorA.accessToken(),
            "{"
                + "\"ownerType\":\"CLASS\","
                + "\"ownerClassId\":"
                + classAId
                + ","
                + "\"term\":\"2026-02-23-1\","
                + "\"title\":\"CP\""
                + "}");
    long classItemId = createItem(tutorA.accessToken(), classPlanId, "{\"title\":\"CI\"}");

    mvc.perform(
            post("/plan-items/" + classItemId + "/progress")
                .header("Authorization", "Bearer " + studentA.accessToken())
                .contentType(APPLICATION_JSON)
                .content("{\"percent\":10}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.planItemId").value((int) classItemId))
        .andExpect(jsonPath("$.percent").value(10))
        .andExpect(jsonPath("$.createdByUserId").value((int) studentAId));

    TokenPair studentB = login("STUDENT", "2002", "hash-student-b", "s-b");
    mvc.perform(
            post("/plan-items/" + classItemId + "/progress")
                .header("Authorization", "Bearer " + studentB.accessToken())
                .contentType(APPLICATION_JSON)
                .content("{\"percent\":20}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
  }

  @Test
  void f_deleteItemThenProgressReturns404() throws Exception {
    TokenPair studentA = login("STUDENT", "2001", "hash-student-a", "s-a");

    long planId =
        createPlan(
            studentA.accessToken(),
            "{"
                + "\"ownerType\":\"USER\","
                + "\"term\":\"2026-02-23-1\","
                + "\"title\":\"P\""
                + "}");
    long itemId = createItem(studentA.accessToken(), planId, "{\"title\":\"I\"}");

    mvc.perform(
            post("/plan-items/" + itemId + "/progress")
                .header("Authorization", "Bearer " + studentA.accessToken())
                .contentType(APPLICATION_JSON)
                .content("{\"percent\":1}"))
        .andExpect(status().isCreated());

    mvc.perform(delete("/plans/" + planId + "/items/" + itemId).header("Authorization", "Bearer " + studentA.accessToken()))
        .andExpect(status().isNoContent());

    mvc.perform(get("/plan-items/" + itemId + "/progress").header("Authorization", "Bearer " + studentA.accessToken()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  private long createPlan(String accessToken, String body) throws Exception {
    MvcResult res =
        mvc.perform(post("/plans").header("Authorization", "Bearer " + accessToken).contentType(APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn();
    return om.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  private long createItem(String accessToken, long planId, String body) throws Exception {
    MvcResult res =
        mvc.perform(
                post("/plans/" + planId + "/items")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn();
    return om.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  private long meUserId(String accessToken) throws Exception {
    MvcResult res = mvc.perform(get("/auth/me").header("Authorization", "Bearer " + accessToken)).andExpect(status().isOk()).andReturn();
    JsonNode n = om.readTree(res.getResponse().getContentAsString());
    return n.get("userId").asLong();
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

