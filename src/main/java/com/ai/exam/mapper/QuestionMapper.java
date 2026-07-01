package com.ai.exam.mapper;

import com.ai.exam.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
    @Select("SELECT category_id,COUNT(*) as count FROM questions GROUP BY category_id")
    @Results({
            @Result(property = "categoryId", column = "category_id"),
            @Result(property = "count", column = "count")
    })
    List<Map<String, Object>> getCategoryQuestionCount();
}
