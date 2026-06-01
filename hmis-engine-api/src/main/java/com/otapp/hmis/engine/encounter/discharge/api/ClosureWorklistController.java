package com.otapp.hmis.engine.encounter.discharge.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.encounter.discharge.application.DischargePlanDtos.ClosureWorklistItem;
import com.otapp.hmis.engine.encounter.discharge.application.DischargePlanService;
import com.otapp.hmis.engine.encounter.discharge.domain.ClosureSubject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The closure worklist (DISCH-1) — the single queue of PENDING closure plans
 * (inpatient discharge/death/referral + outpatient death/referral) awaiting a
 * second approver. The actual approve / cancel actions stay on the subject's own
 * endpoint ({@code .../discharge-plan/approve} or {@code .../closure/approve});
 * this surface just lists what is waiting and deep-links to it.
 */
@Tag(name = "Closure worklist")
@RestController
@RequestMapping("/encounters/closures")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class ClosureWorklistController {

    private final DischargePlanService planService;

    @GetMapping("/worklist")
    public ResponseEntity<PageResponse<ClosureWorklistItem>> worklist(
            @RequestParam(required = false) ClosureSubject subjectType,
            Pageable pageable) {
        return ResponseEntity.ok(planService.worklist(subjectType, pageable));
    }
}
