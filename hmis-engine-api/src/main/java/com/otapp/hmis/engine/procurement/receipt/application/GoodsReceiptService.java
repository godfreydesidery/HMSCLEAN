package com.otapp.hmis.engine.procurement.receipt.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
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
    private final GoodsReceiptNumberGenerator numberGenerator;
    private final StoreStockService storeStockService;

    /**
     * Records a goods receipt against a purchase order:
     * <ol>
     *   <li>Validates the PO is in a receivable state.</li>
     *   <li>For each line: bumps the PO line's received quantity and applies
     *       a RECEIPT movement to the target store's stock balance for the
     *       supplier-provided batch.</li>
     *   <li>Transitions the PO to PARTIALLY_RECEIVED or RECEIVED.</li>
     * </ol>
     * All steps run inside one transaction; failure rolls back stock as well.
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
            poLine.recordReceipt(lineReq.quantity());

            savedLines.add(receiptLineRepository.save(new GoodsReceiptLine(
                    receipt.getUid(),
                    poLine.getUid(),
                    poLine.getMedicineUid(),
                    lineReq.quantity(),
                    lineReq.batchNo(),
                    lineReq.expiresAt())));

            storeStockService.receiveFromProcurement(
                    store.getUid(),
                    poLine.getMedicineUid(),
                    lineReq.batchNo(),
                    lineReq.expiresAt(),
                    lineReq.quantity(),
                    receipt.getUid(),
                    "Receipt against " + order.getOrderNo());
        }

        boolean allFull = orderLineRepository.findAllByOrderUidOrderByCreatedAtAsc(order.getUid()).stream()
                .allMatch(PurchaseOrderLine::isFullyReceived);
        order.onLineReceipt(allFull);

        return toDto(receipt, order, store, savedLines);
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
                r.getReceivedAt(),
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
