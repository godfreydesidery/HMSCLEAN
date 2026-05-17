package com.otapp.hmis.engine.procurement.receipt.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    Optional<GoodsReceipt> findByUid(String uid);

    List<GoodsReceipt> findAllByOrderUidOrderByReceivedAtDesc(String orderUid);
}
