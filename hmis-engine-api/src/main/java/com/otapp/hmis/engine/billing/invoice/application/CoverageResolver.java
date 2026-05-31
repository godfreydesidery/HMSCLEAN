package com.otapp.hmis.engine.billing.invoice.application;

import com.otapp.hmis.engine.masterdata.pricing.domain.ServiceKind;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServicePrice;
import com.otapp.hmis.engine.masterdata.pricing.domain.ServicePriceRepository;
import com.otapp.hmis.engine.patient.domain.Patient;
import com.otapp.hmis.engine.patient.domain.PatientRepository;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Decides, at charge time, whether a patient's insurance plan COVERS a given
 * service — a faithful port of the legacy
 * {@code findBy<Catalogue>AndInsurancePlanAndCovered(item, plan, true)} lookup in
 * Zana-HMIS {@code PatientServiceImpl}.
 *
 * <p>Coverage is keyed off the per-service {@code ServicePrice.covered} flag, NOT
 * off price-existence: a plan having a price row does not mean it covers the
 * service. This is deliberately separate from {@link PriceLookup}'s band
 * resolution (which prefers a plan price for negotiation purposes) so the two
 * concerns never bleed into each other.
 *
 * <p>Billing → masterdata / patient are allowed module dependencies; the lookup
 * is the single {@code findCoveredCell} query (no N+1, no extra round-trips
 * beyond the patient fetch the caller already needs for membership).
 */
@Component
@RequiredArgsConstructor
class CoverageResolver {

    private final ServicePriceRepository priceRepository;
    private final PatientRepository patientRepository;

    /**
     * Resolves coverage for a (service, patient) in a target currency, given the
     * cash price already resolved by {@link PriceLookup}.
     *
     * <ul>
     *   <li>Not insured (no plan) -&gt; {@link CoverageResolution#cash(BigDecimal)}.</li>
     *   <li>Insured but the plan does not flag this service covered ->
     *       {@link CoverageResolution#cash(BigDecimal)} (the caller decides
     *       UNPAID vs VERIFIED by scope, mirroring legacy outpatient-vs-admission).</li>
     *   <li>Insured and covered -> COVERED at the plan ceiling, with the
     *       membership number stamped and any co-pay remainder computed.</li>
     * </ul>
     */
    CoverageResolution resolve(ServiceKind kind, String serviceUid, String patientUid,
                               String currency, BigDecimal cashAmount) {
        Patient patient = patientUid == null ? null : patientRepository.findByUid(patientUid).orElse(null);
        String planUid = patient == null ? null : patient.getInsurancePlanUid();
        if (planUid == null || planUid.isBlank()) {
            return CoverageResolution.cash(cashAmount);
        }

        Optional<ServicePrice> covered = priceRepository.findCoveredCell(planUid, kind, serviceUid, currency);
        if (covered.isEmpty()) {
            // Insured but this service is not covered by the plan.
            return CoverageResolution.cash(cashAmount);
        }

        BigDecimal coveredAmount = covered.get().getAmount();
        // Co-pay: the cash remainder above the plan ceiling (ward top-up case);
        // never negative.
        BigDecimal copay = cashAmount.subtract(coveredAmount).max(BigDecimal.ZERO);
        return CoverageResolution.covered(coveredAmount, copay, planUid, patient.getMembershipNo());
    }

    /**
     * The outcome of a coverage decision.
     *
     * @param covered       whether the plan covers the service
     * @param coveredAmount the insurer-paid amount (plan ceiling) when covered; the cash amount otherwise
     * @param copayAmount   the cash remainder above the plan ceiling (supplementary split); zero when not covered
     * @param planUid       the plan that covered the line; {@code null} when not covered
     * @param membershipNo  the patient's membership number to stamp on a covered line; {@code null} otherwise
     */
    record CoverageResolution(boolean covered, BigDecimal coveredAmount, BigDecimal copayAmount,
                              String planUid, String membershipNo) {

        static CoverageResolution cash(BigDecimal cashAmount) {
            return new CoverageResolution(false, cashAmount, BigDecimal.ZERO, null, null);
        }

        static CoverageResolution covered(BigDecimal coveredAmount, BigDecimal copayAmount,
                                          String planUid, String membershipNo) {
            return new CoverageResolution(true, coveredAmount, copayAmount, planUid, membershipNo);
        }
    }
}
