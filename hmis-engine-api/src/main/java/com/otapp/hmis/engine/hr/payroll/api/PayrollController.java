package com.otapp.hmis.engine.hr.payroll.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.hr.payroll.application.PayrollDtos.CancelPayrollPeriodRequest;
import com.otapp.hmis.engine.hr.payroll.application.PayrollDtos.CreatePayrollPeriodRequest;
import com.otapp.hmis.engine.hr.payroll.application.PayrollDtos.ImportEmployeesResultDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollDtos.PayrollItemDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollDtos.PayrollPeriodDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollDtos.PayrollPeriodWithItemsDto;
import com.otapp.hmis.engine.hr.payroll.application.PayrollDtos.UpsertPayrollItemRequest;
import com.otapp.hmis.engine.hr.payroll.application.PayrollService;
import com.otapp.hmis.engine.hr.payroll.domain.PayrollPeriodStatus;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "HR — payroll", description = "Period + per-employee items skeleton. No statutory tax tables.")
@RestController
@RequestMapping("/hr/payroll")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('HR_ACCESS')")
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/periods")
    public ResponseEntity<PayrollPeriodDto> createPeriod(@Valid @RequestBody CreatePayrollPeriodRequest request) {
        PayrollPeriodDto created = payrollService.createPeriod(request);
        URI loc = UriComponentsBuilder.fromPath("/hr/payroll/periods/uid/{periodUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping("/periods")
    public ResponseEntity<PageResponse<PayrollPeriodDto>> search(
            @RequestParam(required = false) PayrollPeriodStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(payrollService.search(status, pageable));
    }

    @GetMapping("/periods/uid/{periodUid}")
    public ResponseEntity<PayrollPeriodWithItemsDto> findByUid(@PathVariable String periodUid) {
        return ResponseEntity.ok(payrollService.findByUid(periodUid));
    }

    @PostMapping("/periods/uid/{periodUid}/items")
    public ResponseEntity<PayrollItemDto> upsertItem(@PathVariable String periodUid,
                                                     @Valid @RequestBody UpsertPayrollItemRequest request) {
        return ResponseEntity.ok(payrollService.upsertItem(periodUid, request));
    }

    @DeleteMapping("/periods/uid/{periodUid}/items/employee/uid/{employeeUid}")
    public ResponseEntity<Void> removeItem(@PathVariable String periodUid, @PathVariable String employeeUid) {
        payrollService.removeItem(periodUid, employeeUid);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bulk-seed a payroll item per ACTIVE + payable employee, prefilled from
     * their {@code Employee.basicSalary} (legacy import_employees). No body.
     */
    @PostMapping("/periods/uid/{periodUid}/import-employees")
    public ResponseEntity<ImportEmployeesResultDto> importEmployees(@PathVariable String periodUid) {
        return ResponseEntity.ok(payrollService.importEmployees(periodUid));
    }

    @PostMapping("/periods/uid/{periodUid}/verify")
    public ResponseEntity<PayrollPeriodDto> verify(@PathVariable String periodUid) {
        return ResponseEntity.ok(payrollService.verify(periodUid));
    }

    @PostMapping("/periods/uid/{periodUid}/approve")
    public ResponseEntity<PayrollPeriodDto> approve(@PathVariable String periodUid) {
        return ResponseEntity.ok(payrollService.approve(periodUid));
    }

    @PostMapping("/periods/uid/{periodUid}/pay")
    public ResponseEntity<PayrollPeriodDto> markPaid(@PathVariable String periodUid) {
        return ResponseEntity.ok(payrollService.markPaid(periodUid));
    }

    @PostMapping("/periods/uid/{periodUid}/cancel")
    public ResponseEntity<PayrollPeriodDto> cancel(@PathVariable String periodUid,
                                                   @Valid @RequestBody(required = false) CancelPayrollPeriodRequest request) {
        return ResponseEntity.ok(payrollService.cancel(periodUid, request));
    }
}
