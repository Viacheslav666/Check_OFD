package ru.ofd.marking.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ofd.marking.config.AppProperties;
import ru.ofd.marking.domain.entity.AppSetting;
import ru.ofd.marking.domain.repo.AppSettingRepository;

@Service
public class SettingsService {

    public static final String WORST_THRESHOLD = "worst_threshold_percent";
    public static final String DEFAULT_PERIOD_DAYS = "default_period_days";

    private final AppSettingRepository repo;
    private final AppProperties appProperties;

    public SettingsService(AppSettingRepository repo, AppProperties appProperties) {
        this.repo = repo;
        this.appProperties = appProperties;
    }

    public Map<String, String> all() {
        Map<String, String> map = new HashMap<>();
        repo.findAll().forEach(s -> map.put(s.getKey(), s.getValue()));
        map.putIfAbsent(WORST_THRESHOLD, String.valueOf(appProperties.getDefaultWorstThresholdPercent()));
        map.putIfAbsent(DEFAULT_PERIOD_DAYS, "30");
        return map;
    }

    public double worstThreshold() {
        return Double.parseDouble(all().getOrDefault(WORST_THRESHOLD, "25.0"));
    }

    @Transactional
    public Map<String, String> update(Map<String, String> patch) {
        for (var e : patch.entrySet()) {
            AppSetting s = repo.findById(e.getKey()).orElseGet(AppSetting::new);
            s.setKey(e.getKey());
            s.setValue(e.getValue());
            s.setUpdatedAt(Instant.now());
            repo.save(s);
        }
        return all();
    }
}
