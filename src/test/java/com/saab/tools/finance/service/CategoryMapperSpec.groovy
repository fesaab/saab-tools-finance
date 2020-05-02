package com.saab.tools.finance.service

import com.saab.tools.finance.model.entity.CategoryMapping
import com.saab.tools.finance.model.repository.CategoryMappingRepository
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class CategoryMapperSpec extends Specification {

    CategoryMappingRepository categoryMappingRepository;
    CategoryMapper categoryMapper;

    def setup() {
        categoryMappingRepository = Mock(CategoryMappingRepository)
        categoryMapper = new CategoryMapper(categoryMappingRepository)
    }

    def "test mapping"() {
        given: "the mock is configure according to the parameters"
        categoryMappingRepository.query(description) >> repositoryQueryCategory
        categoryMappingRepository.getRegexList() >> repositoryRegexList

        when: "the mapper is invoked"
        String categoryMapped = categoryMapper.map(description)

        then: "the expected category is mapped"
        categoryMapped == expectedCategoryMapped

        where: "multiple values are tested"
        description | expectedCategoryMapped | repositoryQueryCategory                     | repositoryRegexList
        "Bottles"   | "TODO"                 | null                                        | new ArrayList()
        "Bottles"   | "Mercado"              | createCategory("Bottles", "Mercado", false) | new ArrayList()
        "Bottles"   | "TODO"                 | null                                        | createRegexList("Woolies*|Mercado", "OP0*|Brasil")
        "Bottles"   | "Mercado"              | null                                        | createRegexList("Woolies*|Mercado", "OP0*|Brasil", "Bottl*|Mercado")
    }

    def createRegexList(String... categoriesStr) {
        List<CategoryMapping> categoryMappingList = new ArrayList<>();
        for (String categoryStr : categoriesStr) {
            String[] categoryStrSplit = categoryStr.split("\\|");
            categoryMappingList.add(createCategory(categoryStrSplit[0], categoryStrSplit[1], true));
        }
        return categoryMappingList;
    }

    def createCategory(String description, String category, Boolean regex) {
        return CategoryMapping.builder()
                .description(description)
                .category(category)
                .regex(regex)
                .build();
    }
}
