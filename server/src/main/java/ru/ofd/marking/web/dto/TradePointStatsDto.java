package ru.ofd.marking.web.dto;
public record TradePointStatsDto(
        Long tradePointId, String displayName, String address, String place,
        long totalReceipts, long markedReceipts, double markedPercent) {}
