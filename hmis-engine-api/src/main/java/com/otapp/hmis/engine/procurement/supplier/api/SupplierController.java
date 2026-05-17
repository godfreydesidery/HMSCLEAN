package com.otapp.hmis.engine.procurement.supplier.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.procurement.supplier.application.SupplierDtos.CreateSupplierRequest;
import com.otapp.hmis.engine.procurement.supplier.application.SupplierDtos.SupplierDto;
import com.otapp.hmis.engine.procurement.supplier.application.SupplierDtos.UpdateSupplierRequest;
import com.otapp.hmis.engine.procurement.supplier.application.SupplierService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Suppliers")
@RestController
@RequestMapping("/procurement/suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PROCUREMENT_ACCESS')")
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    public ResponseEntity<SupplierDto> create(@Valid @RequestBody CreateSupplierRequest request) {
        SupplierDto created = supplierService.create(request);
        URI loc = UriComponentsBuilder.fromPath("/procurement/suppliers/{uid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<SupplierDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(supplierService.search(query, active, pageable));
    }

    @GetMapping("/{uid}")
    public ResponseEntity<SupplierDto> findByUid(@PathVariable String uid) {
        return ResponseEntity.ok(supplierService.findByUid(uid));
    }

    @PutMapping("/{uid}")
    public ResponseEntity<SupplierDto> update(@PathVariable String uid,
                                              @Valid @RequestBody UpdateSupplierRequest request) {
        return ResponseEntity.ok(supplierService.update(uid, request));
    }

    @PutMapping("/{uid}/active")
    public ResponseEntity<SupplierDto> setActive(@PathVariable String uid, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(supplierService.setActive(uid, request.active()));
    }

    public record ActiveRequest(boolean active) {}
}
