package com.otapp.hmis.engine.procurement.pricelist.api;

import com.otapp.hmis.engine.procurement.pricelist.application.SupplierItemPriceDtos.CreateSupplierItemPriceRequest;
import com.otapp.hmis.engine.procurement.pricelist.application.SupplierItemPriceDtos.SupplierItemPriceDto;
import com.otapp.hmis.engine.procurement.pricelist.application.SupplierItemPriceDtos.UpdateSupplierItemPriceRequest;
import com.otapp.hmis.engine.procurement.pricelist.application.SupplierItemPriceService;
import com.otapp.hmis.engine.common.error.NotFoundException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Supplier item prices")
@RestController
@RequestMapping("/procurement/suppliers/uid/{supplierUid}/prices")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PROCUREMENT_ACCESS')")
public class SupplierItemPriceController {

    private final SupplierItemPriceService priceService;

    @GetMapping
    public ResponseEntity<List<SupplierItemPriceDto>> list(@PathVariable String supplierUid) {
        return ResponseEntity.ok(priceService.listForSupplier(supplierUid));
    }

    /**
     * The supplier's CURRENT contracted quote for one medicine — the price the
     * purchase-order add-line will lock onto. 404 when the supplier does not
     * currently quote the medicine (so the add-line UI can show "not valid").
     */
    @GetMapping("/medicines/uid/{medicineUid}/current")
    public ResponseEntity<SupplierItemPriceDto> currentForMedicine(@PathVariable String supplierUid,
                                                                   @PathVariable String medicineUid) {
        return ResponseEntity.ok(priceService.findContractedPrice(supplierUid, medicineUid)
                .orElseThrow(() -> new NotFoundException(
                        "No current contracted price for this supplier and medicine")));
    }

    @PostMapping
    public ResponseEntity<SupplierItemPriceDto> create(@PathVariable String supplierUid,
                                                       @Valid @RequestBody CreateSupplierItemPriceRequest request) {
        SupplierItemPriceDto created = priceService.create(supplierUid, request);
        URI loc = UriComponentsBuilder
                .fromPath("/procurement/suppliers/uid/{supplierUid}/prices/uid/{priceUid}")
                .buildAndExpand(supplierUid, created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @PutMapping("/uid/{priceUid}")
    public ResponseEntity<SupplierItemPriceDto> update(@PathVariable String supplierUid,
                                                       @PathVariable String priceUid,
                                                       @Valid @RequestBody UpdateSupplierItemPriceRequest request) {
        return ResponseEntity.ok(priceService.update(supplierUid, priceUid, request));
    }

    @PutMapping("/uid/{priceUid}/active")
    public ResponseEntity<SupplierItemPriceDto> setActive(@PathVariable String supplierUid,
                                                          @PathVariable String priceUid,
                                                          @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(priceService.setActive(supplierUid, priceUid, request.active()));
    }

    @DeleteMapping("/uid/{priceUid}")
    public ResponseEntity<Void> delete(@PathVariable String supplierUid,
                                       @PathVariable String priceUid) {
        priceService.delete(supplierUid, priceUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
