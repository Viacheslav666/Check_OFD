package ru.ofd.marking.domain.repo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ofd.marking.domain.entity.Kkt;
public interface KktRepository extends JpaRepository<Kkt, Long> {
    Optional<Kkt> findByRegNumber(String regNumber);
    long countByAgentId(Long agentId);
}
