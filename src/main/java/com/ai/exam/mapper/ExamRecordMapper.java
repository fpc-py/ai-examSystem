package com.ai.exam.mapper;

import com.ai.exam.entity.ExamRecord;
import com.ai.exam.vo.ExamRankingVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {
    List<ExamRankingVO> selectExamRanking(Integer paperId, Integer limit);
}
