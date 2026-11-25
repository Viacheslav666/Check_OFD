package ru.ofd.marking.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ru.ofd.marking.ofd.dto.OfdKktDto;

/** ККТ в разрезе организации (ИНН), как её вернул ОФД агенту. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentKktDto(String inn, OfdKktDto kkt) {
}
