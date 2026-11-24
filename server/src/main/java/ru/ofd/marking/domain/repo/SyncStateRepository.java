package ru.ofd.marking.domain.repo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ofd.marking.domain.entity.SyncState;
public interface SyncStateRepository extends JpaRepository<SyncState, Long> {
    Optional<SyncState> findByKktRegNumber(String kktRegNumber);
}
