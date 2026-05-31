package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.AdmissionBillingSummaryDto;
import com.otapp.hmis.engine.billing.invoice.application.InvoiceDtos.AdmissionOutstandingDto;
import com.otapp.hmis.engine.billing.invoice.domain.Invoice;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceRepository;
import com.otapp.hmis.engine.common.error.NotFoundException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Billing-owned read side of the admission bill-clearance gate (legacy
 * {@code PatientResource.get_discharge/referral/deceased_summary}: closure is blocked
 * while any admission bill is unpaid). The authoritative <em>enforcement</em> lives in
 * the encounter module as the local {@code Admission.billsCleared} flag that this module
 * maintains via {@link SettlementDispatcher} (billing -> encounter, the only allowed
 * direction). These methods just expose the same picture for the cashier / ward-admin UI;
 * they never query encounter.
 */
@Service
@RequiredArgsConstructor
public class AdmissionBillGate {

    private final InvoiceRepository invoiceRepository;

    /** O(1) outstanding check used by the cashier UI gate display. */
    @Transactional(readOnly = true)
    public boolean hasOutstandingAdmissionBills(String admissionUid) {
        return invoiceRepository.existsOutstandingForAdmission(admissionUid);
    }

    @Transactional(readOnly = true)
    public AdmissionOutstandingDto outstanding(String admissionUid) {
        Invoice invoice = invoiceRepository.findByAdmissionUid(admissionUid).orElse(null);
        BigDecimal balance = invoice == null ? BigDecimal.ZERO : invoice.balance();
        boolean hasOutstanding = invoiceRepository.existsOutstandingForAdmission(admissionUid);
        return new AdmissionOutstandingDto(hasOutstanding, balance);
    }

    @Transactional(readOnly = true)
    public AdmissionBillingSummaryDto billingSummary(String admissionUid) {
        Invoice invoice = invoiceRepository.findByAdmissionUid(admissionUid)
                .orElseThrow(() -> new NotFoundException(
                        "No admission invoice for admission: " + admissionUid));
        return new AdmissionBillingSummaryDto(
                invoice.getId(),
                invoice.getUid(),
                invoice.getInvoiceNo(),
                invoice.getStatus(),
                invoice.getSubtotal(),
                invoice.getTotalPaid(),
                invoice.getTotalCredited(),
                invoice.balance(),
                invoice.balance().signum() == 0);
    }
}
