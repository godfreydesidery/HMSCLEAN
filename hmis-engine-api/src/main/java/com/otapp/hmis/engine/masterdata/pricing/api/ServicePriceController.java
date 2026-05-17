package com.otapp.hmis.engine.masterdata.pricing.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceDtos.ServicePriceDto;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceDtos.SetServicePriceRequest;
import com.otapp.hmis.engine.masterdata.pricing.application.ServicePriceService;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(required = false) ServiceKind kind,
            @RequestParam(required = false) String serviceUid,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(planUid, kind, serviceUid, pageable));
    }

    /** Upsert a price for a (plan, service) cell. Plan UID empty/null = cash price. */
    @PutMapping
    public ResponseEntity<ServicePriceDto> setPrice(@Valid @RequestBody SetServicePriceRequest request) {
        return ResponseEntity.ok(service.setPrice(request));
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> delete(@PathVariable String uid) {
        service.delete(uid);
        return ResponseEntity.noContent().build();
    }
}
