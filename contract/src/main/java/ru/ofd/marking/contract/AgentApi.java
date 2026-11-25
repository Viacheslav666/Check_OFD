package ru.ofd.marking.contract;

/**
 * Контракт HTTP API агента (версия v1). Все пути относительны к http(s)://host:port агента.
 * Доступ: IP центрального приложения в белом списке агента + заголовок {@link #TOKEN_HEADER}.
 */
public final class AgentApi {
    private AgentApi() {
    }

    public static final String VERSION = "v1";
    public static final String BASE = "/agent/v1";
    public static final String TOKEN_HEADER = "X-Agent-Token";

    /** GET — жив ли агент (только белый список IP, без токена). */
    public static final String HEALTH = BASE + "/health";
    /** GET — состояние агента и последнего сбора из ОФД ({@link AgentStatusDto}). */
    public static final String STATUS = BASE + "/status";
    /** GET — ККТ из ОФД, которые видит агент (список {@link AgentKktDto}). */
    public static final String KKTS = BASE + "/kkts";
    /** GET ?after=&lt;seq&gt;&amp;limit=&lt;n&gt; — инкрементальная выдача чеков ({@link ReceiptPageDto}). */
    public static final String RECEIPTS = BASE + "/receipts";
    /** POST — запустить сбор из ОФД вне расписания ({@link CollectResultDto}). */
    public static final String COLLECT = BASE + "/collect";

    public static final int DEFAULT_PAGE_SIZE = 500;
    public static final int MAX_PAGE_SIZE = 1000;
}
