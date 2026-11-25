package ru.ofd.agent.security;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Горячая перезагрузка белого списка: раз в N секунд проверяет дату изменения agent.yml
 * и, если файл изменился, перечитывает agent.allowed-ips без перезапуска службы.
 * Другие параметры agent.yml (ключ ОФД, токен, порт) применяются только после перезапуска.
 */
public class AllowlistReloader {
    private static final Logger log = LoggerFactory.getLogger(AllowlistReloader.class);

    private final AllowlistHolder holder;
    private final List<Path> candidates;
    private Path lastFile;
    private FileTime lastModified;

    public AllowlistReloader(AllowlistHolder holder, List<Path> candidates) {
        this.holder = holder;
        this.candidates = candidates;
        Optional<Path> f = currentFile();
        f.ifPresent(p -> {
            lastFile = p;
            lastModified = mtime(p);
        });
        log.info("Горячая перезагрузка белого списка IP: файл {}", f.map(Path::toString).orElse("(внешний agent.yml не найден)"));
    }

    /** @return true, если список изменился */
    public synchronized boolean checkNow() {
        Optional<Path> f = currentFile();
        if (f.isEmpty()) return false;
        Path p = f.get();
        FileTime mt = mtime(p);
        if (p.equals(lastFile) && mt != null && mt.equals(lastModified)) return false;
        lastFile = p;
        lastModified = mt;
        try {
            List<String> ips = readAllowedIps(p);
            if (ips == null) {
                log.debug("В {} нет agent.allowed-ips — белый список не меняется", p);
                return false;
            }
            List<String> before = holder.entries();
            boolean changed = holder.replace(ips);
            if (changed) {
                log.info("Белый список IP обновлён из {}: {} -> {}", p, before, holder.entries());
            }
            return changed;
        } catch (Exception e) {
            log.error("agent.yml изменён, но белый список не применён ({}); действует прежний: {}",
                    e.getMessage(), holder.entries());
            return false;
        }
    }

    private Optional<Path> currentFile() {
        return candidates.stream().filter(Files::isRegularFile).findFirst();
    }

    private static FileTime mtime(Path p) {
        try {
            return Files.getLastModifiedTime(p);
        } catch (Exception e) {
            return null;
        }
    }

    /** null — ключа нет в файле. */
    @SuppressWarnings("unchecked")
    static List<String> readAllowedIps(Path file) throws Exception {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        String text = Files.readString(file, StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') text = text.substring(1); // BOM от Блокнота
        Object root = yaml.load(text);
        if (!(root instanceof Map<?, ?> m)) return null;
        Object agent = m.get("agent");
        Object v = null;
        if (agent instanceof Map<?, ?> am) {
            v = am.containsKey("allowed-ips") ? am.get("allowed-ips") : am.get("allowedIps");
        }
        if (v == null && m.containsKey("agent.allowed-ips")) v = m.get("agent.allowed-ips");
        if (v == null) return null;
        List<String> out = new ArrayList<>();
        if (v instanceof List<?> l) {
            for (Object o : l) if (o != null) out.add(o.toString());
        } else {
            out.add(v.toString());
        }
        return out;
    }
}
