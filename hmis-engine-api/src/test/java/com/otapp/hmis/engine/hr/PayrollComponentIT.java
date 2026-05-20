package com.otapp.hmis.engine.hr;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Section E/F — configurable payroll components + auto-prefill compute.
 *
 *   Configure a FIXED earning, a PERCENT-of-gross deduction, and a
 *   progressive BAND deduction, then assert the stateless compute endpoint
 *   derives gross / deductions / net correctly — including worked-day
 *   proration of the basic. No statutory rates are hard-coded; the test
 *   supplies them as data, mirroring how HR configures the hospital's.
 */
class PayrollComponentIT extends AuthenticatedIntegrationTest {

    @Test
    @SuppressWarnings("rawtypes")
    void configuredComponentsDriveAutoPrefillCompute() {
        // FIXED earning — a flat allowance, never pro-rated.
        expectOk(post("/hr/payroll/components", Map.of(
                "code", "HOUSING", "name", "Housing allowance",
                "type", "EARNING", "method", "FIXED", "fixedAmount", "50000"), Map.class));

        // PERCENT deduction — 10% of gross.
        expectOk(post("/hr/payroll/components", Map.of(
                "code", "NSSF", "name", "Pension 10%",
                "type", "DEDUCTION", "method", "PERCENT", "base", "GROSS", "percentRate", "0.10"), Map.class));

        // BAND deduction — progressive PAYE on gross (open-ended top band).
        expectOk(post("/hr/payroll/components", Map.of(
                "code", "PAYE", "name", "PAYE",
                "type", "DEDUCTION", "method", "BAND", "base", "GROSS",
                "bands", List.of(
                        Map.of("fromAmount", "0",      "toAmount", "270000", "rate", "0"),
                        Map.of("fromAmount", "270000", "toAmount", "520000", "rate", "0.08"),
                        openBand("520000", "0.20"))), Map.class));

        // 1. Full-month compute on a 600,000 basic.
        Map full = expectOk(post("/hr/payroll/compute", Map.of("basicSalary", "600000"), Map.class));
        assertThat(bd(full.get("effectiveBasic"))).isEqualByComparingTo("600000.00");
        assertThat(bd(full.get("totalEarnings"))).isEqualByComparingTo("50000.00");
        assertThat(bd(full.get("grossPay"))).isEqualByComparingTo("650000.00");
        // NSSF 65,000 + PAYE (20,000 + 26,000) = 111,000.
        assertThat(bd(full.get("totalDeductions"))).isEqualByComparingTo("111000.00");
        assertThat(bd(full.get("netPay"))).isEqualByComparingTo("539000.00");
        assertThat((List<?>) full.get("lines")).hasSize(3);

        // 2. Worked-day proration: 15 of 30 days halves the basic (the fixed
        //    allowance stays flat); deductions re-derive off the new gross.
        Map prorated = expectOk(post("/hr/payroll/compute",
                Map.of("basicSalary", "600000", "workedDays", 15, "periodDays", 30), Map.class));
        assertThat(bd(prorated.get("effectiveBasic"))).isEqualByComparingTo("300000.00");
        assertThat(bd(prorated.get("grossPay"))).isEqualByComparingTo("350000.00");
        // NSSF 35,000 + PAYE (80,000 @ 8% = 6,400) = 41,400.
        assertThat(bd(prorated.get("totalDeductions"))).isEqualByComparingTo("41400.00");
        assertThat(bd(prorated.get("netPay"))).isEqualByComparingTo("308600.00");
    }

    /** Map.of rejects null values — build the open-ended top band without toAmount. */
    private static Map<String, String> openBand(String from, String rate) {
        Map<String, String> band = new HashMap<>();
        band.put("fromAmount", from);
        band.put("rate", rate);
        return band;
    }

    private static BigDecimal bd(Object value) {
        return new BigDecimal(value.toString());
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
