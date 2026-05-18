package com.otapp.hmis.engine.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Tests the cashier-shift open/close + reconciliation flow. No real
 * cash payments are recorded inside the test, so the expected-cash
 * total is zero — the test verifies that:
 *
 *   variance = declared - (openingFloat + sumCash)
 *
 * computes correctly and that the partial unique index blocks opening
 * a second shift for the same user.
 */
class CashierShiftIT extends AuthenticatedIntegrationTest {

    @Test
    @SuppressWarnings("rawtypes")
    void openCloseFlowComputesVarianceAgainstZeroTakings() {
        // Open with a 50,000 float.
        Map opened = expectOk(post(
                "/billing/cashier-shifts/open",
                Map.of(
                        "openingFloat", new BigDecimal("50000.00"),
                        "currency",     "TZS"),
                Map.class));
        assertThat(opened.get("status")).isEqualTo("OPEN");
        assertThat(opened.get("cashierUsername")).isEqualTo("root");
        assertThat(new BigDecimal(opened.get("openingFloat").toString()))
                .isEqualByComparingTo("50000.00");

        // A second open for the same user is rejected by the service guard
        // (and by the DB partial unique index as a defence in depth).
        ResponseEntity<Map> secondOpen = post(
                "/billing/cashier-shifts/open",
                Map.of("openingFloat", new BigDecimal("100.00")),
                Map.class);
        assertThat(secondOpen.getStatusCode().is4xxClientError()).isTrue();

        // /me returns the open shift.
        Map mine = expectOk(get("/billing/cashier-shifts/me", Map.class));
        assertThat(mine.get("uid")).isEqualTo(opened.get("uid"));

        // Close: declare 50,000 — no payments were taken so expected =
        // opening + 0 = 50,000 and variance should be exactly 0.
        Map closed = expectOk(post(
                "/billing/cashier-shifts/close",
                Map.of(
                        "closingDeclaredAmount", new BigDecimal("50000.00"),
                        "note", "Clean close — test"),
                Map.class));
        assertThat(closed.get("status")).isEqualTo("CLOSED");
        assertThat(new BigDecimal(closed.get("closingExpectedAmount").toString()))
                .isEqualByComparingTo("50000.00");
        assertThat(new BigDecimal(closed.get("variance").toString()))
                .isEqualByComparingTo("0.00");

        // After close, /me has no open shift — should 404.
        ResponseEntity<Map> noOpen = get("/billing/cashier-shifts/me", Map.class);
        assertThat(noOpen.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void closingShortRecordsNegativeVariance() {
        // Open 10,000 float, close declaring only 9,500.
        Map opened = expectOk(post(
                "/billing/cashier-shifts/open",
                Map.of("openingFloat", new BigDecimal("10000.00")),
                Map.class));
        assertThat(opened.get("status")).isEqualTo("OPEN");

        Map closed = expectOk(post(
                "/billing/cashier-shifts/close",
                Map.of(
                        "closingDeclaredAmount", new BigDecimal("9500.00"),
                        "note", "Short by 500"),
                Map.class));
        assertThat(new BigDecimal(closed.get("variance").toString()))
                .isEqualByComparingTo("-500.00");
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
