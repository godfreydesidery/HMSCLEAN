package com.otapp.hmis.engine.procurement.receipt.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsReceiptLineRepository extends JpaRepository<GoodsReceiptLine, Long> {

    List<GoodsReceiptLine> findAllByReceiptUidOrderByCreatedAtAsc(String receiptUid);
}
