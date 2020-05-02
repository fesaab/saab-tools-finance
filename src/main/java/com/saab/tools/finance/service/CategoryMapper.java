package com.saab.tools.finance.service;

import com.saab.tools.finance.model.entity.CategoryMapping;
import com.saab.tools.finance.model.repository.CategoryMappingRepository;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CategoryMapper {

    private CategoryMappingRepository repository;

    public CategoryMapper(CategoryMappingRepository repository) {
        this.repository = repository;
    }

    public String map(String description) {

        String mappedCategory = "TODO";

        // 1 - try to find an exact match on DynamoDB
        CategoryMapping category = repository.query(description);
        if (category != null) {
            mappedCategory = category.getCategory();
        }
        // 2 - otherwise get all the regexes and apply to the string
        else {
            List<CategoryMapping> categoryMappingRegexList = repository.getRegexList();
            if (categoryMappingRegexList != null && !categoryMappingRegexList.isEmpty()) {
                for (CategoryMapping categoryRegex : categoryMappingRegexList) {
                    Pattern p = Pattern.compile(categoryRegex.getDescription());
                    Matcher m = p.matcher(description);

                    if (m.find()) {
                        mappedCategory = categoryRegex.getCategory();
                        break;
                    }
                }
            }
        }

        return mappedCategory;
    }

}
