package com.otapp.hmis.engine.procurement.supplierinvoice.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrder;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderLine;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderLineRepository;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderRepository;
import com.otapp.hmis.engine.procurement.supplier.domain.Supplier;
import com.otapp.hmis.engine.procurement.supplier.domain.SupplierRepository;
import com.otapp.hmis.engine.procurement.supplierinvoice.application.SupplierInvoiceDtos.CreateInvoiceLineRequest;
import com.otapp.hmis.engine.procurement.supplierinvoice.application.SupplierInvoiceDtos.CreateSupplierInvoiceRequest;
import com.otapp.hmis.engine.procurement.supplierinvoice.application.SupplierInvoiceDtos.InvoiceLineDto;
import com.otapp.hmis.engine.procurement.supplierinvoice.application.SupplierInvoiceDtos.MarkPaidRequest;
import com.otapp.hmis.engine.procurement.supplierinvoice.application.SupplierInvoiceDtos.ReasonRequest;
import com.otapp.hmis.engine.procurement.supplierinvoice.application.SupplierInvoiceDtos.SupplierInvoiceDto;
import com.otapp.hmis.engine.procurement.supplierinvoice.application.SupplierInvoiceDtos.SupplierInvoiceSummary;
import com.otapp.hmis.engine.procurement.supplierinvoice.domain.SupplierInvoice;
import com.otapp.hmis.engine.procurement.supplierinvoice.domain.SupplierInvoiceLine;
import com.otapp.hmis.engine.procurement.supplierinvoice.domain.SupplierInvoiceLineRepository;
import com.otapp.hmis.engine.procurement.supplierinvoice.domain.SupplierInvoiceRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Three-way match (PROCESS.md §17.9). Holds the supplier invoice header
 * + lines and runs the match at the APPROVED transition: for each line,
 * invoiced ≤ received ≤ ordered across all approved invoices for the
 * PO. The matching is cumulative — PurchaseOrderLine carries
 * {@code invoicedQuantity} so multiple invoices can split a single
 * line.
 */
@Service
@RequiredArgsConstructor
public class SupplierInvoiceService {

    private final SupplierInvoiceRepository invoiceRepository;
    private final SupplierInvoiceLineRepository lineRepository;
    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderLineRepository orderLineRepository;
    private final SupplierRepository supplierRepository;
    private final MedicineRepository medicineRepository;

