package ru.practicum.ewm.category.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.service.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Slf4j
public class PublicCategoryController {
    private final CategoryService service;

    @Operation(summary = "Получение категорий")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CategoryDto> getAllCategories(@RequestParam(defaultValue = "0") int from,
                                              @RequestParam(defaultValue = "10") int size) {
        log.info("GET/all in PublicCategoryController. Получение категорий: from = {}, size = {}", from, size);
        return service.getAllCategories(from, size);
    }

    @Operation(summary = "Получение информации о категории по её идентификатору")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    private CategoryDto getCategoryById(@PathVariable @Positive Long id) {
        log.info("GET/id in PublicCategoryController. Получение информации о категории по id = {}", id);
        return service.getCategoryById(id);
    }
}
