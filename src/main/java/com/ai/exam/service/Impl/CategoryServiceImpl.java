package com.ai.exam.service.Impl;

import com.ai.exam.entity.Category;
import com.ai.exam.mapper.CategoryMapper;
import com.ai.exam.mapper.QuestionMapper;
import com.ai.exam.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private QuestionMapper questionMapper;

    @Override
    public List<Category> getAllCategories() {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
        //获取并填充每个分类的题目数量
        fillQuestionCount(categories);
        return  categories;
    }

    @Override
    public List<Category> getCategoryTree() {
        List<Category> allCategories = categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
        fillQuestionCount(allCategories);
        return buildTree(allCategories);

    }

    private List<Category> buildTree(List<Category> allCategories) {

        //按parentId分组
        Map<Long, List<Category>> childrenMap = allCategories.stream()
                .collect(Collectors.groupingBy(Category::getParentId));
        //设置children属性，并从上至下汇总题目数量
        allCategories.forEach(category -> {
            List<Category> children = childrenMap.getOrDefault(category.getId(), new ArrayList<>());
            category.setChildren(children);
            //汇总子分类的题目数量到父分类
            long childrenCount = children.stream()
                    .mapToLong(c -> c.getCount() != null ? c.getCount() : 0L)
                    .sum();
            Long selfCount = category.getCount();
            category.setCount(selfCount + childrenCount);
        });

        //返回顶级分类
        return allCategories.stream()
                .filter(c->c.getParentId()==0)
                .collect(Collectors.toList());



    }

    private void fillQuestionCount(List<Category> categories) {
        //获取每个分类的题目数量
      List<Map<String,Object>> questionCountList = questionMapper.getCategoryQuestionCount();
        //将结果转化为Map<Long,Long>格式
        Map<Long, Long> questionCountMap = questionCountList.stream()
                .collect(Collectors.toMap(
                        map -> Long.valueOf(map.get("categoryId").toString()),
                        map -> Long.valueOf(map.get("count").toString())

                ));
        //设置题目数量
        categories.forEach(category ->
            category.setCount(questionCountMap.getOrDefault(category.getId(), 0L))
        );

    }
}
