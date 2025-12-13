package ru.practicum.ewm.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.PublicEventFilter;
import ru.practicum.ewm.event.service.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
@Slf4j
public class PublicEventController {
    private final EventService eventService;

    @Operation(summary = "Получение событий с возможностью фильтрации")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEventList(@ModelAttribute PublicEventFilter filter,
                                            HttpServletRequest request) {
        filter.setUserIp(request.getRemoteAddr());
        filter.setRequestUri(request.getRequestURI());
        log.info("GET/all in PublicEventController. Получение событий с возможностью фильтрации = {}", filter);
        return eventService.getEventList(filter);
    }

    @Operation(summary = "Получение подробной информации об опубликованном событии по его идентификатору")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEvent(@PathVariable @Positive Long id, HttpServletRequest request) {
        log.info("GET/id in PublicEventController. Получение подробной информации о событии с id = {}", id);
        return eventService.getEvent(id, request.getRemoteAddr(), request.getRequestURI());
    }
}
