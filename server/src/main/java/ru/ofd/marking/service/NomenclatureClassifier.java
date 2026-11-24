package ru.ofd.marking.service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import ru.ofd.marking.domain.ProductGroup;
import ru.ofd.marking.domain.entity.NomenclatureRule;
import ru.ofd.marking.domain.repo.NomenclatureRuleRepository;

@Service
public class NomenclatureClassifier {

    private final NomenclatureRuleRepository repo;
    private volatile List<CompiledRule> rules = List.of();

    public NomenclatureClassifier(NomenclatureRuleRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void reload() {
        List<NomenclatureRule> enabled = repo.findByEnabledTrueOrderByPriorityAsc();
        List<CompiledRule> compiled = new ArrayList<>();
        for (NomenclatureRule r : enabled) {
            try {
                // (?i) alone does not fold Cyrillic; UNICODE_CASE is required.
                compiled.add(new CompiledRule(r.getProductGroup(),
                        Pattern.compile(r.getPattern(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
            } catch (Exception ignored) {
                // skip invalid patterns
            }
        }
        this.rules = List.copyOf(compiled);
    }

    public ProductGroup classify(String name) {
        if (name == null || name.isBlank()) {
            return ProductGroup.OTHER;
        }
        for (CompiledRule r : rules) {
            if (r.pattern.matcher(name).find()) {
                return r.group;
            }
        }
        return ProductGroup.OTHER;
    }

    private record CompiledRule(ProductGroup group, Pattern pattern) {}
}
