package com.otapp.hmis.engine.pharmacy.stock.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.AdjustStockRequest;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.ReceiveStockRequest;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.StockBalanceDto;
import com.otapp.hmis.engine.pharmacy.stock.application.StockDtos.StockMovementDto;
import com.otapp.hmis.engine.pharmacy.stock.application.StockService;
import com.otapp.hmis.engine.pharmacy.stock.domain.StockMovementKind;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Pharmacy stock")
@RestController
@RequestMapping("/pharmacy")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PHARMACY_ACCESS')")
public class StockController {

    private final StockService stockService;

    @GetMapping("/pharmacies/uid/{pharmacyUid}/stock")
    public ResponseEntity<List<StockBalanceDto>> listBalances(@PathVariable String pharmacyUid) {
        return ResponseEntity.ok(stockService.listBalances(pharmacyUid));
    }

    @PostMapping("/pharmacies/uid/{pharmacyUid}/stock/receive")
    public ResponseEntity<StockBalanceDto> receive(@PathVariable String pharmacyUid,
                                                   @Valid @RequestBody ReceiveStockRequest request) {
        return ResponseEntity.ok(stockService.receive(pharmacyUid, request));
    }

    @PostMapping("/pharmacies/uid/{pharmacyUid}/stock/adjust")
    public ResponseEntity<StockBalanceDto> adjust(@PathVariable String pharmacyUid,
                                                  @Valid @RequestBody AdjustStockRequest request) {
        return ResponseEntity.ok(stockService.adjust(pharmacyUid, request));
    }

    @PostMapping("/pharmacies/uid/{pharmacyUid}/dispense/uid/{prescriptionUid}")
    public ResponseEntity<StockMovementDto> dispense(@PathVariable String pharmacyUid,
                                                     @PathVariable String prescriptionUid) {
        return ResponseEntity.ok(stockService.dispense(pharmacyUid, prescriptionUid));
    }

    @PostMapping("/pharmacies/uid/{pharmacyUid}/dispense-sale-line/uid/{saleLineUid}")
    public ResponseEntity<StockMovementDto> dispenseSaleLine(@PathVariable String pharmacyUid,
                                                             @PathVariable String saleLineUid) {
        return ResponseEntity.ok(stockService.dispenseSaleLine(pharmacyUid, saleLineUid));
    }

    @GetMapping("/stock/movements")
    public ResponseEntity<PageResponse<StockMovementDto>> searchMovements(
            @RequestParam(required = false) String pharmacyUid,
            @RequestParam(required = false) String medicineUid,
            @RequestParam(required = false) StockMovementKind kind,
            Pageable pageable) {
        return ResponseEntity.ok(stockService.searchMovements(pharmacyUid, medicineUid, kind, pageable));
    }
}
