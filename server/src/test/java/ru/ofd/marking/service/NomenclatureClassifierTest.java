package ru.ofd.marking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ofd.marking.domain.ProductGroup;
import ru.ofd.marking.domain.entity.NomenclatureRule;
import ru.ofd.marking.domain.repo.NomenclatureRuleRepository;

@ExtendWith(MockitoExtension.class)
class NomenclatureClassifierTest {
    @Mock NomenclatureRuleRepository repo;

    @Test
    void classifiesCyrillicCaseInsensitively() {
        NomenclatureRule dairy = rule(ProductGroup.DAIRY, "(?i)(молоко|кефир)", 10);
        NomenclatureRule draft = rule(ProductGroup.DRAFT_BEER, "(?i)(разливн.*(пив|эль))", 15);
        NomenclatureRule beer = rule(ProductGroup.BEER, "(?i)(пиво|балтика)", 20);
        when(repo.findByEnabledTrueOrderByPriorityAsc()).thenReturn(List.of(dairy, draft, beer));

        NomenclatureClassifier c = new NomenclatureClassifier(repo);
        c.reload();
        assertEquals(ProductGroup.DAIRY, c.classify("Молоко 3.2% 1л"));
        assertEquals(ProductGroup.BEER, c.classify("Пиво Балтика 0.5л"));
        assertEquals(ProductGroup.DRAFT_BEER, c.classify("Разливное пиво светлое 1л"));
    }

    private static NomenclatureRule rule(ProductGroup g, String p, int pri) {
        NomenclatureRule r = new NomenclatureRule();
        r.setProductGroup(g);
        r.setPattern(p);
        r.setPriority(pri);
        r.setEnabled(true);
        return r;
    }
}
