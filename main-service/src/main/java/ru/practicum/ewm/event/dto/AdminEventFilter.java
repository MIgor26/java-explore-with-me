package ru.practicum.ewm.event.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminEventFilter {
    private List<Long> users;
    private List<String> states;
    private List<Long> categories;
    private String rangeStart;
    private String rangeEnd;
    private Integer from = 0;
    private Integer size = 10;
}
