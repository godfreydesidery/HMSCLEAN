package com.otapp.hmis.engine.hr.employee.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.hr.employee.application.EmployeeDtos.ClinicianPerformanceDto;
import com.otapp.hmis.engine.hr.employee.application.EmployeeDtos.CreateEmployeeRequest;
import com.otapp.hmis.engine.hr.employee.application.EmployeeDtos.EmployeeDto;
import com.otapp.hmis.engine.hr.employee.application.EmployeeDtos.SetStatusRequest;
import com.otapp.hmis.engine.hr.employee.application.EmployeeDtos.TerminateEmployeeRequest;
import com.otapp.hmis.engine.hr.employee.application.EmployeeDtos.UpdateCompensationRequest;
import com.otapp.hmis.engine.hr.employee.application.EmployeeDtos.UpdateEmployeeRequest;
import com.otapp.hmis.engine.hr.employee.application.EmployeeService;
import com.otapp.hmis.engine.hr.employee.domain.EmploymentStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Employees")
@RestController
@RequestMapping("/hr/employees")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('HR_ACCESS')")
public class EmployeeController {

    private final EmployeeService service;

    @PostMapping
    public ResponseEntity<EmployeeDto> create(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/hr/employees/uid/{employeeUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<EmployeeDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) String department,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, status, designation, department, pageable));
    }

    @GetMapping("/uid/{employeeUid}")
    public ResponseEntity<EmployeeDto> findByUid(@PathVariable String employeeUid) {
        return ResponseEntity.ok(service.findByUid(employeeUid));
    }

    @PutMapping("/uid/{employeeUid}")
    public ResponseEntity<EmployeeDto> update(@PathVariable String employeeUid,
                                              @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(service.update(employeeUid, request));
    }

    @PutMapping("/uid/{employeeUid}/compensation")
    public ResponseEntity<EmployeeDto> updateCompensation(@PathVariable String employeeUid,
                                                          @Valid @RequestBody UpdateCompensationRequest request) {
        return ResponseEntity.ok(service.updateCompensation(employeeUid, request));
    }

    @PutMapping("/uid/{employeeUid}/status")
    public ResponseEntity<EmployeeDto> setStatus(@PathVariable String employeeUid,
                                                 @Valid @RequestBody SetStatusRequest request) {
        return ResponseEntity.ok(service.setStatus(employeeUid, request));
    }

    @PostMapping("/uid/{employeeUid}/terminate")
    public ResponseEntity<EmployeeDto> terminate(@PathVariable String employeeUid,
                                                 @Valid @RequestBody TerminateEmployeeRequest request) {
        return ResponseEntity.ok(service.terminate(employeeUid, request));
    }

    @GetMapping("/uid/{employeeUid}/clinician-performance")
    public ResponseEntity<ClinicianPerformanceDto> clinicianPerformance(
            @PathVariable String employeeUid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.clinicianPerformance(employeeUid, from, to));
    }
}
