package ru.practicum.ewm.category.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.service.CategoryService;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryController {
    private final CategoryService service;

    @Operation(summary = "Добавление новой категории")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody NewCategoryDto dto) {
        log.info("POST in AdminCategoryController. Добавление новой категории = {}", dto);
        return service.createCategory(dto);
    }

    @Operation(summary = "Удаление категории")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable @Positive Long id) {
        log.info("DELETE/id in AdminCategoryController. Удаление категории по id = {}", id);
        service.deleteCategory(id);
    }

    @Operation(summary = "Изменение категории")
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryDto updateCategory(@PathVariable Long id,
                                      @Valid @RequestBody NewCategoryDto dto) {
        log.info("PATCH/id in AdminCategoryController. Изменение категории по id = {}. Новые параметры = {}", id, dto);
        return service.updateCategory(id, dto);
    }
}
