package com.otapp.hmis.engine.procurement.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.ConflictException;
import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineForm;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.store.domain.StoreRepository;
import com.otapp.hmis.engine.procurement.order.application.PurchaseOrderDtos.AddLineRequest;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrder;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderLine;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderLineRepository;
import com.otapp.hmis.engine.procurement.order.domain.PurchaseOrderRepository;
import com.otapp.hmis.engine.procurement.order.infrastructure.PurchaseOrderNumberGenerator;
import com.otapp.hmis.engine.procurement.pricelist.application.SupplierItemPriceDtos.SupplierItemPriceDto;
import com.otapp.hmis.engine.procurement.pricelist.application.SupplierItemPriceService;
import com.otapp.hmis.engine.procurement.supplier.domain.SupplierRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Legacy-faithful coverage of the purchase-order line supplier-quoted gate and
 * the server-authoritative contracted-price pull (LocalPurchaseOrderServiceImpl
 * .saveDetail: findBySupplierAndItem -> throw if absent; price = quote price).
 */
@ExtendWith(MockitoExtension.class)
class PurchaseOrderLineGateTest {

    private static final String SUPPLIER_UID = "01HSUPPLIER0000000000000AA";
    private static final String STORE_UID    = "01HSTORE000000000000000AAA";
    private static final String MEDICINE_UID = "01HMEDICINE00000000000AAAA";

    @Mock private PurchaseOrderRepository orderRepository;
    @Mock private PurchaseOrderLineRepository lineRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private MedicineRepository medicineRepository;
    @Mock private SupplierItemPriceService supplierItemPriceService;
    @Mock private PurchaseOrderNumberGenerator numberGenerator;

    private PurchaseOrderService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseOrderService(orderRepository, lineRepository, supplierRepository,
                storeRepository, medicineRepository, supplierItemPriceService, numberGenerator);
    }

    private PurchaseOrder draftOrder() {
        // DRAFT is mutable; constructor leaves status = DRAFT.
        return new PurchaseOrder("PO-1", SUPPLIER_UID, STORE_UID, null, null);
    }

    private Medicine activeMedicine() {
        Medicine m = new Medicine("MED1", "Amoxicillin", null, "500mg", MedicineForm.TABLET, null);
        // Fresh entities have no uid until @PrePersist; set it for the gate lookup.
        org.springframework.test.util.ReflectionTestUtils.setField(
                m, AuditableEntity.class, "uid", MEDICINE_UID, String.class);
        return m;
    }

    private SupplierItemPriceDto quote(BigDecimal price, String currency) {
        return new SupplierItemPriceDto(
                "01HPRICE00000000000000AAAA", SUPPLIER_UID, "Supplier", MEDICINE_UID,
                "MED1", "Amoxicillin", "500mg", price, currency,
                LocalDate.now().minusDays(1), null, true, true, null, null, null);
    }

    @Test
    void addLineThrowsWhenSupplierDoesNotQuoteItem() {
        when(orderRepository.findByUid("PO-1")).thenReturn(Optional.of(draftOrder()));
        when(medicineRepository.findByUid(MEDICINE_UID)).thenReturn(Optional.of(activeMedicine()));
        when(supplierItemPriceService.findContractedPrice(SUPPLIER_UID, MEDICINE_UID))
                .thenReturn(Optional.empty());

        AddLineRequest req = new AddLineRequest(MEDICINE_UID, 10, new BigDecimal("999.99"), "USD");

        assertThatThrownBy(() -> service.addLine("PO-1", req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Item not valid for this supplier");

        verify(lineRepository, never()).save(any());
    }

    @Test
    void addLinePullsContractedPriceAndIgnoresClientUnitCost() {
        when(orderRepository.findByUid("PO-1")).thenReturn(Optional.of(draftOrder()));
        when(medicineRepository.findByUid(MEDICINE_UID)).thenReturn(Optional.of(activeMedicine()));
        when(lineRepository.findAllByOrderUidOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(supplierItemPriceService.findContractedPrice(SUPPLIER_UID, MEDICINE_UID))
                .thenReturn(Optional.of(quote(new BigDecimal("1200.00"), "TZS")));

        // Client sends a bogus unit cost + currency; both must be ignored.
        AddLineRequest req = new AddLineRequest(MEDICINE_UID, 10, new BigDecimal("1.00"), "USD");
        service.addLine("PO-1", req);

        ArgumentCaptor<PurchaseOrderLine> captor = ArgumentCaptor.forClass(PurchaseOrderLine.class);
        verify(lineRepository).save(captor.capture());
        PurchaseOrderLine saved = captor.getValue();
        assertThat(saved.getUnitCost()).isEqualByComparingTo("1200.00");
        assertThat(saved.getCurrency()).isEqualTo("TZS");
        assertThat(saved.getOrderedQuantity()).isEqualTo(10);
        assertThat(saved.getMedicineUid()).isEqualTo(MEDICINE_UID);
    }

    @Test
    void addLineRejectsDuplicateMedicineWithConflict() {
        PurchaseOrder order = draftOrder();
        when(orderRepository.findByUid("PO-1")).thenReturn(Optional.of(order));
        when(medicineRepository.findByUid(MEDICINE_UID)).thenReturn(Optional.of(activeMedicine()));
        when(supplierItemPriceService.findContractedPrice(SUPPLIER_UID, MEDICINE_UID))
                .thenReturn(Optional.of(quote(new BigDecimal("1200.00"), "TZS")));
        PurchaseOrderLine existing = new PurchaseOrderLine(order.getUid(), MEDICINE_UID, 5,
                new BigDecimal("1200.00"), "TZS");
        when(lineRepository.findAllByOrderUidOrderByCreatedAtAsc(any())).thenReturn(List.of(existing));

        AddLineRequest req = new AddLineRequest(MEDICINE_UID, 10, null, null);

        assertThatThrownBy(() -> service.addLine("PO-1", req))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Duplicates items are not allowed");

        verify(lineRepository, never()).save(any());
    }

    @Test
    void addLineSucceedsWithNullClientUnitCost() {
        when(orderRepository.findByUid("PO-1")).thenReturn(Optional.of(draftOrder()));
        when(medicineRepository.findByUid(MEDICINE_UID)).thenReturn(Optional.of(activeMedicine()));
        when(lineRepository.findAllByOrderUidOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(supplierItemPriceService.findContractedPrice(SUPPLIER_UID, MEDICINE_UID))
                .thenReturn(Optional.of(quote(new BigDecimal("750.50"), "TZS")));

        // unitCost + currency are now optional/advisory.
        AddLineRequest req = new AddLineRequest(MEDICINE_UID, 3, null, null);
        service.addLine("PO-1", req);

        ArgumentCaptor<PurchaseOrderLine> captor = ArgumentCaptor.forClass(PurchaseOrderLine.class);
        verify(lineRepository).save(captor.capture());
        assertThat(captor.getValue().getUnitCost()).isEqualByComparingTo("750.50");
    }
}
