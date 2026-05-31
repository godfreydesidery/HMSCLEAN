package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.InvoiceDto;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceStatus;
import com.otapp.hmis.engine.encounter.admission.domain.AdmissionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily ward-day accrual (PROCESS_MISMATCHES.md M23, reopened): the legacy system
 * accrued a fresh ward-bed charge for every day a patient stayed admitted. Here the
 * admission invoice is rebuilt each night so its ward-day line tracks the elapsed
 * stay; {@code generateForAdmission} re-arms the discharge bill-clearance gate, so a
 * long stay cannot be closed until the accrued charge is settled.
 *
 * <p>Each admission is billed in its own transaction (the call goes through the
 * {@code InvoiceService} proxy) so one bad row never aborts the rest of the run.
 * Admissions whose invoice already took a payment (or was cancelled) are frozen and
 * {@code generateForAdmission} rejects them — that is expected and skipped.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class AdmissionAccrualJob {

    private final InvoiceService invoiceService;
    private final AdmissionRepository admissionRepository;

    /** 01:00 every day — quiet hours, after the calendar day has rolled over. */
    @Scheduled(cron = "0 0 1 * * *")
    void accrueWardDays() {
        List<String> admittedUids = admissionRepository.findAdmittedUids();
        int accrued = 0;
        for (String admissionUid : admittedUids) {
            try {
                InvoiceDto invoice = invoiceService.generateForAdmission(admissionUid);
                if (invoice.status() == InvoiceStatus.DRAFT) {
                    invoiceService.issue(invoice.uid());
                }
                accrued++;
            } catch (RuntimeException e) {
                // Frozen (paid / cancelled) invoices and transient failures are
                // skipped — the next run retries the rest.
                log.debug("Skipped ward-day accrual for admission {}: {}", admissionUid, e.getMessage());
            }
        }
        if (!admittedUids.isEmpty()) {
            log.info("Ward-day accrual: refreshed {}/{} admitted invoices", accrued, admittedUids.size());
        }
    }
}
