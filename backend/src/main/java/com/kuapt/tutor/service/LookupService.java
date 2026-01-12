package com.kuapt.tutor.service;

import com.kuapt.tutor.mapper.ClassMapper;
import com.kuapt.tutor.mapper.CollegeMapper;
import com.kuapt.tutor.mapper.UserMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LookupService {
  private final UserMapper userMapper;
  private final ClassMapper classMapper;
  private final CollegeMapper collegeMapper;

  public LookupService(UserMapper userMapper, ClassMapper classMapper, CollegeMapper collegeMapper) {
    this.userMapper = userMapper;
    this.classMapper = classMapper;
    this.collegeMapper = collegeMapper;
  }

  public record StudentOption(long id, String userNo, String name) {}
  
  public record ClassOption(long id, String name, String collegeName) {}
  
  public record CollegeOption(long id, String name) {}

  public List<StudentOption> listStudents(Long collegeId) {
    return userMapper.listStudents(collegeId);
  }

  public List<ClassOption> listClasses(Long collegeId, String term) {
    return classMapper.listClassOptions(collegeId, term);
  }

  public List<CollegeOption> listColleges() {
    return collegeMapper.listAll();
  }
}
