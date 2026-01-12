package com.kuapt.tutor.user;

import com.kuapt.tutor.auth.AuthErrorCode;
import com.kuapt.tutor.auth.PasswordProperties;
import com.kuapt.tutor.exception.ApiException;
import com.kuapt.tutor.mapper.ClassMapper;
import com.kuapt.tutor.mapper.ClassStudentMapper;
import com.kuapt.tutor.mapper.CollegeMapper;
import com.kuapt.tutor.mapper.LocalCredentialMapper;
import com.kuapt.tutor.mapper.UserMapper;
import com.kuapt.tutor.model.LocalCredentialRecord;
import com.kuapt.tutor.model.UserRecord;
import com.kuapt.tutor.model.UserType;
import com.kuapt.tutor.user.dto.UserImportResponse;
import com.kuapt.tutor.user.dto.UserImportResponse.Failure;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserImportService {
  private static final String DEFAULT_TERM = "2026-02-23-1";
  private static final String DEFAULT_STUDENT_PASSWORD = "Student@123";
  private static final String DEFAULT_TEACHER_PASSWORD = "Teacher@123";
  
  private final UserMapper userMapper;
  private final CollegeMapper collegeMapper;
  private final ClassMapper classMapper;
  private final ClassStudentMapper classStudentMapper;
  private final LocalCredentialMapper credentialMapper;
  private final PasswordProperties passwordProps;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public UserImportService(UserMapper userMapper, CollegeMapper collegeMapper, 
                           ClassMapper classMapper, ClassStudentMapper classStudentMapper,
                           LocalCredentialMapper credentialMapper, PasswordProperties passwordProps) {
    this.userMapper = userMapper;
    this.collegeMapper = collegeMapper;
    this.classMapper = classMapper;
    this.classStudentMapper = classStudentMapper;
    this.credentialMapper = credentialMapper;
    this.passwordProps = passwordProps;
  }

  public UserImportResponse importUsers(UserType userType, MultipartFile file, String restrictedCollegeName) {
    if (file == null || file.isEmpty()) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "file is required");
    }

    int created = 0;
    int updated = 0;
    List<Failure> failures = new ArrayList<>();

    try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
        CSVParser parser =
            CSVFormat.DEFAULT.builder().setTrim(true).setIgnoreEmptyLines(false).build().parse(reader)) {
      boolean first = true;
      boolean hasClassColumn = false;
      
      for (CSVRecord rec : parser) {
        long row = rec.getRecordNumber();
        if (first && looksLikeHeader(rec)) {
          first = false;
          hasClassColumn = rec.size() >= 4 && "className".equalsIgnoreCase(safeTrim(rec.get(3)));
          continue;
        }
        first = false;

        if (rec.size() < 3) {
          failures.add(new Failure(row, "missing fields: id,name,collegeName"));
          continue;
        }

        String userNo = safeTrim(rec.get(0));
        String name = safeTrim(rec.get(1));
        String collegeName = safeTrim(rec.get(2));
        String className = rec.size() >= 4 ? safeTrim(rec.get(3)) : "";

        if (userNo.isEmpty()) {
          failures.add(new Failure(row, "id is blank"));
          continue;
        }
        if (name.isEmpty()) {
          failures.add(new Failure(row, "name is blank"));
          continue;
        }
        if (collegeName.isEmpty()) {
          failures.add(new Failure(row, "collegeName is blank"));
          continue;
        }

        if (restrictedCollegeName != null && !restrictedCollegeName.equals(collegeName)) {
          failures.add(
              new Failure(
                  row,
                  "collegeName mismatch: requester college is '" + restrictedCollegeName + "', got '" + collegeName + "'"));
          continue;
        }

        Long collegeId = ensureCollegeId(collegeName);
        if (collegeId == null) {
          failures.add(new Failure(row, "failed to ensure college: '" + collegeName + "'"));
          continue;
        }

        UserRecord existing = userMapper.findByTypeAndNo(userType, userNo);
        long userId;
        if (existing == null) {
          userMapper.insertUser(userType, userNo, name, collegeId);
          UserRecord newUser = userMapper.findByTypeAndNo(userType, userNo);
          userId = newUser.userId();
          created++;
        } else {
          userMapper.updateUser(userType, userNo, name, collegeId);
          userId = existing.userId();
          updated++;
        }

        // 为新创建的用户设置默认密码（如果没有密码）
        LocalCredentialRecord existingCred = credentialMapper.findByUserId(userId);
        if (existingCred == null) {
          String defaultPassword = userType == UserType.STUDENT ? DEFAULT_STUDENT_PASSWORD : DEFAULT_TEACHER_PASSWORD;
          setDefaultPassword(userId, userType, userNo, defaultPassword);
        }

        // 如果是学生且有班级列，关联到班级
        if (userType == UserType.STUDENT && !className.isEmpty()) {
          Long classId = ensureClassId(className, collegeId);
          if (classId != null) {
            classStudentMapper.insertIfNotExists(classId, userId);
          }
        }
      }
    } catch (IOException e) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "failed to read csv");
    }

    return new UserImportResponse(created, updated, failures.size(), failures);
  }

  private void setDefaultPassword(long userId, UserType userType, String userNo, String password) {
    try {
      // 模拟前端的密码哈希逻辑
      String clientSalt = sha256Base64Url(userType.name() + ":" + userNo);
      String clientHash = sha256Base64Url(clientSalt + ":" + password);
      String combined = passwordProps.pepper() + ":" + clientHash;
      String serverHash = passwordEncoder.encode(combined);
      credentialMapper.insert(userId, clientSalt, clientHash, serverHash);
    } catch (Exception e) {
      // 忽略密码设置失败，用户可以后续通过重置密码功能设置
    }
  }

  private static String sha256Base64Url(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private Long ensureCollegeId(String collegeName) {
    Long id = collegeMapper.findIdByName(collegeName);
    if (id != null) {
      return id;
    }
    try {
      collegeMapper.insert(collegeName);
    } catch (RuntimeException ignored) {
      // concurrent insert or other constraint, re-select below
    }
    return collegeMapper.findIdByName(collegeName);
  }

  private Long ensureClassId(String className, long collegeId) {
    Long id = classMapper.findIdByNameAndCollege(className, collegeId);
    if (id != null) {
      return id;
    }
    try {
      classMapper.insert(DEFAULT_TERM, className, collegeId);
    } catch (RuntimeException ignored) {
      // concurrent insert or other constraint, re-select below
    }
    return classMapper.findIdByNameAndCollege(className, collegeId);
  }

  private static boolean looksLikeHeader(CSVRecord rec) {
    if (rec.size() < 3) {
      return false;
    }
    return "id".equalsIgnoreCase(safeTrim(rec.get(0)))
        && "name".equalsIgnoreCase(safeTrim(rec.get(1)))
        && "collegeName".equalsIgnoreCase(safeTrim(rec.get(2)));
  }

  private static String safeTrim(String s) {
    if (s == null) {
      return "";
    }
    return s.trim();
  }
}
