package com.otapp.hmis.engine.procurement.order.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.AddLineRequest;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.CancelPurchaseOrderRequest;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.CreatePurchaseOrderRequest;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.PurchaseOrderDto;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.PurchaseOrderSummary;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.UpdateLineRequest;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderService;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Purchase orders")
@RestController
@RequestMapping("/procurement/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PROCUREMENT_ACCESS')")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    public ResponseEntity<PurchaseOrderDto> create(@Valid @RequestBody CreatePurchaseOrderRequest request) {
        PurchaseOrderDto created = purchaseOrderService.create(request);
        URI loc = UriComponentsBuilder.fromPath("/procurement/purchase-orders/uid/{purchaseOrderUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<PurchaseOrderSummary>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) String supplierUid,
            @RequestParam(required = false) String pharmacyUid,
            Pageable pageable) {
        return ResponseEntity.ok(purchaseOrderService.search(query, status, supplierUid, pharmacyUid, pageable));
    }

    @GetMapping("/uid/{purchaseOrderUid}")
    public ResponseEntity<PurchaseOrderDto> findByUid(@PathVariable String purchaseOrderUid) {
        return ResponseEntity.ok(purchaseOrderService.findByUid(purchaseOrderUid));
    }

    @PostMapping("/uid/{purchaseOrderUid}/lines")
    public ResponseEntity<PurchaseOrderDto> addLine(@PathVariable String purchaseOrderUid,
                                                    @Valid @RequestBody AddLineRequest request) {
        return ResponseEntity.ok(purchaseOrderService.addLine(purchaseOrderUid, request));
    }

    @PutMapping("/uid/{purchaseOrderUid}/lines/uid/{lineUid}")
    public ResponseEntity<PurchaseOrderDto> updateLine(@PathVariable String purchaseOrderUid,
                                                       @PathVariable String lineUid,
                                                       @Valid @RequestBody UpdateLineRequest request) {
        return ResponseEntity.ok(purchaseOrderService.updateLine(purchaseOrderUid, lineUid, request));
    }

    @DeleteMapping("/uid/{purchaseOrderUid}/lines/uid/{lineUid}")
    public ResponseEntity<PurchaseOrderDto> removeLine(@PathVariable String purchaseOrderUid,
                                                       @PathVariable String lineUid) {
        return ResponseEntity.ok(purchaseOrderService.removeLine(purchaseOrderUid, lineUid));
    }

    @PostMapping("/uid/{purchaseOrderUid}/order")
    public ResponseEntity<PurchaseOrderDto> markOrdered(@PathVariable String purchaseOrderUid) {
        return ResponseEntity.ok(purchaseOrderService.markOrdered(purchaseOrderUid));
    }

    @PostMapping("/uid/{purchaseOrderUid}/cancel")
    public ResponseEntity<PurchaseOrderDto> cancel(@PathVariable String purchaseOrderUid,
                                                   @Valid @RequestBody(required = false) CancelPurchaseOrderRequest request) {
        return ResponseEntity.ok(purchaseOrderService.cancel(purchaseOrderUid, request));
    }
}
