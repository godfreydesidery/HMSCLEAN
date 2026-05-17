package com.otapp.hmis.engine.pharmacy.sale.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacySaleOrderLineRepository extends JpaRepository<PharmacySaleOrderLine, Long> {

    Optional<PharmacySaleOrderLine> findByUid(String uid);

    List<PharmacySaleOrderLine> findAllBySaleUidOrderByCreatedAtAsc(String saleUid);

    long countBySaleUidAndStatusNotIn(String saleUid,
                                      java.util.Collection<PharmacySaleLineStatus> terminalStatuses);
}
