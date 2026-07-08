package com.ai.exam.service;

import com.ai.exam.common.Result;
import com.ai.exam.entity.Notice;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface NoticeService extends IService<Notice> {
    Result<List<Notice>> getActiveNotices();


    Result<List<Notice>> getLatestNotices(int limit);

    Result<List<Notice>> getAllNotices();


    Result<String> addNotice(Notice notice);

    Result<String> updateNotice(Notice notice);

    Result<String> deleteNotice(Long id);

    Result<String> toggleNoticeStatus(Long id, Boolean isActive);
}
