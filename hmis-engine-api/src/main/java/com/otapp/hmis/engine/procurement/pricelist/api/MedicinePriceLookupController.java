package com.otapp.hmis.engine.procurement.pricelist.api;

import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.procurement.pricelist.application.SupplierItemPriceDtos.SupplierItemPriceDto;
import com.otapp.hmis.engine.procurement.pricelist.application.SupplierItemPriceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Read-only comparison-shopping endpoints anchored on a medicine —
 * shows what each supplier is currently quoting. Procurement uses this
 * when picking who to put the next LPO with.
 */
@Tag(name = "Supplier item prices (per medicine)")
@RestController
@RequestMapping("/procurement/medicines/uid/{medicineUid}/prices")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PROCUREMENT_ACCESS')")
public class MedicinePriceLookupController {

    private final SupplierItemPriceService priceService;

    /** Full history across all suppliers — both expired and active quotes. */
    @GetMapping
    public ResponseEntity<List<SupplierItemPriceDto>> listAll(@PathVariable String medicineUid) {
        return ResponseEntity.ok(priceService.listForMedicine(medicineUid));
    }

    /** Currently-valid quotes only (active AND today within window), cheapest first. */
    @GetMapping("/active")
    public ResponseEntity<List<SupplierItemPriceDto>> listActive(@PathVariable String medicineUid) {
        return ResponseEntity.ok(priceService.listActiveForMedicine(medicineUid));
    }

    /** Convenience: cheapest currently-valid quote, 404 if none. */
    @GetMapping("/best")
    public ResponseEntity<SupplierItemPriceDto> currentBest(@PathVariable String medicineUid) {
        return priceService.findCurrentBest(medicineUid)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException(
                        "No currently-valid supplier quote for medicine: " + medicineUid));
    }
}
