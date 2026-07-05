package com.ai.exam.service;

import com.ai.exam.dto.AiPaperDTO;
import com.ai.exam.dto.PaperDTO;
import com.ai.exam.entity.Paper;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PaperService extends IService<Paper> {
    Paper createPaper(PaperDTO dto);

    Paper updatePaper(Integer id, PaperDTO dto);

    Paper createPaperWithAI(AiPaperDTO dto);

    Paper getPaperWithQuestions(Integer id);

    void updatePaperStatus(Integer id, String status);
}
