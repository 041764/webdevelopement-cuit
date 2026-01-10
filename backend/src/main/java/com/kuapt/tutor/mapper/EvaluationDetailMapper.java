package com.kuapt.tutor.mapper;

import com.kuapt.tutor.model.EvaluationDetailItemRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EvaluationDetailMapper {
  @Select("SELECT item_key AS itemKey, score, comment FROM evaluation_detail WHERE evaluation_id = #{evaluationId} ORDER BY id ASC")
  List<EvaluationDetailItemRecord> listByEvaluationId(@Param("evaluationId") long evaluationId);

  @Insert("INSERT INTO evaluation_detail(evaluation_id, item_key, score, comment) VALUES(#{evaluationId}, #{itemKey}, #{score}, #{comment})")
  int insert(@Param("evaluationId") long evaluationId, @Param("itemKey") String itemKey, @Param("score") int score, @Param("comment") String comment);
}
