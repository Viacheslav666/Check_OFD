package ru.ofd.marking.domain.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ofd.marking.domain.entity.Receipt;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    Optional<Receipt> findByOfdRawId(String ofdRawId);
    boolean existsByOfdRawId(String ofdRawId);

    @Query("""
        select r.tradePoint.id, count(r), sum(case when r.hasMarkedGoods then 1 else 0 end)
        from Receipt r
        where r.docDateTime >= :from and r.docDateTime < :to
        group by r.tradePoint.id
        """)
    List<Object[]> aggregateByTradePoint(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        select r from Receipt r
        where r.docDateTime >= :from and r.docDateTime < :to
          and (:tpId is null or r.tradePoint.id = :tpId)
          and (:kkt is null or r.kktRegNumber = :kkt)
          and (:markedOnly = false or r.hasMarkedGoods = true)
        """)
    Page<Receipt> search(@Param("from") Instant from, @Param("to") Instant to,
                         @Param("tpId") Long tpId, @Param("kkt") String kkt,
                         @Param("markedOnly") boolean markedOnly, Pageable pageable);
}
