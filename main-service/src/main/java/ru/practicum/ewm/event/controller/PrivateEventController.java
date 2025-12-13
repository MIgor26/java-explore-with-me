package ru.practicum.ewm.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;

import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/events")
@RequiredArgsConstructor
@Slf4j
public class PrivateEventController {
    private final EventService eventService;


    @Operation(summary = "Получение событий, добавленных текущим пользователем")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEventsByInitiator(@PathVariable Long userId,
                                                    @RequestParam(name = "from", defaultValue = "0") @PositiveOrZero Integer from,
                                                    @RequestParam(name = "size", defaultValue = "10") @Positive Integer size) {
        log.info("GET/all in PrivateEventController. Получение событий, добавленных пользователем id = {}", userId);
        return eventService.getEventsByInitiator(userId, PageRequest.of(from, size));
    }

    @Operation(summary = "Добавление нового события")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(@PathVariable Long userId,
                                 @Valid @RequestBody NewEventDto newEventDto) {
        log.info("POST in PrivateEventController. От пользователя id = {} новое событие = {}", userId, newEventDto);
        return eventService.addEvent(userId, newEventDto);
    }

    @Operation(summary = "Получение полной информации о событии добавленном текущим пользователем")
    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEventByInitiator(@PathVariable Long userId,
                                            @PathVariable Long eventId) {
        log.info("GET/id in PrivateEventController. От пользователя id = {} получение информации о событии с id = {}",
                userId, eventId);
        return eventService.getEventByInitiator(userId, eventId);
    }

    @Operation(summary = "Изменение события добавленного текущим пользователем")
    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEventByInitiator(@PathVariable Long userId,
                                               @PathVariable Long eventId,
                                               @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest) {
        log.info("GET/id in PrivateEventController. Пользователь id = {} изменение события id = {} на новые данные = {}",
                userId, eventId, updateEventUserRequest);
        return eventService.updateEventByInitiator(userId, eventId, updateEventUserRequest);
    }

    @Operation(summary = "Получение информации о запросах на участие в событии текущего пользователя")
    @GetMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getRequestsByCurrentUserOfCurrentEvent(@PathVariable Long userId,
                                                                                @PathVariable Long eventId) {
        log.info("GET/requests in PrivateEventController. Получение информации о запросах на участие в событии " +
                "текущего пользователя" + "userId = " + userId + "eventId = " + eventId);
        return eventService.getRequestsByCurrentUserOfCurrentEvent(userId, eventId);
    }

    @Operation(summary = "Изменение статуса (подтверждена, отменена) заявок на участие в событии текущего пользователя")
    @PatchMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResult updateRequest(@PathVariable Long userId,
                                                        @PathVariable Long eventId,
                                                        @Valid @RequestBody EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest) {
        log.info("PrivateEventController / updateRequest: " +
                "Изменение статуса (подтверждена, отменена) заявок на участие в событии текущего пользователя " +
                userId + eventId + eventRequestStatusUpdateRequest);
        return eventService.updateRequest(userId, eventId, eventRequestStatusUpdateRequest);
    }
}
