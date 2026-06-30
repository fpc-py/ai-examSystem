package com.ai.exam.mapper;

import com.ai.exam.entity.Banner;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BannerMapper extends BaseMapper<Banner> {

    @Select("SELECT * FROM banners WHERE is_active = true ORDER BY sort_order ASC ")
    List<Banner> selectActiveBanner();

}
