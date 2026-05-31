package com.otapp.hmis.engine.billing.refund.application;

import com.otapp.hmis.engine.billing.invoice.application.SettlementDispatcher;
import com.otapp.hmis.engine.billing.invoice.domain.Invoice;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceRepository;
import com.otapp.hmis.engine.billing.refund.application.RefundDtos.CreateRefundRequest;
import com.otapp.hmis.engine.billing.refund.application.RefundDtos.RefundDto;
import com.otapp.hmis.engine.billing.refund.domain.Refund;
import com.otapp.hmis.engine.billing.refund.domain.RefundRepository;
import com.otapp.hmis.engine.billing.refund.infrastructure.RefundNumberGenerator;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository repository;
    private final InvoiceRepository invoiceRepository;
    private final RefundNumberGenerator numberGenerator;
    private final SettlementDispatcher settlementDispatcher;

    @Transactional
    public RefundDto raise(String invoiceUid, CreateRefundRequest request) {
        Invoice invoice = invoiceRepository.findByUid(invoiceUid)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + invoiceUid));
        invoice.applyRefund(request.amount());
        // A refund can roll an admission invoice back to a positive balance —
        // re-arm the bill-clearance gate so closure is blocked again.
        settlementDispatcher.onInvoiceMaybeSettled(invoice);
        Refund refund = repository.save(new Refund(
                numberGenerator.next(),
                invoice.getUid(),
                request.amount(),
                invoice.getCurrency(),
                request.method(),
                request.reason(),
                emptyToNull(request.description()),
                emptyToNull(request.reference()),
                currentUsername()));
        return toDto(refund);
    }

    @Transactional(readOnly = true)
    public List<RefundDto> listForInvoice(String invoiceUid) {
        return repository.findByInvoiceUidOrderByRefundedAtDesc(invoiceUid).stream()
                .map(RefundService::toDto)
                .toList();
    }

    private static RefundDto toDto(Refund r) {
        return new RefundDto(
                r.getUid(), r.getRefundNo(), r.getInvoiceUid(),
                r.getAmount(), r.getCurrency(),
                r.getMethod(), r.getReason(),
                r.getDescription(), r.getReference(),
                r.getRefundedByUsername(), r.getRefundedAt(),
                r.getCreatedAt());
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new BusinessRuleException("Authenticated user required");
        }
        return auth.getName();
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
