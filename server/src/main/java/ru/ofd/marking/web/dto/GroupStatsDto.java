package ru.ofd.marking.web.dto;
public record GroupStatsDto(
        String groupCode, String groupTitle,
        long totalItems, long markedItems, double markedPercent) {}
