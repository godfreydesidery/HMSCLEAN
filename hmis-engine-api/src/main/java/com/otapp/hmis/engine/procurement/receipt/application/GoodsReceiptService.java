package com.otapp.hmis.engine.procurement.receipt.application;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.Pharmacy;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.PharmacyRepository;
import com.otapp.hmis.engine.pharmacy.stock.application.StockService;
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
    private final PharmacyRepository pharmacyRepository;
    private final MedicineRepository medicineRepository;
    private final GoodsReceiptNumberGenerator numberGenerator;
    private final StockService stockService;

    /**
     * Records a goods receipt against a purchase order:
     * <ol>
     *   <li>Validates the PO is in a receivable state.</li>
     *   <li>For each line: bumps the PO line's received quantity and applies
     *       a RECEIPT movement to the target pharmacy's stock balance.</li>
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
        Pharmacy pharmacy = pharmacyRepository.findByUid(order.getPharmacyUid())
                .orElseThrow(() -> new NotFoundException("Pharmacy not found: " + order.getPharmacyUid()));

        GoodsReceipt receipt = receiptRepository.save(new GoodsReceipt(
                numberGenerator.next(),
                order.getUid(),
                pharmacy.getUid(),
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
            // Bumps the line's received quantity (guards outstanding amount).
            poLine.recordReceipt(lineReq.quantity());

            savedLines.add(receiptLineRepository.save(new GoodsReceiptLine(
                    receipt.getUid(),
                    poLine.getUid(),
                    poLine.getMedicineUid(),
                    lineReq.quantity())));

            // Apply the receipt to the pharmacy's stock balance.
            stockService.receiveForReference(
                    pharmacy.getUid(),
                    poLine.getMedicineUid(),
                    lineReq.quantity(),
                    receipt.getUid(),
                    "Receipt against " + order.getOrderNo());
        }

        // Transition the PO based on whether every line is now fully received.
        boolean allFull = orderLineRepository.findAllByOrderUidOrderByCreatedAtAsc(order.getUid()).stream()
                .allMatch(PurchaseOrderLine::isFullyReceived);
        order.onLineReceipt(allFull);

        return toDto(receipt, order, pharmacy, savedLines);
    }

    @Transactional(readOnly = true)
    public List<GoodsReceiptDto> listForOrder(String orderUid) {
        PurchaseOrder order = orderRepository.findByUid(orderUid)
                .orElseThrow(() -> new NotFoundException("Purchase order not found: " + orderUid));
        Pharmacy pharmacy = pharmacyRepository.findByUid(order.getPharmacyUid()).orElse(null);
        return receiptRepository.findAllByOrderUidOrderByReceivedAtDesc(order.getUid()).stream()
                .map(r -> toDto(r, order, pharmacy,
                        receiptLineRepository.findAllByReceiptUidOrderByCreatedAtAsc(r.getUid())))
                .toList();
    }

    private GoodsReceiptDto toDto(GoodsReceipt r, PurchaseOrder order, Pharmacy pharmacy,
                                  List<GoodsReceiptLine> lines) {
        return new GoodsReceiptDto(
                r.getUid(),
                r.getReceiptNo(),
                r.getOrderUid(),
                order == null ? null : order.getOrderNo(),
                r.getPharmacyUid(),
                pharmacy == null ? null : pharmacy.getName(),
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
                line.getQuantity());
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
