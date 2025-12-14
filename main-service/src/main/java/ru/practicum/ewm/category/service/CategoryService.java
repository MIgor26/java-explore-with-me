package ru.practicum.ewm.category.service;

import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {

    // Методы Администратора
    CategoryDto createCategory(NewCategoryDto dto);

    void deleteCategory(Long id);

    CategoryDto updateCategory(Long id, NewCategoryDto dto);

    // Методы публичные
    List<CategoryDto> getAllCategories(int from, int size);

    CategoryDto getCategoryById(Long id);
}
