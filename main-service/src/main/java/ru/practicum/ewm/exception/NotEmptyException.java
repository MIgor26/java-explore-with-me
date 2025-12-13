package ru.practicum.ewm.exception;

public class NotEmptyException extends RuntimeException {
    public NotEmptyException(final String message) {
        super(message);
    }
}
