package com.otapp.hmis.engine.encounter;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * OPC-1 — faithful two-phase consultation transfer (legacy Zana-HMIS clinic-to-clinic
 * hand-off):
 *
 *   1. the treating doctor raises a PENDING transfer to a target CLINIC only
 *      (no clinician); the source consultation flips to TRANSFERRED;
 *   2. the request shows in the receiving queue;
 *   3. reception accepts it, choosing the receiving clinician — a FRESH
 *      consultation is booked at the target clinic, the transfer COMPLETES and
 *      the source is linked to it;
 *   4. or the initiating doctor reverts it — the transfer CANCELs and the source
 *      returns to IN_PROGRESS.
 *
 * Gates: source must be IN_PROGRESS, the patient may not already have a pending
 * transfer, and the target clinic must differ from the source clinic.
 *
 * Uses the seeded OPD + PED clinics.
 */
class ConsultationTransferIT extends AuthenticatedIntegrationTest {

    private static final String OPD_CLINIC_UID = "01J5KQRPCD0000000000000CN1";
    private static final String PED_CLINIC_UID = "01J5KQRPCD0000000000000CN2";
    private static final String CBC_UID        = "01J5KQRPCD0000000000000LB1";

    /**
     * Register a CASH patient, pay the registration fee, book a consultation at OPD
     * and open it (settle fee + start) so it is IN_PROGRESS — the transfer
     * pre-condition. Returns the in-progress consultation uid.
     */
    @SuppressWarnings("rawtypes")
    private String openOpdConsultation(String firstName) {
        String patientUid = (String) expectOk(post(
                "/patients",
                Map.of(
                        "firstName",   firstName,
                        "lastName",    "Transfer",
                        "dateOfBirth", LocalDate.now().minusYears(20).toString(),
                        "gender",      "MALE",
                        "type",        "OUTPATIENT",
                        "paymentType", "CASH"),
                Map.class)).get("uid");

        Map regFee = expectOk(get(
                "/billing/patients/uid/" + patientUid + "/registration-fee", Map.class));
        String regInvoiceUid = (String) regFee.get("uid");
        expectOk(post(
                "/billing/invoices/uid/" + regInvoiceUid + "/payments",
                Map.of("method", "CASH", "amount", new BigDecimal("5000.00"), "currency", "TZS"),
                Map.class));

        Map consult = expectOk(post(
                "/encounters/consultations",
                Map.of(
                        "patientUid",        patientUid,
                        "clinicUid",         OPD_CLINIC_UID,
                        "clinicianUsername", clinicianAffiliatedWith(OPD_CLINIC_UID),
                        "paymentType",       "CASH",
                        "reason",            "fever"),
                Map.class));
        String consultUid = (String) consult.get("uid");
        openConsultation(consultUid);
        return consultUid;
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void createTransferFlipsSourceAndQueuesPending() {
        String sourceUid = openOpdConsultation("Pend");

        // Raise a PENDING transfer to Pediatrics — clinic only, no clinician.
        Map transfer = expectOk(post(
                "/encounters/consultations/uid/" + sourceUid + "/transfer",
                Map.of("targetClinicUid", PED_CLINIC_UID, "reason", "Better suited to pediatrics"),
                Map.class));
        String transferUid = (String) transfer.get("uid");
        assertThat(transfer.get("status")).isEqualTo("PENDING");
        assertThat(transfer.get("targetClinicUid")).isEqualTo(PED_CLINIC_UID);
        assertThat(transfer.get("sourceConsultationUid")).isEqualTo(sourceUid);
        assertThat(transfer.get("id")).isNotNull();

        // Source consultation now reads TRANSFERRED with NO receiver yet.
        Map source = expectOk(get("/encounters/consultations/uid/" + sourceUid, Map.class));
        assertThat(source.get("status")).isEqualTo("TRANSFERRED");
        assertThat(source.get("transferredToConsultationUid")).isNull();

        // It shows up in the receiving (PENDING) queue.
        Map queue = expectOk(get("/encounters/consultations/transfers?status=PENDING", Map.class));
        List<Map<String, Object>> rows = (List<Map<String, Object>>) queue.get("content");
        assertThat(rows).anyMatch(r -> transferUid.equals(r.get("uid")));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void cancelRevertsSourceToInProgress() {
        String sourceUid = openOpdConsultation("Revert");

        Map transfer = expectOk(post(
                "/encounters/consultations/uid/" + sourceUid + "/transfer",
                Map.of("targetClinicUid", PED_CLINIC_UID),
                Map.class));
        String transferUid = (String) transfer.get("uid");

        // The initiating doctor reverts it.
        Map reverted = expectOk(post(
                "/encounters/consultations/transfers/uid/" + transferUid + "/cancel",
                Map.of("reason", "changed mind"),
                Map.class));
        // Returns the source consultation, now back IN_PROGRESS.
        assertThat(reverted.get("uid")).isEqualTo(sourceUid);
        assertThat(reverted.get("status")).isEqualTo("IN_PROGRESS");

        // The transfer is CANCELLED.
        Map source = expectOk(get("/encounters/consultations/uid/" + sourceUid, Map.class));
        assertThat(source.get("status")).isEqualTo("IN_PROGRESS");
        assertThat(source.get("transferredToConsultationUid")).isNull();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void acceptBooksFreshConsultationAtTargetAndLinksSource() {
        String sourceUid = openOpdConsultation("Accept");

        Map transfer = expectOk(post(
                "/encounters/consultations/uid/" + sourceUid + "/transfer",
                Map.of("targetClinicUid", PED_CLINIC_UID, "reason", "needs pediatrics"),
                Map.class));
        String transferUid = (String) transfer.get("uid");

        // Reception accepts, choosing a clinician affiliated with the target clinic.
        String pedClinician = clinicianAffiliatedWith(PED_CLINIC_UID);
        Map receiver = expectOk(post(
                "/encounters/consultations/transfers/uid/" + transferUid + "/accept",
                Map.of("clinicianUsername", pedClinician),
                Map.class));
        String receiverUid = (String) receiver.get("uid");
        assertThat(receiver.get("status")).isEqualTo("BOOKED");
        assertThat(receiver.get("clinicUid")).isEqualTo(PED_CLINIC_UID);
        assertThat(receiver.get("clinicianUsername")).isEqualTo(pedClinician);
        assertThat(receiver.get("transferredFromConsultationUid")).isEqualTo(sourceUid);

        // The source is now linked to the fresh consultation for audit.
        Map source = expectOk(get("/encounters/consultations/uid/" + sourceUid, Map.class));
        assertThat(source.get("status")).isEqualTo("TRANSFERRED");
        assertThat(source.get("transferredToConsultationUid")).isEqualTo(receiverUid);

        // The transfer reads COMPLETED (it has dropped out of the PENDING queue).
        Map completedQueue = expectOk(get(
                "/encounters/consultations/transfers?status=COMPLETED", Map.class));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) completedQueue.get("content");
        assertThat(rows).anyMatch(r -> transferUid.equals(r.get("uid"))
                && receiverUid.equals(r.get("createdConsultationUid")));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void sameClinicTransferIsRejected() {
        String sourceUid = openOpdConsultation("Same");

        ResponseEntity<Map> denied = post(
                "/encounters/consultations/uid/" + sourceUid + "/transfer",
                Map.of("targetClinicUid", OPD_CLINIC_UID),
                Map.class);
        assertThat(denied.getStatusCode().is4xxClientError())
                .as("Cannot transfer to the same clinic")
                .isTrue();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void pendingOrderBlocksTransferUntilCancelled() {
        String sourceUid = openOpdConsultation("Pending");

        // Raise an order — it sits in REQUESTED (un-acted) on the source.
        String orderUid = (String) expectOk(post(
                "/encounters/consultations/uid/" + sourceUid + "/orders",
                Map.of("kind", "LAB_TEST", "serviceUid", CBC_UID, "urgency", "NORMAL"),
                Map.class)).get("uid");

        // Transfer is blocked while the un-acted order exists.
        ResponseEntity<Map> blocked = post(
                "/encounters/consultations/uid/" + sourceUid + "/transfer",
                Map.of("targetClinicUid", PED_CLINIC_UID),
                Map.class);
        assertThat(blocked.getStatusCode().is4xxClientError())
                .as("a pending (un-accepted) order blocks the transfer").isTrue();

        // Cancel the order, then the transfer goes through.
        expectOk(post("/encounters/orders/uid/" + orderUid + "/cancel",
                Map.of("reason", "transferring patient"), Map.class));
        Map transfer = expectOk(post(
                "/encounters/consultations/uid/" + sourceUid + "/transfer",
                Map.of("targetClinicUid", PED_CLINIC_UID),
                Map.class));
        assertThat(transfer.get("status")).isEqualTo("PENDING");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void secondPendingTransferIsRejected() {
        String sourceUid = openOpdConsultation("Double");

        // First transfer succeeds.
        expectOk(post(
                "/encounters/consultations/uid/" + sourceUid + "/transfer",
                Map.of("targetClinicUid", PED_CLINIC_UID),
                Map.class));

        // The source is now TRANSFERRED, so a second transfer of the same
        // consultation fails the IN_PROGRESS gate AND the patient already has a
        // pending transfer — either way a 4xx.
        ResponseEntity<Map> denied = post(
                "/encounters/consultations/uid/" + sourceUid + "/transfer",
                Map.of("targetClinicUid", PED_CLINIC_UID),
                Map.class);
        assertThat(denied.getStatusCode().is4xxClientError())
                .as("A patient may have at most one pending transfer")
                .isTrue();
    }

    private static <T> T expectOk(ResponseEntity<T> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s",
                        response.getStatusCode(), response.getBody())
                .isTrue();
        T body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }
}
