package com.ai.exam.service;

import com.ai.exam.entity.ExamRecord;
import com.ai.exam.vo.ExamRankingVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ExamRecordService extends IService<ExamRecord> {
    List<ExamRankingVO> getExamRankingOptimized(Integer paperId, Integer limit);

}
