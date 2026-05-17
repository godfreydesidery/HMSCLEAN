package com.otapp.hmis.engine.procurement.order.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.Pharmacy;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.PharmacyRepository;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.AddLineRequest;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.CancelPurchaseOrderRequest;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.CreatePurchaseOrderRequest;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.PurchaseOrderDto;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.PurchaseOrderLineDto;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.PurchaseOrderSummary;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.UpdateLineRequest;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrder;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderLine;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderLineRepository;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderRepository;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderStatus;
import com.otapp.hmis.engine.procurement.order.infrastructure.PurchaseOrderNumberGenerator;
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
    private final PharmacyRepository pharmacyRepository;
    private final MedicineRepository medicineRepository;
    private final PurchaseOrderNumberGenerator numberGenerator;

    @Transactional
    public PurchaseOrderDto create(CreatePurchaseOrderRequest request) {
        Supplier supplier = activeSupplier(request.supplierUid());
        Pharmacy pharmacy = activePharmacy(request.pharmacyUid());

        PurchaseOrder order = new PurchaseOrder(
                numberGenerator.next(),
                supplier.getUid(),
                pharmacy.getUid(),
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
        PurchaseOrderLine line = new PurchaseOrderLine(
                order.getUid(),
                medicine.getUid(),
                request.orderedQuantity(),
                request.unitCost(),
                emptyToNull(request.currency()));
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
        line.setOrderedQuantity(request.orderedQuantity());
        line.setUnitCost(request.unitCost());
        if (request.currency() != null && !request.currency().isBlank()) {
            line.setCurrency(request.currency());
        }
        return toDto(order);
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
    public PurchaseOrderDto markOrdered(String orderUid) {
        PurchaseOrder order = loadOrThrow(orderUid);
        if (lineRepository.findAllByOrderUidOrderByCreatedAtAsc(orderUid).isEmpty()) {
            throw new BusinessRuleException("Cannot order a purchase order with no lines");
        }
        order.markOrdered();
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
                                                     String supplierUid, String pharmacyUid, Pageable pageable) {
        return PageResponse.from(
                orderRepository.search(
                        query == null ? null : query.trim(),
                        status,
                        emptyToNull(supplierUid),
                        emptyToNull(pharmacyUid),
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

    private Pharmacy activePharmacy(String uid) {
        Pharmacy pharmacy = pharmacyRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Pharmacy not found: " + uid));
        if (!pharmacy.isActive()) {
            throw new BusinessRuleException("Pharmacy is not active: " + pharmacy.getName());
        }
        return pharmacy;
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
        Pharmacy pharmacy = pharmacyRepository.findByUid(po.getPharmacyUid()).orElse(null);
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
                po.getPharmacyUid(),
                pharmacy == null ? null : pharmacy.getName(),
                po.getStatus(),
                po.getExpectedDeliveryDate(),
                po.getNotes(),
                po.getOrderedAt(),
                po.getReceivedAt(),
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
        Pharmacy pharmacy = pharmacyRepository.findByUid(po.getPharmacyUid()).orElse(null);
        List<PurchaseOrderLine> lines = lineRepository.findAllByOrderUidOrderByCreatedAtAsc(po.getUid());
        BigDecimal subtotal = lines.stream()
                .map(PurchaseOrderLine::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = lines.isEmpty() ? "TZS" : lines.get(0).getCurrency();
        return new PurchaseOrderSummary(
                po.getUid(),
                po.getOrderNo(),
                supplier == null ? null : supplier.getName(),
                pharmacy == null ? null : pharmacy.getName(),
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
