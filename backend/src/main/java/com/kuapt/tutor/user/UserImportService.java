package com.kuapt.tutor.user;

import com.kuapt.tutor.auth.AuthErrorCode;
import com.kuapt.tutor.exception.ApiException;
import com.kuapt.tutor.mapper.CollegeMapper;
import com.kuapt.tutor.mapper.UserMapper;
import com.kuapt.tutor.model.UserRecord;
import com.kuapt.tutor.model.UserType;
import com.kuapt.tutor.user.dto.UserImportResponse;
import com.kuapt.tutor.user.dto.UserImportResponse.Failure;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserImportService {
  private final UserMapper userMapper;
  private final CollegeMapper collegeMapper;

  public UserImportService(UserMapper userMapper, CollegeMapper collegeMapper) {
    this.userMapper = userMapper;
    this.collegeMapper = collegeMapper;
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
      for (CSVRecord rec : parser) {
        long row = rec.getRecordNumber();
        if (first && looksLikeHeader(rec)) {
          first = false;
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
        if (existing == null) {
          userMapper.insertUser(userType, userNo, name, collegeId);
          created++;
        } else {
          userMapper.updateUser(userType, userNo, name, collegeId);
          updated++;
        }
      }
    } catch (IOException e) {
      throw new ApiException(AuthErrorCode.VALIDATION_ERROR, "failed to read csv");
    }

    return new UserImportResponse(created, updated, failures.size(), failures);
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
