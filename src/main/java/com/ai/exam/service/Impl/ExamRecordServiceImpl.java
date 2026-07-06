package com.ai.exam.service.Impl;

import com.ai.exam.entity.ExamRecord;
import com.ai.exam.mapper.ExamRecordMapper;
import com.ai.exam.service.ExamRecordService;
import com.ai.exam.vo.ExamRankingVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExamRecordServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamRecordService {
    @Override
    public List<ExamRankingVO> getExamRankingOptimized(Integer paperId, Integer limit) {
        return baseMapper.selectExamRanking(paperId, limit);
    }
}
