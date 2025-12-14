package ru.practicum.ewm.exception;

public class NotMeetRulesException extends RuntimeException {
    public NotMeetRulesException(final String message) {
        super(message);
    }
}