package com.otapp.hmis.engine.hr;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Phase 47 — HR payroll lifecycle:
 *
 *   Create period → register 2 employees → upsert items → approve →
 *   pay → assert state machine refuses post-approval mutations and
 *   double-pay.
 */
class PayrollIT extends AuthenticatedIntegrationTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void createPeriodAddItemsApproveAndPay() {
        String suffix = String.valueOf(System.nanoTime() % 1_000_000L);

        // 1. Two employees.
        String emp1 = createEmployee("EMP-A-" + suffix);
        String emp2 = createEmployee("EMP-B-" + suffix);

        // 2. New payroll period.
        Map period = expectOk(post(
                "/hr/payroll/periods",
                Map.of(
                        "code",      "PAY-" + suffix,
                        "label",     "Test period " + suffix,
                        "startDate", LocalDate.now().withDayOfMonth(1).toString(),
                        "endDate",   LocalDate.now().withDayOfMonth(28).toString(),
                        "currency",  "TZS",
                        "note",      "Phase 47 IT"),
                Map.class));
        String periodUid = (String) period.get("uid");
        assertThat(period.get("status")).isEqualTo("DRAFT");

        // 3. Upsert items.
        Map item1 = expectOk(post(
                "/hr/payroll/periods/uid/" + periodUid + "/items",
                Map.of(
                        "employeeUid",     emp1,
                        "grossPay",        "1000000.00",
                        "totalDeductions", "150000.00"),
                Map.class));
        assertThat(new BigDecimal(item1.get("netPay").toString()))
                .isEqualByComparingTo("850000.00");

        expectOk(post(
                "/hr/payroll/periods/uid/" + periodUid + "/items",
                Map.of(
                        "employeeUid",     emp2,
                        "grossPay",        "800000.00",
                        "totalDeductions", "100000.00"),
                Map.class));

        // 4. Upsert again — same (period, employee) updates the row.
        Map item1updated = expectOk(post(
                "/hr/payroll/periods/uid/" + periodUid + "/items",
                Map.of(
                        "employeeUid",     emp1,
                        "grossPay",        "1100000.00",
                        "totalDeductions", "200000.00"),
                Map.class));
        assertThat(item1updated.get("uid")).isEqualTo(item1.get("uid"));
        assertThat(new BigDecimal(item1updated.get("netPay").toString()))
                .isEqualByComparingTo("900000.00");

        // 5. View — period totals should reflect both items.
        Map view = expectOk(get("/hr/payroll/periods/uid/" + periodUid, Map.class));
        Map periodDto = (Map) view.get("period");
        List items = (List) view.get("items");
        assertThat(items).hasSize(2);
        assertThat(((Number) periodDto.get("itemCount")).intValue()).isEqualTo(2);
        assertThat(new BigDecimal(periodDto.get("totalNet").toString()))
                .as("900,000 + 700,000")
                .isEqualByComparingTo("1600000.00");

        // 6. Approve.
        Map approved = expectOk(post("/hr/payroll/periods/uid/" + periodUid + "/approve", null, Map.class));
        assertThat(approved.get("status")).isEqualTo("APPROVED");
        assertThat(approved.get("approvedByUsername")).isEqualTo("root");

        // 7. APPROVED locks items — further upsert refused.
        ResponseEntity<Map> locked = post(
                "/hr/payroll/periods/uid/" + periodUid + "/items",
                Map.of(
                        "employeeUid",     emp2,
                        "grossPay",        "999999.99",
                        "totalDeductions", "0.00"),
                Map.class);
        assertThat(locked.getStatusCode().is4xxClientError())
                .as("APPROVED period should refuse item edits")
                .isTrue();

        // 8. Pay.
        Map paid = expectOk(post("/hr/payroll/periods/uid/" + periodUid + "/pay", null, Map.class));
        assertThat(paid.get("status")).isEqualTo("PAID");

        // 9. PAID is terminal — re-pay refused, cancel refused.
        ResponseEntity<Map> rePay = post("/hr/payroll/periods/uid/" + periodUid + "/pay", null, Map.class);
        assertThat(rePay.getStatusCode().is4xxClientError()).isTrue();

        ResponseEntity<Map> postCancel = post(
                "/hr/payroll/periods/uid/" + periodUid + "/cancel",
                Map.of("reason", "Late attempt"),
                Map.class);
        assertThat(postCancel.getStatusCode().is4xxClientError())
                .as("Paid periods cannot be cancelled")
                .isTrue();
    }

    @SuppressWarnings("rawtypes")
    private String createEmployee(String tagSuffix) {
        Map emp = expectOk(post(
                "/hr/employees",
                Map.of(
                        "firstName",  "Pay",
                        "lastName",   tagSuffix,
                        "hireDate",   LocalDate.now().minusYears(2).toString(),
                        "designation","Nurse",
                        "department", "Wards"),
                Map.class));
        return (String) emp.get("uid");
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
