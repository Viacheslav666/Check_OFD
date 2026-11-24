package ru.ofd.marking.web.dto;
public record MatrixCellDto(
        Long tradePointId, String tradePointName,
        String groupCode, String groupTitle,
        long totalItems, long markedItems, double markedPercent) {}
