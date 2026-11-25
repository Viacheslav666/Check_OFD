package ru.ofd.marking.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Ответ на запуск сбора: accepted=false, если сбор уже идёт. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CollectResultDto(boolean accepted, boolean collecting, String message) {
}
