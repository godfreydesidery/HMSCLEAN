package com.otapp.hmis.engine.procurement.order.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.store.domain.Store;
import com.otapp.hmis.engine.masterdata.store.domain.StoreRepository;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.AddLineRequest;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.CancelPurchaseOrderRequest;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.CreatePurchaseOrderRequest;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.PurchaseOrderDto;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.PurchaseOrderLineDto;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.PurchaseOrderSummary;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.RejectPurchaseOrderRequest;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.UpdateLineRequest;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrder;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderLine;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderLineRepository;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderRepository;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderStatus;
import com.otapp.hmis.engine.procurement.order.infrastructure.PurchaseOrderNumberGenerator;
import com.otapp.hmis.engine.procurement.pricelist.application.SupplierItemPriceDtos.SupplierItemPriceDto;
import com.otapp.hmis.engine.procurement.pricelist.application.SupplierItemPriceService;
import com.otapp.hmis.engine.procurement.supplier.domain.Supplier;
import com.otapp.hmis.engine.procurement.supplier.domain.SupplierRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderLineRepository lineRepository;
    private final SupplierRepository supplierRepository;
    private final StoreRepository storeRepository;
    private final MedicineRepository medicineRepository;
    private final SupplierItemPriceService supplierItemPriceService;
    private final PurchaseOrderNumberGenerator numberGenerator;

    @Transactional
    public PurchaseOrderDto create(CreatePurchaseOrderRequest request) {
        Supplier supplier = activeSupplier(request.supplierUid());
        Store store = activeStore(request.storeUid());

        PurchaseOrder order = new PurchaseOrder(
                numberGenerator.next(),
                supplier.getUid(),
                store.getUid(),
                request.expectedDeliveryDate(),
                emptyToNull(request.notes()));
        orderRepository.save(order);
        return toDto(order);
    }

    @Transactional
    public PurchaseOrderDto addLine(String orderUid, AddLineRequest request) {
        PurchaseOrder order = loadOrThrow(orderUid);
        if (!order.isMutable()) {
            throw new BusinessRuleException("Cannot edit a " + order.getStatus() + " purchase order");
        }
        Medicine medicine = activeMedicine(request.medicineUid());

        // Legacy gate: the supplier must quote this item, and the line price is
        // COPIED from the supplier's current contracted quote (not client input).
        SupplierItemPriceDto quote = contractedQuoteOrThrow(order.getSupplierUid(), medicine.getUid());

        // Legacy: duplicate items on the same order are not allowed.
        boolean duplicate = lineRepository.findAllByOrderUidOrderByCreatedAtAsc(order.getUid()).stream()
                .anyMatch(l -> l.getMedicineUid().equals(medicine.getUid()));
        if (duplicate) {
            throw new ConflictException("Duplicates items are not allowed");
        }

        PurchaseOrderLine line = new PurchaseOrderLine(
                order.getUid(),
                medicine.getUid(),
                request.orderedQuantity(),
                quote.unitPrice(),
                quote.currency());
        lineRepository.save(line);
        return toDto(order);
    }

    @Transactional
    public PurchaseOrderDto updateLine(String orderUid, String lineUid, UpdateLineRequest request) {
        PurchaseOrder order = loadOrThrow(orderUid);
        if (!order.isMutable()) {
            throw new BusinessRuleException("Cannot edit a " + order.getStatus() + " purchase order");
        }
        PurchaseOrderLine line = lineRepository.findByUid(lineUid)
                .orElseThrow(() -> new NotFoundException("Line not found: " + lineUid));
        if (!line.getOrderUid().equals(order.getUid())) {
            throw new BusinessRuleException("Line does not belong to this purchase order");
        }
        // Qty stays editable; price is re-pulled server-side from the supplier's
        // current contracted quote (the gate still applies on edit).
        SupplierItemPriceDto quote = contractedQuoteOrThrow(order.getSupplierUid(), line.getMedicineUid());
        line.setOrderedQuantity(request.orderedQuantity());
        line.setUnitCost(quote.unitPrice());
        line.setCurrency(quote.currency());
        return toDto(order);
    }

    /**
     * Supplier-quoted gate (legacy {@code findBySupplierAndItem} → throw if
     * absent): returns the supplier's current contracted quote for the
     * medicine, or 422 "Item not valid for this supplier" when the supplier
     * does not (currently) quote it.
     */
    private SupplierItemPriceDto contractedQuoteOrThrow(String supplierUid, String medicineUid) {
        return supplierItemPriceService.findContractedPrice(supplierUid, medicineUid)
                .orElseThrow(() -> new BusinessRuleException("Item not valid for this supplier"));
    }

    @Transactional
    public PurchaseOrderDto removeLine(String orderUid, String lineUid) {
        PurchaseOrder order = loadOrThrow(orderUid);
        if (!order.isMutable()) {
            throw new BusinessRuleException("Cannot edit a " + order.getStatus() + " purchase order");
        }
        PurchaseOrderLine line = lineRepository.findByUid(lineUid)
                .orElseThrow(() -> new NotFoundException("Line not found: " + lineUid));
        if (!line.getOrderUid().equals(order.getUid())) {
            throw new BusinessRuleException("Line does not belong to this purchase order");
        }
        lineRepository.delete(line);
        return toDto(order);
    }

    @Transactional
    public PurchaseOrderDto verifyOrder(String orderUid) {
        PurchaseOrder order = loadOrThrow(orderUid);
        if (lineRepository.findAllByOrderUidOrderByCreatedAtAsc(orderUid).isEmpty()) {
            throw new BusinessRuleException("Cannot verify a purchase order with no lines");
        }
        order.verify();
        return toDto(order);
    }

    @Transactional
    public PurchaseOrderDto approveOrder(String orderUid) {
        PurchaseOrder order = loadOrThrow(orderUid);
        order.approve();
        return toDto(order);
    }

    @Transactional
    public PurchaseOrderDto markOrdered(String orderUid) {
        PurchaseOrder order = loadOrThrow(orderUid);
        order.markOrdered();
        return toDto(order);
    }

    @Transactional
    public PurchaseOrderDto rejectOrder(String orderUid, RejectPurchaseOrderRequest request) {
        PurchaseOrder order = loadOrThrow(orderUid);
        order.reject(emptyToNull(request == null ? null : request.reason()));
        return toDto(order);
    }

    @Transactional
    public PurchaseOrderDto cancel(String orderUid, CancelPurchaseOrderRequest request) {
        PurchaseOrder order = loadOrThrow(orderUid);
        order.cancel(emptyToNull(request == null ? null : request.reason()));
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto findByUid(String uid) {
        return toDto(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderSummary> search(String query, PurchaseOrderStatus status,
                                                     String supplierUid, String storeUid, Pageable pageable) {
        return PageResponse.from(
                orderRepository.search(
                        query == null ? null : query.trim(),
                        status,
                        emptyToNull(supplierUid),
                        emptyToNull(storeUid),
                        pageable).map(this::toSummary));
    }

    PurchaseOrder loadOrThrow(String uid) {
        return orderRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Purchase order not found: " + uid));
    }

    private Supplier activeSupplier(String uid) {
        Supplier supplier = supplierRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + uid));
        if (!supplier.isActive()) {
            throw new BusinessRuleException("Supplier is not active: " + supplier.getName());
        }
        return supplier;
    }

    private Store activeStore(String uid) {
        Store store = storeRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Store not found: " + uid));
        if (!store.isActive()) {
            throw new BusinessRuleException("Store is not active: " + store.getName());
        }
        return store;
    }

    private Medicine activeMedicine(String uid) {
        Medicine medicine = medicineRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + uid));
        if (!medicine.isActive()) {
            throw new BusinessRuleException("Medicine is not active: " + medicine.getName());
        }
        return medicine;
    }

    // ----- mapping ----------------------------------------------------------

    private PurchaseOrderDto toDto(PurchaseOrder po) {
        Supplier supplier = supplierRepository.findByUid(po.getSupplierUid()).orElse(null);
        Store store = storeRepository.findByUid(po.getStoreUid()).orElse(null);
        List<PurchaseOrderLine> lines = lineRepository.findAllByOrderUidOrderByCreatedAtAsc(po.getUid());
        BigDecimal subtotal = lines.stream()
                .map(PurchaseOrderLine::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = lines.isEmpty() ? "TZS" : lines.get(0).getCurrency();

        return new PurchaseOrderDto(
                po.getUid(),
                po.getOrderNo(),
                po.getSupplierUid(),
                supplier == null ? null : supplier.getName(),
                po.getStoreUid(),
                store == null ? null : store.getName(),
                po.getStatus(),
                po.getExpectedDeliveryDate(),
                po.getNotes(),
                po.getVerifiedAt(),
                po.getApprovedAt(),
                po.getOrderedAt(),
                po.getReceivedAt(),
                po.getRejectedAt(),
                po.getRejectReason(),
                po.getCancelledAt(),
                po.getCancelReason(),
                currency,
                subtotal,
                po.getCreatedAt(),
                po.getUpdatedAt(),
                lines.stream().map(this::toLineDto).toList());
    }

    private PurchaseOrderLineDto toLineDto(PurchaseOrderLine line) {
        Medicine medicine = medicineRepository.findByUid(line.getMedicineUid()).orElse(null);
        return new PurchaseOrderLineDto(
                line.getUid(),
                line.getMedicineUid(),
                medicine == null ? null : medicine.getCode(),
                medicine == null ? null : medicine.getName(),
                medicine == null ? null : medicine.getStrength(),
                line.getOrderedQuantity(),
                line.getReceivedQuantity(),
                line.outstandingQuantity(),
                line.getUnitCost(),
                line.getCurrency(),
                line.lineAmount());
    }

    private PurchaseOrderSummary toSummary(PurchaseOrder po) {
        Supplier supplier = supplierRepository.findByUid(po.getSupplierUid()).orElse(null);
        Store store = storeRepository.findByUid(po.getStoreUid()).orElse(null);
        List<PurchaseOrderLine> lines = lineRepository.findAllByOrderUidOrderByCreatedAtAsc(po.getUid());
        BigDecimal subtotal = lines.stream()
                .map(PurchaseOrderLine::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = lines.isEmpty() ? "TZS" : lines.get(0).getCurrency();
        return new PurchaseOrderSummary(
                po.getUid(),
                po.getOrderNo(),
                supplier == null ? null : supplier.getName(),
                store == null ? null : store.getName(),
                po.getStatus(),
                po.getExpectedDeliveryDate(),
                subtotal,
                currency,
                po.getCreatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
