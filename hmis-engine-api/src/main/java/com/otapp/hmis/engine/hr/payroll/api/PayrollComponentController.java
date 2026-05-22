package com.otapp.hmis.engine.hr.payroll.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.ComputePayrollRequest;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.ComputedPayrollDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.CreatePayrollComponentRequest;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.PayrollComponentDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.SetActiveRequest;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentDtos.UpdatePayrollComponentRequest;
import com.otapp.hmis.engine.hr.payroll.application.PayrollComponentService;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollComponentType;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "HR — payroll components",
     description = "Configurable earning/deduction components + payroll auto-prefill compute.")
@RestController
@RequestMapping("/hr/payroll")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('HR_ACCESS')")
public class PayrollComponentController {

    private final PayrollComponentService service;

    @GetMapping("/components")
    public ResponseEntity<PageResponse<PayrollComponentDto>> search(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) PayrollComponentType type,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(active, type, pageable));
    }

    @PostMapping("/components")
    public ResponseEntity<PayrollComponentDto> create(@Valid @RequestBody CreatePayrollComponentRequest request) {
        PayrollComponentDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/hr/payroll/components/uid/{uid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping("/components/uid/{uid}")
    public ResponseEntity<PayrollComponentDto> findByUid(@PathVariable String uid) {
        return ResponseEntity.ok(service.findByUid(uid));
    }

    @PutMapping("/components/uid/{uid}")
    public ResponseEntity<PayrollComponentDto> update(@PathVariable String uid,
                                                      @Valid @RequestBody UpdatePayrollComponentRequest request) {
        return ResponseEntity.ok(service.update(uid, request));
    }

    @PutMapping("/components/uid/{uid}/active")
    public ResponseEntity<PayrollComponentDto> setActive(@PathVariable String uid,
                                                         @Valid @RequestBody SetActiveRequest request) {
        return ResponseEntity.ok(service.setActive(uid, request.active()));
    }

    @DeleteMapping("/components/uid/{uid}")
    public ResponseEntity<Void> delete(@PathVariable String uid) {
        service.delete(uid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/compute")
    public ResponseEntity<ComputedPayrollDto> compute(@Valid @RequestBody ComputePayrollRequest request) {
        return ResponseEntity.ok(service.compute(request));
    }
}
