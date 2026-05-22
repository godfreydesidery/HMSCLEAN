package com.otapp.hmis.engine.procurement;

import static org.assertj.core.api.Assertions.assertThat;

import com.otapp.hmis.engine.AuthenticatedIntegrationTest;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Procurement segregation of duties (PROCESS_MISMATCHES.md M18): verifying a
 * purchase order / GRN needs PROCUREMENT_VERIFY (manager), approving needs
 * PROCUREMENT_APPROVE (director). The PROCUREMENT role carries VERIFY but not
 * APPROVE, so a procurement officer can verify but cannot approve.
 *
 * Authorization is enforced before the handler runs, so this exercises the
 * gate against a (non-existent) uid: a denied call returns 403; an allowed
 * call passes authorization and falls through to a 404.
 */
class ProcurementSegregationIT extends AuthenticatedIntegrationTest {

    @Test
    @SuppressWarnings("rawtypes")
    void procurementOfficerCanVerifyButNotApprove() {
        String officer = tokenForRoles("procofficer", Set.of("PROCUREMENT"));
        String bogus = "01J5KQRPCD0000000000000XXX";

        // VERIFY is permitted for the PROCUREMENT role → passes authz, 404 (no such PO).
        ResponseEntity<Map> verify = postAs(officer,
                "/procurement/purchase-orders/uid/" + bogus + "/verify", null, Map.class);
        assertThat(verify.getStatusCode())
                .as("PROCUREMENT role may verify (authorization passes; PO not found)")
                .isNotEqualTo(HttpStatus.FORBIDDEN);

        // APPROVE requires PROCUREMENT_APPROVE, which the PROCUREMENT role lacks → 403.
        ResponseEntity<Map> approve = postAs(officer,
                "/procurement/purchase-orders/uid/" + bogus + "/approve", null, Map.class);
        assertThat(approve.getStatusCode())
                .as("PROCUREMENT role must not be able to approve")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void rootRetainsBothVerifyAndApprove() {
        String bogus = "01J5KQRPCD0000000000000XXX";
        // ROOT has every privilege → both pass authz and fall through to 404, never 403.
        assertThat(post("/procurement/purchase-orders/uid/" + bogus + "/verify", null, Map.class).getStatusCode())
                .isNotEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post("/procurement/purchase-orders/uid/" + bogus + "/approve", null, Map.class).getStatusCode())
                .isNotEqualTo(HttpStatus.FORBIDDEN);
    }
}
