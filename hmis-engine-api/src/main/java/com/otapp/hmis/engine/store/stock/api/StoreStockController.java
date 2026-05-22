package com.otapp.hmis.engine.store.stock.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.store.stock.application.StoreStockDtos.AdjustStoreStockRequest;
import com.otapp.hmis.engine.store.stock.application.StoreStockDtos.ReceiveStoreStockRequest;
import com.otapp.hmis.engine.store.stock.application.StoreStockDtos.StoreStockBalanceDto;
import com.otapp.hmis.engine.store.stock.application.StoreStockDtos.StoreStockBatchDto;
import com.otapp.hmis.engine.store.stock.application.StoreStockDtos.StoreStockMovementDto;
import com.otapp.hmis.engine.store.stock.application.StoreStockService;
import com.otapp.hmis.engine.store.stock.domain.StoreStockMovementKind;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Store stock")
@RestController
@RequestMapping("/store")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('STORE_ACCESS')")
public class StoreStockController {

    private final StoreStockService storeStockService;

    @GetMapping("/stores/uid/{storeUid}/stock")
    public ResponseEntity<PageResponse<StoreStockBalanceDto>> searchBalances(
            @PathVariable String storeUid,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "false") boolean lowOnly,
            @RequestParam(required = false, defaultValue = "false") boolean expiringOnly,
            Pageable pageable) {
        return ResponseEntity.ok(storeStockService.searchBalances(storeUid, query, lowOnly, expiringOnly, pageable));
    }

    @PostMapping("/stores/uid/{storeUid}/stock/receive")
    public ResponseEntity<StoreStockBatchDto> receive(@PathVariable String storeUid,
                                                      @Valid @RequestBody ReceiveStoreStockRequest request) {
        return ResponseEntity.ok(storeStockService.receive(storeUid, request));
    }

    @PostMapping("/stores/uid/{storeUid}/stock/adjust")
    public ResponseEntity<StoreStockBatchDto> adjust(@PathVariable String storeUid,
                                                     @Valid @RequestBody AdjustStoreStockRequest request) {
        return ResponseEntity.ok(storeStockService.adjust(storeUid, request));
    }

    @GetMapping("/stock/movements")
    public ResponseEntity<PageResponse<StoreStockMovementDto>> searchMovements(
            @RequestParam(required = false) String storeUid,
            @RequestParam(required = false) String medicineUid,
            @RequestParam(required = false) StoreStockMovementKind kind,
            Pageable pageable) {
        return ResponseEntity.ok(storeStockService.searchMovements(storeUid, medicineUid, kind, pageable));
    }
}
