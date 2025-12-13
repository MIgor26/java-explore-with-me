package ru.practicum.ewm.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.AdminEventFilter;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.service.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/admin/events")
@RequiredArgsConstructor
@Slf4j
public class AdminEventController {
    private final EventService eventService;

    @Operation(summary = "Поиск событий")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventFullDto> getEventListByAdmin(@ModelAttribute AdminEventFilter filter) {
        log.info("GET/all in AdminEventController. Поиск событий по фильтрам: {}", filter);
        return eventService.getEventsByAdmin(filter);
    }

    @Operation(summary = "Редактирование данных события и его статуса (отклонение/публикация).")
    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(@PathVariable Long eventId,
                                    @Valid @RequestBody UpdateEventAdminRequest updateEventAdminRequest) {
        log.info("PATCH/id in AdminEventController. Редактирование события с id = {} и новые данные = {}",
                eventId, updateEventAdminRequest);
        return eventService.updateEventByAdmin(eventId, updateEventAdminRequest);
    }
}
