package ru.ofd.marking.domain.repo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ofd.marking.domain.entity.TradePoint;
public interface TradePointRepository extends JpaRepository<TradePoint, Long> {
    Optional<TradePoint> findByKeyHash(String keyHash);
}
