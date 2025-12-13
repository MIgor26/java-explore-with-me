package ru.practicum.ewm.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotEmptyException;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper mapper;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto dto) {
        Category category = mapper.toCategory(dto);
        Category saved = categoryRepository.save(category);
        log.info("Категория {} успешно сохранена.", saved);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        findCategoryById(id);
        if (!eventRepository.findAllByCategoryId(id).isEmpty()) {
            throw new NotEmptyException(String.format("Существуют события, связанные с категорией по id = ", id));
        }
        categoryRepository.deleteById(id);
        log.info("Категория по id = {} успешна удалена", id);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long id, NewCategoryDto dto) {
        Category category = findCategoryById(id);
        category.setName(dto.getName());
        Category updated = categoryRepository.save(category);
        log.info("Категория = {} успешно изменена", updated);
        return mapper.toDto(updated);
    }

    @Override
    public List<CategoryDto> getAllCategories(int from, int size) {
        List<CategoryDto> categoryDtoList = categoryRepository.findAll(PageRequest.of(from / size, size))
                .map(mapper::toDto)
                .toList();
        log.info("Успешно получено {} категорий", categoryDtoList.size());
        return categoryDtoList;
    }

    @Override
    public CategoryDto getCategoryById(Long id) {
        Category category = findCategoryById(id);
        log.info("Успешное получение категории по id = {}", id);
        return mapper.toDto(category);
    }

    private Category findCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Категория с id = %d не найдена.", id)));
    }
}