    @Transactional
    public SupplierInvoiceDto create(CreateSupplierInvoiceRequest request) {
        PurchaseOrder order = orderRepository.findByUid(request.orderUid())
                .orElseThrow(() -> new NotFoundException("Purchase order not found: " + request.orderUid()));
        Supplier supplier = supplierRepository.findByUid(order.getSupplierUid())
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + order.getSupplierUid()));

        if (invoiceRepository.existsBySupplierUidAndSupplierInvoiceNo(
                supplier.getUid(), request.supplierInvoiceNo().trim())) {
            throw new ConflictException("Supplier invoice already recorded: " + request.supplierInvoiceNo());
        }

        SupplierInvoice invoice = invoiceRepository.save(new SupplierInvoice(
                supplier.getUid(),
                order.getUid(),
                request.supplierInvoiceNo().trim(),
                request.invoiceDate(),
                request.dueDate(),
                emptyToNull(request.currency()),
                emptyToNull(request.notes())));

        BigDecimal total = BigDecimal.ZERO;
        for (CreateInvoiceLineRequest lineReq : request.lines()) {
            PurchaseOrderLine poLine = orderLineRepository.findByUid(lineReq.poLineUid())
                    .orElseThrow(() -> new NotFoundException("PO line not found: " + lineReq.poLineUid()));
            if (!poLine.getOrderUid().equals(order.getUid())) {
                throw new BusinessRuleException(
                        "PO line " + lineReq.poLineUid() + " does not belong to this order");
            }
            SupplierInvoiceLine saved = lineRepository.save(new SupplierInvoiceLine(
                    invoice.getUid(),
                    poLine.getUid(),
                    lineReq.invoicedQuantity(),
                    lineReq.unitCost()));
            total = total.add(saved.getLineAmount());
        }
        invoice.setTotalAmount(total);
        return toDto(invoice);
    }

    @Transactional
    public SupplierInvoiceDto submit(String invoiceUid) {
        SupplierInvoice invoice = loadOrThrow(invoiceUid);
        if (lineRepository.findAllByInvoiceUidOrderByCreatedAtAsc(invoice.getUid()).isEmpty()) {
            throw new BusinessRuleException("Cannot submit an invoice with no lines");
        }
        invoice.submit(currentUsername());
        return toDto(invoice);
    }

    /**
     * Approval = three-way match. Validates each line against the live
     * PO-line state (cumulative across already-approved invoices), then
     * applies {@code recordInvoice} so subsequent invoices see the
     * updated invoiced totals.
     */
    @Transactional
    public SupplierInvoiceDto approve(String invoiceUid) {
        SupplierInvoice invoice = loadOrThrow(invoiceUid);
        List<SupplierInvoiceLine> lines = lineRepository.findAllByInvoiceUidOrderByCreatedAtAsc(invoice.getUid());
        for (SupplierInvoiceLine line : lines) {
            PurchaseOrderLine poLine = orderLineRepository.findByUid(line.getPoLineUid())
                    .orElseThrow(() -> new NotFoundException("PO line not found: " + line.getPoLineUid()));
            // Throws BusinessRuleException with full match-failure detail if the
            // three-way invariants don't hold.
            poLine.recordInvoice(line.getInvoicedQuantity());
        }
        invoice.approve(currentUsername());
        return toDto(invoice);
    }

    @Transactional
    public SupplierInvoiceDto markPaid(String invoiceUid, MarkPaidRequest request) {
        SupplierInvoice invoice = loadOrThrow(invoiceUid);
        invoice.markPaid(currentUsername(), request.method(), emptyToNull(request.reference()));
        return toDto(invoice);
    }

    @Transactional
    public SupplierInvoiceDto reject(String invoiceUid, ReasonRequest request) {
        SupplierInvoice invoice = loadOrThrow(invoiceUid);
        invoice.reject(currentUsername(), emptyToNull(request == null ? null : request.reason()));
        return toDto(invoice);
    }

    @Transactional
    public SupplierInvoiceDto cancel(String invoiceUid) {
        SupplierInvoice invoice = loadOrThrow(invoiceUid);
        invoice.cancel();
        return toDto(invoice);
    }

    @Transactional(readOnly = true)
    public SupplierInvoiceDto findByUid(String invoiceUid) {
        return toDto(loadOrThrow(invoiceUid));
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierInvoiceSummary> search(String query,
                                                        com.otapp.hmis.engine.procurement.supplierinvoice.domain.SupplierInvoiceStatus status,
                                                        String supplierUid, String orderUid,
                                                        Pageable pageable) {
        return PageResponse.from(
                invoiceRepository.search(emptyToNull(query), status,
                                emptyToNull(supplierUid), emptyToNull(orderUid), pageable)
                        .map(this::toSummary));
    }

    private SupplierInvoice loadOrThrow(String uid) {
        return invoiceRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Supplier invoice not found: " + uid));
    }

    private SupplierInvoiceDto toDto(SupplierInvoice i) {
        Supplier supplier = supplierRepository.findByUid(i.getSupplierUid()).orElse(null);
        PurchaseOrder order = orderRepository.findByUid(i.getOrderUid()).orElse(null);
        List<SupplierInvoiceLine> lines = lineRepository.findAllByInvoiceUidOrderByCreatedAtAsc(i.getUid());
        return new SupplierInvoiceDto(
                i.getUid(),
                i.getSupplierUid(), supplier == null ? null : supplier.getName(),
                i.getOrderUid(),    order    == null ? null : order.getOrderNo(),
                i.getSupplierInvoiceNo(),
                i.getInvoiceDate(), i.getDueDate(),
                i.getCurrency(), i.getTotalAmount(),
                i.getStatus(),
                i.getSubmittedAt(),  i.getSubmittedByUsername(),
                i.getApprovedAt(),   i.getApprovedByUsername(),
                i.getPaidAt(),       i.getPaidByUsername(),
                i.getPaymentMethod(), i.getPaymentReference(),
                i.getRejectedAt(),   i.getRejectedByUsername(), i.getRejectReason(),
                i.getCancelledAt(),
                i.getNotes(),
                i.getCreatedAt(), i.getUpdatedAt(),
                lines.stream().map(this::toLineDto).toList());
    }

    private InvoiceLineDto toLineDto(SupplierInvoiceLine line) {
        PurchaseOrderLine poLine = orderLineRepository.findByUid(line.getPoLineUid()).orElse(null);
        Medicine medicine = poLine == null ? null
                : medicineRepository.findByUid(poLine.getMedicineUid()).orElse(null);
        return new InvoiceLineDto(
                line.getUid(),
                line.getPoLineUid(),
                poLine == null ? null : poLine.getMedicineUid(),
                medicine == null ? null : medicine.getCode(),
                medicine == null ? null : medicine.getName(),
                poLine == null ? 0 : poLine.getOrderedQuantity(),
                poLine == null ? 0 : poLine.getReceivedQuantity(),
                poLine == null ? 0 : poLine.getInvoicedQuantity(),
                line.getInvoicedQuantity(),
                line.getUnitCost(),
                line.getLineAmount());
    }

    private SupplierInvoiceSummary toSummary(SupplierInvoice i) {
        Supplier supplier = supplierRepository.findByUid(i.getSupplierUid()).orElse(null);
        PurchaseOrder order = orderRepository.findByUid(i.getOrderUid()).orElse(null);
        return new SupplierInvoiceSummary(
                i.getUid(),
                supplier == null ? null : supplier.getName(),
                order    == null ? null : order.getOrderNo(),
                i.getSupplierInvoiceNo(),
                i.getInvoiceDate(),
                i.getCurrency(), i.getTotalAmount(),
                i.getStatus(),
                i.getCreatedAt());
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
