package ru.ofd.marking.web.dto;

/** Создание/изменение агента в ЛК. token: при изменении пусто/null — оставить прежний. */
public record AgentRequest(String name, String host, Integer port, Boolean useTls, String token, Boolean enabled,
                           String comment) {
}
