package ru.practicum.ewm.request.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.service.RequestService;

import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/requests")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PrivateRequestController {
    private final RequestService requestService;

    @Operation(summary = "Получение информации о заявках текущего пользователя на участие в чужих событиях")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getRequestsByCurrentUser(@PathVariable @Positive Long userId) {
        log.info("GET/all in PrivateRequestController. Получение информации о заявках текущего  пользователя c id = "
                + userId + " на участие в чужих событиях");
        return requestService.getRequestsByCurrentUser(userId);
    }

    @Operation(summary = "Добавление запроса от текущего пользователя на участие в событии")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addRequest(@PathVariable Long userId,
                                              @RequestParam Long eventId) {
        log.info("POST in PrivateRequestController. Добавление запроса от текущего пользователя c id = "
                + userId + " на участие в событии с id = " + eventId);
        return requestService.addRequest(userId, eventId);
    }

    @Operation(summary = "Отмена своего запроса на участие в событии")
    @PatchMapping("/{requestId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public ParticipationRequestDto cancelRequest(@PathVariable @Positive Long userId,
                                                 @PathVariable @Positive Long requestId) {
        log.info("PATCH in PrivateRequestController. Отмена своего id = {} запроса на участие в событии id = {}", userId, requestId);
        return requestService.cancelRequest(userId, requestId);
    }
}
