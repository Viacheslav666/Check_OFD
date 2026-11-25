package ru.ofd.marking.domain.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ofd.marking.domain.entity.Agent;

public interface AgentRepository extends JpaRepository<Agent, Long> {
    Optional<Agent> findByName(String name);
    List<Agent> findAllByOrderByNameAsc();
    List<Agent> findByEnabledTrueOrderByNameAsc();
}
