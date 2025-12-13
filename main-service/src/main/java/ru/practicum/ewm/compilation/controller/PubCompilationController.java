package ru.practicum.ewm.compilation.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.service.CompilationService;

import java.util.List;

@RestController
@RequestMapping(path = "/compilations")
@RequiredArgsConstructor
@Slf4j
public class PubCompilationController {
    private final CompilationService compilationService;

    @Operation(summary = "Получение подборок событий")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CompilationDto> getCompilationList(@RequestParam(required = false) Boolean pinned,
                                                   @RequestParam(required = false, defaultValue = "0") @PositiveOrZero Integer from,
                                                   @RequestParam(required = false, defaultValue = "10") @Positive Integer size) {
        log.info("GET/all in PubCompilationController. Получение подборок событий pinned = {}, from = {}, size = {}", pinned, from, size);
        return compilationService.getCompilationList(pinned, from, size);
    }

    @Operation(summary = "Получение подборки событий по его id")
    @GetMapping("/{compId}")
    @ResponseStatus(HttpStatus.OK)
    public CompilationDto getCompilation(@PathVariable @Positive Long compId) {
        log.info("GET/id in PubCompilationController. Получение подборки событий по его id = {}", compId);
        return compilationService.getCompilation(compId);
    }
}
