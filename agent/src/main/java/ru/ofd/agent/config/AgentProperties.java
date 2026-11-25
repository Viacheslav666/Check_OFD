package ru.ofd.agent.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent")
public class AgentProperties {
    /** Отображаемое имя агента (например, «РДП Самара»). */
    private String name = "agent";
    /** Общий секрет с центральным приложением. Пусто — агент сгенерирует и сохранит в data-dir/agent-token.txt. */
    private String token = "";
    /** Белый список IP/подсетей (CIDR), с которых разрешены запросы. Обычно — IP центрального приложения. */
    private List<String> allowedIps = new ArrayList<>(List.of("127.0.0.1", "::1"));
    /** Каталог локальных данных агента (H2, токен, логи). */
    private String dataDir = "./data";
    /** Сколько дней хранить чеки локально (по дате документа). */
    private int retentionDays = 120;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public List<String> getAllowedIps() { return allowedIps; }
    public void setAllowedIps(List<String> allowedIps) { this.allowedIps = allowedIps; }
    public String getDataDir() { return dataDir; }
    public void setDataDir(String dataDir) { this.dataDir = dataDir; }
    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
}
