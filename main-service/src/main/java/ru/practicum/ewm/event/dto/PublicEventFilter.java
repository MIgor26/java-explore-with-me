package ru.practicum.ewm.event.dto;

import lombok.Data;

import java.util.List;

@Data
public class PublicEventFilter {
    private String text;
    private List<Long> categories;
    private Boolean paid;
    private String rangeStart;
    private String rangeEnd;
    private Boolean onlyAvailable;
    private String sort;
    private Integer from = 0;
    private Integer size = 10;
    private String userIp;
    private String requestUri;
}

