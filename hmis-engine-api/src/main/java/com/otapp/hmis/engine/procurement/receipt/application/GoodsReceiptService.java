package com.otapp.hmis.engine.procurement.receipt.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.medicine.application.UnitConversionService;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineUnit;
import com.otapp.hmis.engine.masterdata.store.domain.Store;
import com.otapp.hmis.engine.masterdata.store.domain.StoreRepository;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrder;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderLine;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderLineRepository;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderRepository;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderStatus;
import com.otapp.hmis.engine.procurement.receipt.application.GoodsReceiptDtos.GoodsReceiptDto;
import com.otapp.hmis.engine.procurement.receipt.application.GoodsReceiptDtos.GoodsReceiptLineDto;
import com.otapp.hmis.engine.procurement.receipt.application.GoodsReceiptDtos.RecordReceiptRequest;
import com.otapp.hmis.engine.procurement.receipt.application.GoodsReceiptDtos.RejectReceiptRequest;
import com.otapp.hmis.engine.procurement.receipt.domain.GoodsReceipt;
import com.otapp.hmis.engine.procurement.receipt.domain.GoodsReceiptLine;
import com.otapp.hmis.engine.procurement.receipt.domain.GoodsReceiptLineRepository;
import com.otapp.hmis.engine.procurement.receipt.domain.GoodsReceiptRepository;
import com.otapp.hmis.engine.procurement.receipt.infrastructure.GoodsReceiptNumberGenerator;
import com.otapp.hmis.engine.store.stock.application.StoreStockService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoodsReceiptService {

    private final GoodsReceiptRepository receiptRepository;
    private final GoodsReceiptLineRepository receiptLineRepository;
    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderLineRepository orderLineRepository;
    private final StoreRepository storeRepository;
    private final MedicineRepository medicineRepository;
    private final UnitConversionService unitConversion;
    private final GoodsReceiptNumberGenerator numberGenerator;
    private final StoreStockService storeStockService;

    /**
     * Creates a GRN against a purchase order in PENDING state. The PO
     * must be ORDERED or PARTIALLY_RECEIVED. <strong>No stock change
     * yet</strong> — the receipt only credits the store on
     * {@link #approve(String)}. Line quantities are captured here so the
     * approver sees what was claimed.
     */
    @Transactional
    public GoodsReceiptDto record(String orderUid, RecordReceiptRequest request) {
        PurchaseOrder order = orderRepository.findByUid(orderUid)
                .orElseThrow(() -> new NotFoundException("Purchase order not found: " + orderUid));
        if (order.getStatus() != PurchaseOrderStatus.ORDERED
                && order.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new BusinessRuleException("Cannot receive against a " + order.getStatus() + " order");
        }
        Store store = storeRepository.findByUid(order.getStoreUid())
                .orElseThrow(() -> new NotFoundException("Store not found: " + order.getStoreUid()));

        GoodsReceipt receipt = receiptRepository.save(new GoodsReceipt(
                numberGenerator.next(),
                order.getUid(),
                store.getUid(),
                currentUsername(),
                emptyToNull(request.deliveryNote()),
                emptyToNull(request.notes())));

        List<GoodsReceiptLine> savedLines = new ArrayList<>();
        for (var lineReq : request.lines()) {
            PurchaseOrderLine poLine = orderLineRepository.findByUid(lineReq.poLineUid())
                    .orElseThrow(() -> new NotFoundException("PO line not found: " + lineReq.poLineUid()));
            if (!poLine.getOrderUid().equals(order.getUid())) {
                throw new BusinessRuleException("PO line " + lineReq.poLineUid() + " does not belong to this order");
            }
            // Convert to base units up front so PO outstanding accounting + GRN
            // storage are always in the same unit. Null unitUid → already base.
            MedicineUnit unit = unitConversion.resolveUnit(poLine.getMedicineUid(), lineReq.unitUid());
            int claimed = unitConversion.toBaseQuantity(unit, lineReq.quantity());
            int outstanding = poLine.outstandingQuantity();
            if (claimed > outstanding) {
                throw new BusinessRuleException(
                        "Receipt quantity " + claimed + " exceeds outstanding " + outstanding
                                + " for PO line " + poLine.getUid());
            }
            savedLines.add(receiptLineRepository.save(new GoodsReceiptLine(
                    receipt.getUid(),
                    poLine.getUid(),
                    poLine.getMedicineUid(),
                    claimed,
                    lineReq.batchNo(),
                    lineReq.manufacturedDate(),
                    lineReq.expiresAt())));
        }
        return toDto(receipt, order, store, savedLines);
    }

    @Transactional
    public GoodsReceiptDto verify(String receiptUid) {
        GoodsReceipt receipt = loadOrThrow(receiptUid);
        receipt.verify(currentUsername());
        PurchaseOrder order = orderRepository.findByUid(receipt.getOrderUid()).orElse(null);
        Store store = storeRepository.findByUid(receipt.getStoreUid()).orElse(null);
        return toDto(receipt, order, store,
                receiptLineRepository.findAllByReceiptUidOrderByCreatedAtAsc(receipt.getUid()));
    }

    /**
     * Approve the GRN: validates the PO is still receivable, runs the
     * per-line stock credits (FEFO not needed — supplier-supplied batch
     * goes into the store as a new {@code StoreStockBatch}), advances
     * the PO line {@code receivedQuantity}, and rolls the PO header to
     * PARTIALLY_RECEIVED / RECEIVED. All inside one transaction.
     */
    @Transactional
    public GoodsReceiptDto approve(String receiptUid) {
        GoodsReceipt receipt = loadOrThrow(receiptUid);
        PurchaseOrder order = orderRepository.findByUid(receipt.getOrderUid())
                .orElseThrow(() -> new NotFoundException("Purchase order not found: " + receipt.getOrderUid()));
        if (order.getStatus() != PurchaseOrderStatus.ORDERED
                && order.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new BusinessRuleException(
                    "Cannot approve a GRN against a " + order.getStatus() + " purchase order");
        }
        Store store = storeRepository.findByUid(receipt.getStoreUid())
                .orElseThrow(() -> new NotFoundException("Store not found: " + receipt.getStoreUid()));

        receipt.approve(currentUsername());

        List<GoodsReceiptLine> lines = receiptLineRepository
                .findAllByReceiptUidOrderByCreatedAtAsc(receipt.getUid());
        for (GoodsReceiptLine line : lines) {
            PurchaseOrderLine poLine = orderLineRepository.findByUid(line.getPoLineUid())
                    .orElseThrow(() -> new NotFoundException("PO line not found: " + line.getPoLineUid()));
            int outstanding = poLine.outstandingQuantity();
            if (line.getQuantity() > outstanding) {
                throw new BusinessRuleException(
                        "Receipt line " + line.getUid() + " exceeds PO outstanding "
                                + outstanding + " at approval — re-verify");
            }
            poLine.recordReceipt(line.getQuantity());
            storeStockService.receiveFromProcurement(
                    store.getUid(),
                    poLine.getMedicineUid(),
                    line.getBatchNo(),
                    line.getManufacturedDate(),
                    line.getExpiresAt(),
                    line.getQuantity(),
                    receipt.getUid(),
                    "Receipt against " + order.getOrderNo());
        }

        boolean allFull = orderLineRepository.findAllByOrderUidOrderByCreatedAtAsc(order.getUid()).stream()
                .allMatch(PurchaseOrderLine::isFullyReceived);
        order.onLineReceipt(allFull);

        return toDto(receipt, order, store, lines);
    }

    @Transactional
    public GoodsReceiptDto reject(String receiptUid, RejectReceiptRequest request) {
        GoodsReceipt receipt = loadOrThrow(receiptUid);
        receipt.reject(currentUsername(), emptyToNull(request == null ? null : request.reason()));
        PurchaseOrder order = orderRepository.findByUid(receipt.getOrderUid()).orElse(null);
        Store store = storeRepository.findByUid(receipt.getStoreUid()).orElse(null);
        return toDto(receipt, order, store,
                receiptLineRepository.findAllByReceiptUidOrderByCreatedAtAsc(receipt.getUid()));
    }

    @Transactional(readOnly = true)
    public GoodsReceiptDto findByUid(String receiptUid) {
        GoodsReceipt receipt = loadOrThrow(receiptUid);
        PurchaseOrder order = orderRepository.findByUid(receipt.getOrderUid()).orElse(null);
        Store store = storeRepository.findByUid(receipt.getStoreUid()).orElse(null);
        return toDto(receipt, order, store,
                receiptLineRepository.findAllByReceiptUidOrderByCreatedAtAsc(receipt.getUid()));
    }

    @Transactional(readOnly = true)
    public List<GoodsReceiptDto> listForOrder(String orderUid) {
        PurchaseOrder order = orderRepository.findByUid(orderUid)
                .orElseThrow(() -> new NotFoundException("Purchase order not found: " + orderUid));
        Store store = storeRepository.findByUid(order.getStoreUid()).orElse(null);
        return receiptRepository.findAllByOrderUidOrderByReceivedAtDesc(order.getUid()).stream()
                .map(r -> toDto(r, order, store,
                        receiptLineRepository.findAllByReceiptUidOrderByCreatedAtAsc(r.getUid())))
                .toList();
    }

    private GoodsReceipt loadOrThrow(String uid) {
        return receiptRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Goods receipt not found: " + uid));
    }

    private GoodsReceiptDto toDto(GoodsReceipt r, PurchaseOrder order, Store store,
                                  List<GoodsReceiptLine> lines) {
        return new GoodsReceiptDto(
                r.getUid(),
                r.getReceiptNo(),
                r.getOrderUid(),
                order == null ? null : order.getOrderNo(),
                r.getStoreUid(),
                store == null ? null : store.getName(),
                r.getReceivedByUsername(),
                r.getDeliveryNote(),
                r.getNotes(),
                r.getStatus(),
                r.getReceivedAt(),
                r.getVerifiedAt(), r.getVerifiedByUsername(),
                r.getApprovedAt(), r.getApprovedByUsername(),
                r.getRejectedAt(), r.getRejectedByUsername(), r.getRejectReason(),
                r.getCreatedAt(),
                lines.stream().map(this::toLineDto).toList());
    }

    private GoodsReceiptLineDto toLineDto(GoodsReceiptLine line) {
        Medicine medicine = medicineRepository.findByUid(line.getMedicineUid()).orElse(null);
        return new GoodsReceiptLineDto(
                line.getUid(),
                line.getPoLineUid(),
                line.getMedicineUid(),
                medicine == null ? null : medicine.getCode(),
                medicine == null ? null : medicine.getName(),
                line.getQuantity(),
                line.getBatchNo(),
                line.getManufacturedDate(),
                line.getExpiresAt());
    }

    private static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
