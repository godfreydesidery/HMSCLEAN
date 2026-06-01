package com.otapp.hmis.engine.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;

/**
 * Billing reports (gap audit cluster 5):
 *   * BILL-2 — per-cashier collections / cash-up.
 *   * BILL-5 — revenue by payment mode + pharmacy sales.
 *
 * <p>The container is JVM-lifetime and shared, so other tests add their own
 * payments / medicine lines; assertions are tolerant (the report MUST include at
 * least this test's contribution) over a ±1-day range (dodges any UTC/local
 * date skew on {@code receivedAt}), never absolute totals.
 */
class ReportingBillingIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";
    private static final String AMOXIL_UID     = "01J5KQRPCD0000000000000MD2";

    private static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<>() {};

    private final String from = LocalDate.now().minusDays(1).toString();
    private final String to   = LocalDate.now().plusDays(1).toString();

    @Test
    @SuppressWarnings("unchecked")
    void collectionsAndRevenueByModeReflectAPayment() {
        // A patient + their seeded 5,000 registration invoice, paid by MOBILE_MONEY.
        String patientUid = registerPatient("Report", "Payer", "OUTPATIENT");
        Map<String, Object> regInvoice = expectOk(get(
                "/billing/patients/uid/" + patientUid + "/registration-fee", MAP));
        String invoiceUid = (String) regInvoice.get("uid");
        String currency = String.valueOf(regInvoice.get("currency"));
        expectOk(post("/billing/invoices/uid/" + invoiceUid + "/payments",
                Map.of("method", "MOBILE_MONEY", "amount", new BigDecimal("5000.00"), "currency", currency),
                Map.class));

        // --- revenue-by-mode: the MOBILE_MONEY slice includes our 5,000 -----
        Map<String, Object> byMode = expectOk(get(
                "/reporting/revenue-by-mode?from=" + from + "&to=" + to, MAP));
        List<Map<String, Object>> modeRows = (List<Map<String, Object>>) byMode.get("byMethod");
        Map<String, Object> mobile = modeRows.stream()
                .filter(r -> "MOBILE_MONEY".equals(r.get("method"))).findFirst().orElse(null);
        assertThat(mobile).as("MOBILE_MONEY slice present").isNotNull();
        assertThat(amount(mobile.get("amount"))).isGreaterThanOrEqualTo(new BigDecimal("5000.00"));
        assertThat(((Number) mobile.get("count")).longValue()).isGreaterThanOrEqualTo(1);
        // The breakdown sums to the reported total.
        BigDecimal sumOfModes = modeRows.stream()
                .map(r -> amount(r.get("amount"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumOfModes).isEqualByComparingTo(amount(byMode.get("totalCollected")));

        // --- collections: root's till includes our 5,000 via MOBILE_MONEY ---
        Map<String, Object> collections = expectOk(get(
                "/reporting/collections?from=" + from + "&to=" + to, MAP));
        List<Map<String, Object>> cashiers = (List<Map<String, Object>>) collections.get("cashiers");
        Map<String, Object> root = cashiers.stream()
                .filter(c -> "root".equals(c.get("cashierUsername"))).findFirst().orElse(null);
        assertThat(root).as("root cashier present in the cash-up").isNotNull();
        assertThat(amount(root.get("totalCollected"))).isGreaterThanOrEqualTo(new BigDecimal("5000.00"));
        List<Map<String, Object>> rootMethods = (List<Map<String, Object>>) root.get("byMethod");
        assertThat(rootMethods.stream().anyMatch(m -> "MOBILE_MONEY".equals(m.get("method"))))
                .as("root's breakdown carries the MOBILE_MONEY line").isTrue();
        // Grand totals are consistent with the per-cashier rows.
        BigDecimal sumCashiers = cashiers.stream()
                .map(c -> amount(c.get("totalCollected"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumCashiers).isEqualByComparingTo(amount(collections.get("totalCollected")));
        assertThat(amount(collections.get("totalCollected"))).isGreaterThanOrEqualTo(new BigDecimal("5000.00"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void pharmacySalesListsABilledMedicine() {
        String patientUid = registerPatient("Report", "Pharma", "OUTPATIENT");
        String consultationUid = (String) expectOk(post(
                "/encounters/consultations",
                Map.of("patientUid", patientUid, "clinicUid", OPD_CLINIC_UID,
                        "clinicianUsername", clinicianAffiliatedWith(OPD_CLINIC_UID),
                        "paymentType", "CASH", "reason", "cough"),
                Map.class)).get("uid");
        openConsultation(consultationUid);
        // Prescribing bills a MEDICINE line onto the consultation invoice (M13).
        expectOk(post("/encounters/consultations/uid/" + consultationUid + "/prescriptions",
                Map.of("medicineUid", AMOXIL_UID, "dose", "1 tab", "frequency", "BD", "quantity", 7),
                Map.class));

        Map<String, Object> sales = expectOk(get(
                "/reporting/pharmacy-sales?from=" + from + "&to=" + to, MAP));
        List<Map<String, Object>> items = (List<Map<String, Object>>) sales.get("items");
        Map<String, Object> amoxil = items.stream()
                .filter(i -> AMOXIL_UID.equals(i.get("medicineUid"))).findFirst().orElse(null);
        assertThat(amoxil).as("the prescribed medicine appears in pharmacy sales").isNotNull();
        assertThat(amount(amoxil.get("quantity"))).isGreaterThanOrEqualTo(new BigDecimal("7"));
        assertThat(((Number) amoxil.get("lineCount")).longValue()).isGreaterThanOrEqualTo(1);
        assertThat(amount(sales.get("totalQuantity"))).isGreaterThanOrEqualTo(new BigDecimal("7"));
    }

    // ----- helpers ---------------------------------------------------------

    private static BigDecimal amount(Object raw) {
        return raw == null ? BigDecimal.ZERO : new BigDecimal(raw.toString());
    }

    private String registerPatient(String first, String last, String type) {
        return (String) expectOk(post(
                "/patients",
                Map.of("firstName", first, "lastName", last,
                        "dateOfBirth", LocalDate.now().minusYears(33).toString(),
                        "gender", "FEMALE", "type", type, "paymentType", "CASH"),
                Map.class)).get("uid");
    }

    private static <T> T expectOk(org.springframework.http.ResponseEntity<T> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s", response.getStatusCode(), response.getBody())
                .isTrue();
        T body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }
}
