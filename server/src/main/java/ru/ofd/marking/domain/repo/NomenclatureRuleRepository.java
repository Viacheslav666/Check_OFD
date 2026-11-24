package ru.ofd.marking.domain.repo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ofd.marking.domain.entity.NomenclatureRule;
public interface NomenclatureRuleRepository extends JpaRepository<NomenclatureRule, Long> {
    List<NomenclatureRule> findByEnabledTrueOrderByPriorityAsc();
}
