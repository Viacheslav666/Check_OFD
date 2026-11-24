package ru.ofd.marking.domain.repo;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ofd.marking.domain.entity.ReceiptItem;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {

    @Query("""
        select i.productGroup, count(i), sum(case when i.marked then 1 else 0 end)
        from ReceiptItem i join i.receipt r
        where r.docDateTime >= :from and r.docDateTime < :to
        group by i.productGroup
        """)
    List<Object[]> aggregateByGroup(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        select r.tradePoint.id, i.productGroup, count(i), sum(case when i.marked then 1 else 0 end)
        from ReceiptItem i join i.receipt r
        where r.docDateTime >= :from and r.docDateTime < :to
        group by r.tradePoint.id, i.productGroup
        """)
    List<Object[]> matrix(@Param("from") Instant from, @Param("to") Instant to);
}
