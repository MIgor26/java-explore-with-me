package ru.practicum.ewm.exception;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, ValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidn(final Exception e) {
        log.warn("Ошибка валидации DTO: {}", e.getMessage(), e);
        return new ErrorResponse("Ошибка валидации DTO: ", e.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(final Exception e) {
        log.warn("Ошибка валидации параметров запроса: {}", e.getMessage(), e);
        return new ErrorResponse("Ошибка валидации параметров запроса: ", e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadable(final Exception e) {
        log.warn("Ошибка чтения тела запроса: {}", e.getMessage(), e);
        return new ErrorResponse("Ошибка чтения тела запроса: ", e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingServletRequestParameter(final Exception e) {
        log.warn("Отсутствует обязательный параметр: {}", e.getMessage(), e);
        return new ErrorResponse("Отсутствует обязательный параметр: ", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(final Exception e) {
        log.warn("Некорректный аргумент: {}", e.getMessage(), e);
        return new ErrorResponse("Некорректный аргумент: ", e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolation(final Exception e) {
        log.warn("Нарушение целостности данных: {}", e.getMessage(), e);
        return new ErrorResponse("Нарушение целостности данных: ", e.getMessage());
    }

    @ExceptionHandler({NotEmptyException.class, ConflictException.class, NotMeetRulesException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(final Exception e) {
        log.warn("Конфликт данных. {}", e.getMessage());
        return new ErrorResponse("Конфликт данных. ", e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(final Exception e) {
        log.warn("Ресурс не найден. {}", e.getMessage());
        return new ErrorResponse("Ресурс не найден. ", e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(final Exception e) {
        log.error("Внутренняя ошибка сервера {}", e.getMessage());
        return new ErrorResponse("Произошла внутренняя ошибка сервера", "");
    }
}
