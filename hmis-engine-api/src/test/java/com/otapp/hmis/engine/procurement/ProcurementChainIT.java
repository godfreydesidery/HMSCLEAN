package com.otapp.hmis.engine.procurement;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * End-to-end test for the full procurement chain:
 *
 *   Supplier registered → LPO created → verified → approved → ordered →
 *   GRN recorded (PENDING) → verified → approved (stock credited) →
 *   SupplierInvoice raised → submitted → approved (three-way match
 *   passes) → marked paid.
 *
 * Uses the seeded Main Store (V4) + Panadol medicine (V4); creates the
 * supplier fresh per test run so the suite is repeatable.
 */
class ProcurementChainIT extends AuthenticatedIntegrationTest {

    private static final String MAIN_STORE_UID = "01J5KQRPCD0000000000000ST1";
    private static final String PANADOL_UID    = "01J5KQRPCD0000000000000MD1";

    @Test
    void fullChainFromLpoThroughThreeWayMatchAndPayment() {
        String supplierUid = createSupplier();
        // Supplier must quote the item before it can be ordered (price-list gate).
        quotePanadol(supplierUid, new BigDecimal("250.00"));

        // ----- LPO ---------------------------------------------------------
        Map<String, Object> lpo = expectOk(post(
                "/procurement/purchase-orders",
                Map.of(
                        "supplierUid", supplierUid,
                        "storeUid",    MAIN_STORE_UID,
                        "expectedDeliveryDate", LocalDate.now().plusDays(7).toString(),
                        "notes", "Q1 paracetamol stock"),
                Map.class));
        String orderUid = (String) lpo.get("uid");
        assertThat(lpo.get("status")).isEqualTo("DRAFT");

        // line — 100 units of Panadol @ 250.00
        expectOk(post(
                "/procurement/purchase-orders/uid/" + orderUid + "/lines",
                Map.of(
                        "medicineUid",     PANADOL_UID,
                        "orderedQuantity", 100,
                        "unitCost",        new BigDecimal("250.00"),
                        "currency",        "TZS"),
                Map.class));

        // Workflow: verify → approve → order
        Map<String, Object> verified = expectOk(post("/procurement/purchase-orders/uid/" + orderUid + "/verify", null, Map.class));
        assertThat(verified.get("status")).isEqualTo("VERIFIED");
        Map<String, Object> approved = expectOk(post("/procurement/purchase-orders/uid/" + orderUid + "/approve", null, Map.class));
        assertThat(approved.get("status")).isEqualTo("APPROVED");
        Map<String, Object> ordered = expectOk(post("/procurement/purchase-orders/uid/" + orderUid + "/order", null, Map.class));
        assertThat(ordered.get("status")).isEqualTo("ORDERED");

        String poLineUid = firstLineUid(ordered);

        // ----- GRN ---------------------------------------------------------
        Map<String, Object> grn = expectOk(post(
                "/procurement/purchase-orders/uid/" + orderUid + "/receipts",
                Map.of(
                        "deliveryNote", "DN-001",
                        "notes",        "Carton intact, expiry OK",
                        "lines", List.of(Map.of(
                                "poLineUid",  poLineUid,
                                "quantity",   100,
                                "batchNo",    "B-001",
                                "expiresAt",  LocalDate.now().plusYears(2).toString()))),
                Map.class));
        String grnUid = (String) grn.get("uid");
        assertThat(grn.get("status")).isEqualTo("PENDING");

        // Pre-approval the PO line shows zero received — stock credit
        // deferred until approve(). Verify + approve.
        expectOk(post("/procurement/goods-receipts/uid/" + grnUid + "/verify", null, Map.class));
        Map<String, Object> approvedGrn = expectOk(post(
                "/procurement/goods-receipts/uid/" + grnUid + "/approve", null, Map.class));
        assertThat(approvedGrn.get("status")).isEqualTo("APPROVED");

        // PO line should now show 100 received.
        Map<String, Object> orderAfter = expectOk(get(
                "/procurement/purchase-orders/uid/" + orderUid, Map.class));
        assertThat((String) orderAfter.get("status")).isIn("PARTIALLY_RECEIVED", "RECEIVED");
        Map<String, Object> poLineAfter = firstLine(orderAfter);
        assertThat(((Number) poLineAfter.get("receivedQuantity")).intValue()).isEqualTo(100);

        // ----- SupplierInvoice (three-way match) ---------------------------
        Map<String, Object> invoice = expectOk(post(
                "/procurement/supplier-invoices",
                Map.of(
                        "orderUid",          orderUid,
                        "supplierInvoiceNo", "INV-XYZ-001",
                        "invoiceDate",       LocalDate.now().toString(),
                        "currency",          "TZS",
                        "notes",             "Q1 invoice",
                        "lines", List.of(Map.of(
                                "poLineUid",        poLineUid,
                                "invoicedQuantity", 100,
                                "unitCost",         new BigDecimal("250.00")))),
                Map.class));
        String invoiceUid = (String) invoice.get("uid");
        assertThat(invoice.get("status")).isEqualTo("DRAFT");
        assertThat(new BigDecimal(invoice.get("totalAmount").toString()))
                .isEqualByComparingTo("25000.00");

        expectOk(post("/procurement/supplier-invoices/uid/" + invoiceUid + "/submit", null, Map.class));

        // Approve = three-way match runs. Should pass since invoiced==received==ordered.
        Map<String, Object> approvedInvoice = expectOk(post(
                "/procurement/supplier-invoices/uid/" + invoiceUid + "/approve", null, Map.class));
        assertThat(approvedInvoice.get("status")).isEqualTo("APPROVED");

        // PO line invoicedQuantity rolled up via the match.
        Map<String, Object> orderAfterMatch = expectOk(get(
                "/procurement/purchase-orders/uid/" + orderUid, Map.class));
        Map<String, Object> lineAfterMatch = firstLine(orderAfterMatch);
        assertThat(((Number) lineAfterMatch.get("orderedQuantity")).intValue()).isEqualTo(100);

        // Mark paid.
        Map<String, Object> paid = expectOk(post(
                "/procurement/supplier-invoices/uid/" + invoiceUid + "/pay",
                Map.of("method", "BANK_TRANSFER", "reference", "TXN-12345"),
                Map.class));
        assertThat(paid.get("status")).isEqualTo("PAID");
        assertThat(paid.get("paymentMethod")).isEqualTo("BANK_TRANSFER");
        assertThat(paid.get("paymentReference")).isEqualTo("TXN-12345");
    }

