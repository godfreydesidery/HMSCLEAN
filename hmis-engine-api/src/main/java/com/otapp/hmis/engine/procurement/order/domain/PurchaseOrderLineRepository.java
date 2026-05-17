package com.otapp.hmis.engine.procurement.order.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, Long> {

    Optional<PurchaseOrderLine> findByUid(String uid);

    List<PurchaseOrderLine> findAllByOrderUidOrderByCreatedAtAsc(String orderUid);

    void deleteByUid(String uid);
}
