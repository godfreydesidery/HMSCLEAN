package com.otapp.hmis.engine.pharmacy.sale.application;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.common.error.BusinessRuleException;
import com.otapp.hmis.engine.common.error.NotFoundException;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlan;
import com.otapp.hmis.engine.masterdata.insurance.domain.InsurancePlanRepository;
import com.otapp.hmis.engine.masterdata.medicine.domain.Medicine;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineRepository;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.Pharmacy;
import com.otapp.hmis.engine.masterdata.pharmacy.domain.PharmacyRepository;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import com.otapp.hmis.engine.patient.domain.PaymentType;
import com.otapp.hmis.engine.pharmacy.sale.application.PharmacySaleOrderDtos.AddLineRequest;
import com.otapp.hmis.engine.pharmacy.sale.application.PharmacySaleOrderDtos.CancelSaleRequest;
import com.otapp.hmis.engine.pharmacy.sale.application.PharmacySaleOrderDtos.CreatePharmacySaleOrderRequest;
import com.otapp.hmis.engine.pharmacy.sale.application.PharmacySaleOrderDtos.PharmacySaleOrderDto;
import com.otapp.hmis.engine.pharmacy.sale.application.PharmacySaleOrderDtos.PharmacySaleOrderLineDto;
import com.otapp.hmis.engine.pharmacy.sale.application.PharmacySaleOrderDtos.PharmacySaleOrderSummary;
import com.otapp.hmis.engine.pharmacy.sale.application.PharmacySaleOrderDtos.RejectLineRequest;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleLineStatus;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleOrder;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleOrderLine;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleOrderLineRepository;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleOrderRepository;
import com.otapp.hmis.engine.pharmacy.sale.domain.PharmacySaleOrderStatus;
import com.otapp.hmis.engine.pharmacy.sale.infrastructure.PharmacySaleOrderNumberGenerator;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PharmacySaleOrderService {

    private static final EnumSet<PharmacySaleLineStatus> TERMINAL = EnumSet.of(
            PharmacySaleLineStatus.SOLD,
            PharmacySaleLineStatus.REJECTED,
            PharmacySaleLineStatus.CANCELLED);

    private final PharmacySaleOrderRepository saleRepository;
    private final PharmacySaleOrderLineRepository lineRepository;
    private final PharmacyRepository pharmacyRepository;
    private final MedicineRepository medicineRepository;
    private final PatientRepository patientRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final PharmacySaleOrderNumberGenerator numberGenerator;

    @Transactional
    public PharmacySaleOrderDto create(CreatePharmacySaleOrderRequest request) {
        Pharmacy pharmacy = activePharmacy(request.pharmacyUid());

        String patientUid = emptyToNull(request.patientUid());
        if (patientUid != null) {
            patientRepository.findByUid(patientUid)
                    .orElseThrow(() -> new NotFoundException("Patient not found: " + patientUid));
        }
        String planUid = emptyToNull(request.insurancePlanUid());
        if (planUid != null) {
            insurancePlanRepository.findByUid(planUid)
                    .orElseThrow(() -> new NotFoundException("Insurance plan not found: " + planUid));
        }
        if ((request.paymentType() == PaymentType.INSURANCE || request.paymentType() == PaymentType.MIXED)
                && planUid == null) {
            throw new BusinessRuleException("Insurance plan is required for payment type " + request.paymentType());
        }

        PharmacySaleOrder sale = saleRepository.save(new PharmacySaleOrder(
                numberGenerator.next(),
                pharmacy.getUid(),
                request.customerName().trim(),
                emptyToNull(request.customerPhone()),
                patientUid,
                request.paymentType(),
                planUid,
                "TZS"));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (AddLineRequest line : request.lines()) {
            Medicine medicine = activeMedicine(line.medicineUid());
            PharmacySaleOrderLine entity = lineRepository.save(new PharmacySaleOrderLine(
                    sale.getUid(),
                    medicine.getUid(),
                    line.quantity(),
                    emptyToNull(line.dose()),
                    emptyToNull(line.frequency()),
                    line.durationDays(),
                    emptyToNull(line.instructions()),
                    line.unitPrice()));
            subtotal = subtotal.add(entity.getLineAmount());
        }
        sale.setSubtotal(subtotal);
        return toDto(sale);
    }

    @Transactional
    public PharmacySaleOrderDto accept(String saleUid, String lineUid) {
        return transition(saleUid, lineUid, PharmacySaleOrderLine::accept);
    }

    @Transactional
    public PharmacySaleOrderDto hold(String saleUid, String lineUid) {
        return transition(saleUid, lineUid, PharmacySaleOrderLine::hold);
    }

    @Transactional
    public PharmacySaleOrderDto verify(String saleUid, String lineUid) {
        return transition(saleUid, lineUid, PharmacySaleOrderLine::verify);
    }

    @Transactional
    public PharmacySaleOrderDto approve(String saleUid, String lineUid) {
        return transition(saleUid, lineUid, PharmacySaleOrderLine::approve);
    }

    @Transactional
    public PharmacySaleOrderDto rejectLine(String saleUid, String lineUid, RejectLineRequest request) {
        String reason = emptyToNull(request == null ? null : request.reason());
        return transition(saleUid, lineUid, line -> line.reject(reason));
    }

    @Transactional
    public PharmacySaleOrderDto cancelLine(String saleUid, String lineUid, RejectLineRequest request) {
        String reason = emptyToNull(request == null ? null : request.reason());
        return transition(saleUid, lineUid, line -> line.cancel(reason));
    }

    @Transactional
    public PharmacySaleOrderDto cancelSale(String saleUid, CancelSaleRequest request) {
        PharmacySaleOrder sale = loadOrThrow(saleUid);
        // Cancel any still-open line so the rollup completes properly.
        for (PharmacySaleOrderLine line : lineRepository.findAllBySaleUidOrderByCreatedAtAsc(saleUid)) {
            if (!line.isTerminal()) {
                line.cancel("Sale cancelled");
            }
        }
        sale.cancel(emptyToNull(request == null ? null : request.reason()));
        return toDto(sale);
    }

    @Transactional(readOnly = true)
    public PharmacySaleOrderDto findByUid(String uid) {
        return toDto(loadOrThrow(uid));
    }

    @Transactional(readOnly = true)
    public PageResponse<PharmacySaleOrderSummary> search(String query, PharmacySaleOrderStatus status,
                                                         String pharmacyUid, String patientUid, Pageable pageable) {
        return PageResponse.from(
                saleRepository.search(
                                query == null ? null : query.trim(),
                                status,
                                emptyToNull(pharmacyUid),
                                emptyToNull(patientUid),
                                pageable)
                        .map(this::toSummary));
    }

    /** Pull the sale order from a SOLD line — used by the stock service after a dispense. */
    @Transactional
    public PharmacySaleOrderDto refreshHeaderAfterLineChange(String saleUid) {
        PharmacySaleOrder sale = loadOrThrow(saleUid);
        long open = lineRepository.countBySaleUidAndStatusNotIn(sale.getUid(), TERMINAL);
        sale.onLineTransition((int) open);
        return toDto(sale);
    }

    // ----- helpers -----------------------------------------------------------

    private PharmacySaleOrderDto transition(String saleUid, String lineUid,
                                            java.util.function.Consumer<PharmacySaleOrderLine> change) {
        PharmacySaleOrder sale = loadOrThrow(saleUid);
        PharmacySaleOrderLine line = lineRepository.findByUid(lineUid)
                .orElseThrow(() -> new NotFoundException("Sale line not found: " + lineUid));
        if (!line.getSaleUid().equals(sale.getUid())) {
            throw new BusinessRuleException("Line does not belong to this sale");
        }
        change.accept(line);
        long open = lineRepository.countBySaleUidAndStatusNotIn(sale.getUid(), TERMINAL);
        sale.onLineTransition((int) open);
        return toDto(sale);
    }

    PharmacySaleOrder loadOrThrow(String uid) {
        return saleRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Pharmacy sale order not found: " + uid));
    }

    private Pharmacy activePharmacy(String uid) {
        Pharmacy p = pharmacyRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Pharmacy not found: " + uid));
        if (!p.isActive()) {
            throw new BusinessRuleException("Pharmacy is not active: " + p.getName());
        }
        return p;
    }

    private Medicine activeMedicine(String uid) {
        Medicine m = medicineRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + uid));
        if (!m.isActive()) {
            throw new BusinessRuleException("Medicine is not active: " + m.getName());
        }
        return m;
    }

    // ----- mapping -----------------------------------------------------------

    private PharmacySaleOrderDto toDto(PharmacySaleOrder s) {
        Pharmacy pharmacy = pharmacyRepository.findByUid(s.getPharmacyUid()).orElse(null);
        Patient patient = s.getPatientUid() == null
                ? null
                : patientRepository.findByUid(s.getPatientUid()).orElse(null);
        InsurancePlan plan = s.getInsurancePlanUid() == null
                ? null
                : insurancePlanRepository.findByUid(s.getInsurancePlanUid()).orElse(null);
        List<PharmacySaleOrderLine> lines = lineRepository.findAllBySaleUidOrderByCreatedAtAsc(s.getUid());

        return new PharmacySaleOrderDto(
                s.getUid(),
                s.getSaleNo(),
                s.getPharmacyUid(),
                pharmacy == null ? null : pharmacy.getName(),
                s.getPatientUid(),
                patient == null ? null : patient.getPatientNo(),
                s.getCustomerName(),
                s.getCustomerPhone(),
                s.getStatus(),
                s.getPaymentType(),
                s.getInsurancePlanUid(),
                plan == null ? null : plan.getName(),
                s.getCurrency(),
                s.getSubtotal(),
                s.getTotalPaid(),
                s.balance(),
                s.getOpenedAt(),
                s.getCompletedAt(),
                s.getCancelledAt(),
                s.getCancelReason(),
                s.getCreatedAt(),
                s.getUpdatedAt(),
                lines.stream().map(this::toLineDto).toList());
    }

    private PharmacySaleOrderLineDto toLineDto(PharmacySaleOrderLine line) {
        Medicine m = medicineRepository.findByUid(line.getMedicineUid()).orElse(null);
        return new PharmacySaleOrderLineDto(
                line.getUid(),
                line.getMedicineUid(),
                m == null ? null : m.getCode(),
                m == null ? null : m.getName(),
                m == null ? null : m.getStrength(),
                line.getQuantity(),
                line.getDose(),
                line.getFrequency(),
                line.getDurationDays(),
                line.getInstructions(),
                line.getUnitPrice(),
                line.getLineAmount(),
                line.getStatus(),
                line.getAcceptedAt(),
                line.getHeldAt(),
                line.getVerifiedAt(),
                line.getApprovedAt(),
                line.getSoldAt(),
                line.getRejectedAt(),
                line.getRejectReason(),
                line.getCancelReason(),
                line.getCreatedAt());
    }

    private PharmacySaleOrderSummary toSummary(PharmacySaleOrder s) {
        Pharmacy pharmacy = pharmacyRepository.findByUid(s.getPharmacyUid()).orElse(null);
        return new PharmacySaleOrderSummary(
                s.getUid(),
                s.getSaleNo(),
                pharmacy == null ? null : pharmacy.getName(),
                s.getCustomerName(),
                s.getPatientUid(),
                s.getStatus(),
                s.getPaymentType(),
                s.getSubtotal(),
                s.getTotalPaid(),
                s.balance(),
                s.getCurrency(),
                s.getOpenedAt(),
                s.getCompletedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
