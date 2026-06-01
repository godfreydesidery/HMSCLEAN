package com.otapp.hmis.engine.reporting.api;

import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.BedOccupancyEntry;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.CollectionsReportDto;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.ExpiringBatchEntry;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.IpdRegisterEntry;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.PharmacySalesDto;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.RevenueByModeDto;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.RevenueSummaryDto;
import com.otapp.hmis.engine.reporting.application.ReportingDtos.StockOutEntry;
import com.otapp.hmis.engine.reporting.application.ReportingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reports")
@RestController
@RequestMapping("/reporting")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('REPORTING_ACCESS')")
public class ReportingController {

    private final ReportingService service;

    @GetMapping("/revenue")
    public ResponseEntity<RevenueSummaryDto> revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.revenueSummary(from, to));
    }

    @GetMapping("/revenue-by-mode")
    public ResponseEntity<RevenueByModeDto> revenueByMode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.revenueByMode(from, to));
    }

    @GetMapping("/collections")
    public ResponseEntity<CollectionsReportDto> collections(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.collections(from, to));
    }

    @GetMapping("/pharmacy-sales")
    public ResponseEntity<PharmacySalesDto> pharmacySales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.pharmacySales(from, to));
    }

    @GetMapping("/ipd-register")
    public ResponseEntity<List<IpdRegisterEntry>> ipdRegister(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String wardUid,
            @RequestParam(required = false) AdmissionStatus status) {
        return ResponseEntity.ok(service.ipdRegister(from, to, wardUid, status));
    }

    @GetMapping("/bed-occupancy")
    public ResponseEntity<List<BedOccupancyEntry>> bedOccupancy() {
        return ResponseEntity.ok(service.bedOccupancy());
    }

    @GetMapping("/stock-out")
    public ResponseEntity<List<StockOutEntry>> stockOut(
            @RequestParam(defaultValue = "0") int threshold) {
        return ResponseEntity.ok(service.stockOut(threshold));
    }

    @GetMapping("/expiring-batches")
    public ResponseEntity<List<ExpiringBatchEntry>> expiringBatches(
            @RequestParam(defaultValue = "30") int daysAhead) {
        return ResponseEntity.ok(service.expiringBatches(daysAhead));
    }
}
