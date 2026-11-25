package ru.ofd.agent.security;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Текущий белый список IP; заменяется атомарно при горячей перезагрузке agent.yml. */
public final class AllowlistHolder {
    private final AtomicReference<IpAllowlist> current;
    private volatile List<String> entries;

    public AllowlistHolder(List<String> entries) {
        IpAllowlist a = new IpAllowlist(entries);
        if (a.isEmpty()) {
            throw new IllegalStateException("agent.allowed-ips пуст: укажите IP центрального приложения");
        }
        this.current = new AtomicReference<>(a);
        this.entries = normalize(entries);
    }

    public IpAllowlist get() {
        return current.get();
    }

    public List<String> entries() {
        return entries;
    }

    /**
     * Заменяет список. Пустой или ошибочный список не применяется (агент не должен оказаться без защиты
     * или без доступа центра из-за опечатки) — бросает IllegalArgumentException, текущий список остаётся.
     * @return true, если список изменился
     */
    public synchronized boolean replace(List<String> newEntries) {
        List<String> norm = normalize(newEntries);
        IpAllowlist a = new IpAllowlist(norm);
        if (a.isEmpty()) throw new IllegalArgumentException("новый allowed-ips пуст");
        if (norm.equals(entries)) return false;
        current.set(a);
        entries = norm;
        return true;
    }

    static List<String> normalize(List<String> raw) {
        if (raw == null) return List.of();
        return raw.stream().filter(s -> s != null)
                .flatMap(s -> java.util.Arrays.stream(s.split("[,;\\s]+")))
                .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
    }
}