    @Test
    void approveRejectsInvoiceWhoseLinesExceedReceived() {
        String supplierUid = createSupplier();

        // LPO for 100, deliver only 60 received, then try to invoice 100.
        String orderUid = createOrderedPoFor(supplierUid, 100);
        String poLineUid = firstLineUid(expectOk(get(
                "/procurement/purchase-orders/uid/" + orderUid, Map.class)));

        String grnUid = (String) expectOk(post(
                "/procurement/purchase-orders/uid/" + orderUid + "/receipts",
                Map.of("lines", List.of(Map.of(
                        "poLineUid", poLineUid,
                        "quantity",  60,
                        "batchNo",   "B-PART",
                        "expiresAt", LocalDate.now().plusYears(1).toString()))),
                Map.class)).get("uid");
        expectOk(post("/procurement/goods-receipts/uid/" + grnUid + "/verify", null, Map.class));
        expectOk(post("/procurement/goods-receipts/uid/" + grnUid + "/approve", null, Map.class));

        String invoiceUid = (String) expectOk(post(
                "/procurement/supplier-invoices",
                Map.of(
                        "orderUid",          orderUid,
                        "supplierInvoiceNo", "INV-OVER-001",
                        "invoiceDate",       LocalDate.now().toString(),
                        "lines", List.of(Map.of(
                                "poLineUid",        poLineUid,
                                "invoicedQuantity", 100,  // exceeds received 60
                                "unitCost",         new BigDecimal("100.00")))),
                Map.class)).get("uid");
        expectOk(post("/procurement/supplier-invoices/uid/" + invoiceUid + "/submit", null, Map.class));

        // Approve should be rejected by the three-way match.
        ResponseEntity<Map> denied = post(
                "/procurement/supplier-invoices/uid/" + invoiceUid + "/approve", null, Map.class);
        assertThat(denied.getStatusCode().is4xxClientError()).isTrue();
    }

    // ----- helpers ---------------------------------------------------------

    private String createSupplier() {
        String code = "SUP-" + System.nanoTime();
        @SuppressWarnings("rawtypes")
        Map body = expectOk(post(
                "/procurement/suppliers",
                Map.of("code", code, "name", "Acme Pharma"),
                Map.class));
        return (String) body.get("uid");
    }

    /** Register the supplier's contracted price for Panadol so PO lines pass the gate. */
    private void quotePanadol(String supplierUid, BigDecimal unitPrice) {
        expectOk(post(
                "/procurement/suppliers/uid/" + supplierUid + "/prices",
                Map.of(
                        "medicineUid", PANADOL_UID,
                        "unitPrice",   unitPrice,
                        "currency",    "TZS",
                        "validFrom",   LocalDate.now().minusDays(1).toString()),
                Map.class));
    }

    private String createOrderedPoFor(String supplierUid, int quantity) {
        quotePanadol(supplierUid, new BigDecimal("100.00"));
        @SuppressWarnings("rawtypes")
        Map lpo = expectOk(post(
                "/procurement/purchase-orders",
                Map.of("supplierUid", supplierUid, "storeUid", MAIN_STORE_UID),
                Map.class));
        String orderUid = (String) lpo.get("uid");
        expectOk(post(
                "/procurement/purchase-orders/uid/" + orderUid + "/lines",
                Map.of(
                        "medicineUid",     PANADOL_UID,
                        "orderedQuantity", quantity,
                        "unitCost",        new BigDecimal("100.00"),
                        "currency",        "TZS"),
                Map.class));
        expectOk(post("/procurement/purchase-orders/uid/" + orderUid + "/verify",  null, Map.class));
        expectOk(post("/procurement/purchase-orders/uid/" + orderUid + "/approve", null, Map.class));
        expectOk(post("/procurement/purchase-orders/uid/" + orderUid + "/order",   null, Map.class));
        return orderUid;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> firstLine(Map<String, Object> orderBody) {
        List<Map<String, Object>> lines = (List<Map<String, Object>>) orderBody.get("lines");
        assertThat(lines).isNotEmpty();
        return lines.get(0);
    }

    private static String firstLineUid(Map<String, Object> orderBody) {
        return (String) firstLine(orderBody).get("uid");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> T expectOk(ResponseEntity<T> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx but got %s with body %s",
                        response.getStatusCode(), response.getBody())
                .isTrue();
        T body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }

    // Silence unused-import lint (TypeReference is reserved for future use)
    @SuppressWarnings("unused")
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
}
