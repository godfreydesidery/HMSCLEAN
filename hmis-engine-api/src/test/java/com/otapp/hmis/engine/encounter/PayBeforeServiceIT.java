package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Pay-before-service gate (PROCESS_MISMATCHES.md M13): a consultation-bound lab
 * order is billed onto the consultation invoice when raised, and cannot be
 * completed (result released) for a CASH patient until that invoice is settled.
 *
 * Uses the seeded OPD clinic + CBC lab test; ROOT as the actor. A CBC cash price
 * is set so the order is non-zero (a zero-price order would auto-settle).
 */
class PayBeforeServiceIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";
    private static final String CBC_UID        = "01J5KQRPCD0000000000000LB1";
    private static final BigDecimal CBC_PRICE  = new BigDecimal("8000.00");

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void cashLabOrderCannotCompleteUntilItsBillIsSettled() {
        // CBC has a cash price so the order is billable (non-zero). Create-only now;
        // tolerate 409 if a prior run in this JVM already created the cell.
        ResponseEntity<Map> priceResp = post("/masterdata/service-prices",
                Map.of("kind", "LAB_TEST", "serviceUid", CBC_UID, "amount", CBC_PRICE, "currency", "TZS"),
                Map.class);
        assertThat(priceResp.getStatusCode().is2xxSuccessful() || priceResp.getStatusCode().value() == 409).isTrue();

        String patientUid = stringField(expectOk(post(
                "/patients",
                Map.of("firstName", "Pay", "lastName", "First",
                        "dateOfBirth", LocalDate.now().minusYears(30).toString(),
                        "gender", "MALE", "type", "OUTPATIENT", "paymentType", "CASH"),
                Map.class)), "uid");

        String consultationUid = stringField(expectOk(post(
                "/encounters/consultations",
                Map.of("patientUid", patientUid, "clinicUid", OPD_CLINIC_UID,
                        "clinicianUsername", clinicianAffiliatedWith(OPD_CLINIC_UID),
                        "paymentType", "CASH", "reason", "fever"),
                Map.class)), "uid");

        // Doctor raises a lab order → billed onto the consultation invoice up front.
        String orderUid = stringField(expectOk(post(
                "/encounters/consultations/uid/" + consultationUid + "/orders",
                Map.of("kind", "LAB_TEST", "serviceUid", CBC_UID, "urgency", "NORMAL"),
                Map.class)), "uid");

        // Tech accepts + starts the work, but cannot complete (release result) unpaid.
        expectOk(post("/encounters/orders/uid/" + orderUid + "/accept", null, Map.class));
        expectOk(post("/encounters/orders/uid/" + orderUid + "/start", null, Map.class));
        ResponseEntity<Map> denied = post(
                "/encounters/orders/uid/" + orderUid + "/complete",
                Map.of("result", "WBC normal"), Map.class);
        assertThat(denied.getStatusCode().is4xxClientError())
                .as("CASH lab order must not complete until its bill is settled").isTrue();

        // Cashier settles the consultation invoice (which now carries the lab line).
        Map invoice = expectOk(get("/billing/consultations/uid/" + consultationUid + "/invoice", Map.class));
        String invoiceUid = (String) invoice.get("uid");
        BigDecimal balance = new BigDecimal(invoice.get("balance").toString());
        assertThat(balance).as("Invoice should carry the lab charge").isGreaterThanOrEqualTo(CBC_PRICE);
        expectOk(post("/billing/invoices/uid/" + invoiceUid + "/payments",
                Map.of("method", "CASH", "amount", balance, "currency", "TZS"), Map.class));

        // Now the result can be released.
        Map completed = expectOk(post(
                "/encounters/orders/uid/" + orderUid + "/complete",
                Map.of("result", "WBC normal"), Map.class));
        assertThat(completed.get("status")).isEqualTo("COMPLETED");
    }

    private static <T> T expectOk(ResponseEntity<T> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s", response.getStatusCode(), response.getBody())
                .isTrue();
        T body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }
}
