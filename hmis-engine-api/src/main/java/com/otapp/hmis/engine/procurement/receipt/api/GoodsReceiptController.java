package com.otapp.hmis.engine.procurement.receipt.api;

import com.otapp.hmis.engine.procurement.receipt.application.GoodsReceiptDtos.GoodsReceiptDto;
import com.otapp.hmis.engine.procurement.receipt.application.GoodsReceiptDtos.RecordReceiptRequest;
import com.otapp.hmis.engine.procurement.receipt.application.GoodsReceiptService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Goods receipts")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PROCUREMENT_ACCESS')")
public class GoodsReceiptController {

    private final GoodsReceiptService receiptService;

    @PostMapping("/procurement/purchase-orders/uid/{orderUid}/receipts")
    public ResponseEntity<GoodsReceiptDto> record(@PathVariable String orderUid,
                                                  @Valid @RequestBody RecordReceiptRequest request) {
        return ResponseEntity.ok(receiptService.record(orderUid, request));
    }

    @GetMapping("/procurement/purchase-orders/uid/{orderUid}/receipts")
    public ResponseEntity<List<GoodsReceiptDto>> listForOrder(@PathVariable String orderUid) {
        return ResponseEntity.ok(receiptService.listForOrder(orderUid));
    }
}
