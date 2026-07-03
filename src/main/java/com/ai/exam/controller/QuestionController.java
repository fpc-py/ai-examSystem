package com.ai.exam.controller;

import com.ai.exam.common.Result;
import com.ai.exam.dto.QuestionQueryDTO;
import com.ai.exam.entity.Question;
import com.ai.exam.entity.QuestionAnswer;
import com.ai.exam.entity.QuestionChoice;
import com.ai.exam.mapper.QuestionAnswerMapper;
import com.ai.exam.mapper.QuestionChoiceMapper;
import com.ai.exam.service.QuestionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/questions")
@Tag(name = "题目管理", description = "题目相关的增删改查操作，包括分页查询、随机获取、热门推荐等功能")
public class QuestionController {
    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionChoiceMapper questionChoiceMapper;

    @Autowired
    private QuestionAnswerMapper questionAnswerMapper;

    @GetMapping("/list")
    @Operation(summary = "分页查询题目列表", description = "支持按分类、难度、题型、关键词进行多条件筛选的分页查询")
    public Result<Page<Question>> getQuestionList(QuestionQueryDTO dto) {
        int pageNum = Optional.ofNullable(dto.getPage()).orElse(1);
        int pageSize = Optional.ofNullable(dto.getSize()).orElse(10);

        Page<Question> pageInfo = new Page<>(pageNum, pageSize);
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (dto.getCategoryId()!= null){
            queryWrapper.eq("category_id",dto.getCategoryId());
        }
        if (StringUtils.hasText(dto.getDifficulty())){
            queryWrapper.eq("difficulty",dto.getDifficulty());
        }
        if (StringUtils.hasText(dto.getType())){
            queryWrapper.eq("type",dto.getType());
        }
        if (StringUtils.hasText(dto.getKeyword())){
            queryWrapper.like("title",dto.getKeyword());

        }
        queryWrapper.orderByDesc("create_time");
        Page<Question> result = questionService.page(pageInfo, queryWrapper);

        fillQuestionsDetailsBatch(result.getRecords());
        return Result.success(result);
    }
/**
 * 批量为题目列表填充选项和答案，避免N+1查询
 */
    private void fillQuestionsDetailsBatch(List<Question> questions) {
        if (questions == null || questions.isEmpty()) return;
        List<Long> ids = questions.stream().map(Question::getId).toList();
        // 批量查询所有选项
        List<QuestionChoice> allChoices = questionChoiceMapper.selectList(
                new QueryWrapper<QuestionChoice>().in("question_id", ids)
        );
        // 批量查询所有答案
        List<QuestionAnswer> allAnswers = questionAnswerMapper.selectList(
                new QueryWrapper<QuestionAnswer>().in("question_id", ids)
        );

        Map<Long, List<QuestionChoice>> choicesMap = allChoices.stream().collect(Collectors.groupingBy(QuestionChoice::getQuestionId));
        Map<Long, QuestionAnswer> answersMap = allAnswers.stream().collect(Collectors.toMap(QuestionAnswer::getQuestionId, a -> a));

        for(Question q :  questions){
            if ("CHOICE".equals(q.getType())) {
                List<QuestionChoice> choices = choicesMap.getOrDefault(q.getId(), new ArrayList<>());
                choices.sort(Comparator.comparingInt(c -> c.getSort() == null ? 0 : c.getSort()));
                q.setChoices(choices);
            }
            q.setAnswer(answersMap.get(q.getId()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询题目详情", description = "获取指定ID的题目完整信息，包括题目内容、选项、答案等详细数据")
    public Result<Question> getQuestionById(@PathVariable("id") Long id) {
       Question question = questionService.getQuestionWithDetails(id);
        if (question != null) {
            // 通过注入的 Bean 调用，@Async 才会生效；避免 @Cacheable 缓存命中后计数不执行
            questionService.incrementQuestionViewCount(id);
            return Result.success(question);
        } else {
            return Result.error("题目未找到");
        }
    }
    @PostMapping
    @Operation(summary = "创建新题目", description = "添加新的考试题目，支持选择题、判断题、简答题等多种题型")
    public Result<Question> createQuestion(@RequestBody Question question) {
        questionService.saveQuestionWithDetails(question);
        return Result.success(question);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新题目信息", description = "修改指定题目的内容、选项、答案等信息")
    public Result<Question> updateQuestion(
           @PathVariable Long id,
            @RequestBody Question question) {
        // 设置题目ID，确保更新正确的记录
        question.setId(id);
        questionService.updateQuestionWithDetails(question);
        return Result.success(question);
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "删除题目", description = "根据ID删除指定的题目，包括关联的选项和答案数据")
    public Result<String> deleteQuestion(@PathVariable Long id) {

        boolean success = questionService.removeById(id);
        if (success) {
            return Result.success("题目删除成功");
        } else {
            return Result.error("题目删除失败");
        }
    }

    @GetMapping("/popular")
    @Operation(summary = "获取热门题目", description = "获取访问次数最多的热门题目，用于首页推荐展示")
    public Result<List<Question>> getPopularQuestions(@RequestParam(defaultValue = "10") Integer size) {
        try {
            List<Question> questions = questionService.getPopularQuestions(size);

            if (questions.size() < size) {
                int needMore = size - questions.size();

                List<Long> existIds = questions.stream()
                        .map(Question::getId)
                        .collect(Collectors.toList());

                QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
                if (!existIds.isEmpty()) {
                    queryWrapper.notIn("id", existIds);
                }
                queryWrapper.orderByDesc("create_time")
                        .last("LIMIT " + needMore);

                List<Question> latestQuestions = questionService.list(queryWrapper);
                fillQuestionsDetailsBatch(latestQuestions);

                questions.addAll(latestQuestions);
            }

            return Result.success(questions);
        } catch (Exception e) {
            return Result.error("获取热门题目失败：" + e.getMessage());
        }
    }
}
