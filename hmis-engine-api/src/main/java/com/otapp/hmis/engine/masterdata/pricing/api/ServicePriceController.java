package com.otapp.hmis.engine.masterdata.pricing.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceDtos.ServiceCoverageDto;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceDtos.ServicePriceDto;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceDtos.SetServicePriceRequest;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceDtos.UpdateServicePriceRequest;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceService;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Service prices")
@RestController
@RequestMapping("/masterdata/service-prices")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class ServicePriceController {

    private final ServicePriceService service;

    @GetMapping
    public ResponseEntity<PageResponse<ServicePriceDto>> search(
            @RequestParam(required = false) String planUid,
            @RequestParam(required = false, defaultValue = "false") boolean cashOnly,
            @RequestParam(required = false) ServiceKind kind,
            @RequestParam(required = false) String serviceUid,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String query,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(planUid, cashOnly, kind, serviceUid, currency, query, pageable));
    }

    /** Create a price for a (payer, service, currency) cell. Plan UID empty/null = cash price.
     *  409 Conflict if that exact cell already exists — edit the existing row instead. */
    @PostMapping
    public ResponseEntity<ServicePriceDto> create(@Valid @RequestBody SetServicePriceRequest request) {
        ServicePriceDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/service-prices/uid/{uid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    /** Per-plan coverage grid: the plan's price cells with their covered flag. 404 if plan unknown. */
    @GetMapping("/coverage")
    public ResponseEntity<PageResponse<ServiceCoverageDto>> coverage(
            @RequestParam String planUid,
            @RequestParam(required = false) ServiceKind kind,
            @RequestParam(required = false) String query,
            Pageable pageable) {
        return ResponseEntity.ok(service.coverageGrid(planUid, kind, query, pageable));
    }

    /** Update an existing price (amount / band / note / covered) by uid. Key fields are immutable. */
    @PutMapping("/uid/{servicePriceUid}")
    public ResponseEntity<ServicePriceDto> update(@PathVariable String servicePriceUid,
                                                  @Valid @RequestBody UpdateServicePriceRequest request) {
        return ResponseEntity.ok(service.update(servicePriceUid, request));
    }

    /** Cover the service this plan price addresses. 422 if the cell is unpriced or a cash row; 404 if unknown. */
    @PostMapping("/uid/{servicePriceUid}/cover")
    public ResponseEntity<ServicePriceDto> cover(@PathVariable String servicePriceUid) {
        return ResponseEntity.ok(service.cover(servicePriceUid));
    }

    /** Uncover the service this plan price addresses. 404 if unknown. */
    @PostMapping("/uid/{servicePriceUid}/uncover")
    public ResponseEntity<ServicePriceDto> uncover(@PathVariable String servicePriceUid) {
        return ResponseEntity.ok(service.uncover(servicePriceUid));
    }

    @DeleteMapping("/uid/{servicePriceUid}")
    public ResponseEntity<Void> delete(@PathVariable String servicePriceUid) {
        service.delete(servicePriceUid);
        return ResponseEntity.noContent().build();
    }
}
