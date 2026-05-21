package com.otapp.hmis.engine.billing.creditnote.application;

import com.otapp.hmis.engine.billing.creditnote.application.CreditNoteDtos.CreateCreditNoteRequest;
import com.otapp.hmis.engine.billing.creditnote.application.CreditNoteDtos.CreditNoteDto;
import com.otapp.hmis.engine.billing.creditnote.domain.CreditNote;
import com.otapp.hmis.engine.billing.creditnote.domain.CreditNoteRepository;
import com.otapp.hmis.engine.billing.creditnote.infrastructure.CreditNoteNumberGenerator;
import com.otapp.hmis.engine.billing.invoice.application.SettlementDispatcher;
import com.otapp.hmis.engine.billing.invoice.domain.Invoice;
import com.otapp.hmis.engine.billing.invoice.domain.InvoiceRepository;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreditNoteService {

    private final CreditNoteRepository repository;
    private final InvoiceRepository invoiceRepository;
    private final CreditNoteNumberGenerator numberGenerator;
    private final SettlementDispatcher settlementDispatcher;

    @Transactional
    public CreditNoteDto raise(String invoiceUid, CreateCreditNoteRequest request) {
        Invoice invoice = invoiceRepository.findByUid(invoiceUid)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + invoiceUid));
        invoice.applyCreditNote(request.amount());
        // A full write-down can settle the invoice — propagate to encounter.
        settlementDispatcher.onInvoiceMaybeSettled(invoice);
        CreditNote note = repository.save(new CreditNote(
                numberGenerator.next(),
                invoice.getUid(),
                request.amount(),
                invoice.getCurrency(),
                request.reason(),
                emptyToNull(request.description()),
                currentUsername()));
        return toDto(note);
    }

    @Transactional(readOnly = true)
    public List<CreditNoteDto> listForInvoice(String invoiceUid) {
        return repository.findByInvoiceUidOrderByIssuedAtDesc(invoiceUid).stream()
                .map(CreditNoteService::toDto)
                .toList();
    }

    private static CreditNoteDto toDto(CreditNote n) {
        return new CreditNoteDto(
                n.getUid(), n.getNoteNo(), n.getInvoiceUid(),
                n.getAmount(), n.getCurrency(), n.getReason(),
                n.getDescription(), n.getIssuedByUsername(), n.getIssuedAt(),
                n.getCreatedAt());
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
