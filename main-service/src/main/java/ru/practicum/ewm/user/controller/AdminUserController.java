package ru.practicum.ewm.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/users")
@Validated
public class AdminUserController {

    private final UserService userService;

    @Operation(summary = "Получение информации о пользователях")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<UserDto> getUsers(@RequestParam(required = false) List<Long> ids,
                                  @RequestParam(defaultValue = "0") int from,
                                  @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET/all in AdminUserController. Получение информации о пользователях");
        return userService.getUsers(ids, from, size);
    }

    @Operation(summary = "Добавление нового пользователя")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody UserDto userDto) {
        log.info("POST in AdminUserController. Добавление нового пользователя {}", userDto);
        return userService.createUser(userDto);
    }

    @Operation(summary = "Удаление пользователя")
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable @Positive Long id) {
        log.info("DELETE/id in AdminUserController. Удаление пользователя id = {}", id);
        userService.deleteUser(id);
    }
}
