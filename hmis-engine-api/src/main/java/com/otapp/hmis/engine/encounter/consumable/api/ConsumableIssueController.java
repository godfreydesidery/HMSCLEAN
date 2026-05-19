package com.otapp.hmis.engine.encounter.consumable.api;

import com.otapp.hmis.engine.encounter.consumable.application.ConsumableIssueDtos.ConsumableIssueDto;
import com.otapp.hmis.engine.encounter.consumable.application.ConsumableIssueDtos.IssueConsumableRequest;
import com.otapp.hmis.engine.encounter.consumable.application.ConsumableIssueService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Patient consumable chart")
@RestController
@RequestMapping("/encounters/admissions/uid/{admissionUid}/consumables")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class ConsumableIssueController {

    private final ConsumableIssueService consumableIssueService;

    @PostMapping
    public ResponseEntity<ConsumableIssueDto> issue(@PathVariable String admissionUid,
                                                    @Valid @RequestBody IssueConsumableRequest request) {
        return ResponseEntity.ok(consumableIssueService.issue(admissionUid, request));
    }

    @GetMapping
    public ResponseEntity<List<ConsumableIssueDto>> list(@PathVariable String admissionUid) {
        return ResponseEntity.ok(consumableIssueService.listForAdmission(admissionUid));
    }
}
