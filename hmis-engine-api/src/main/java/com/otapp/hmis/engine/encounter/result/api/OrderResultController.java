package com.otapp.hmis.engine.encounter.result.api;

import com.otapp.hmis.engine.encounter.result.application.OrderResultDtos.OrderResultDto;
import com.otapp.hmis.engine.encounter.result.application.OrderResultDtos.SaveResultRequest;
import com.otapp.hmis.engine.encounter.result.application.OrderResultService;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.AnalyteTemplateDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Order results")
@RestController
@RequestMapping("/encounters/orders/uid/{orderUid}/result")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class OrderResultController {

    private final OrderResultService resultService;

    @GetMapping
    public ResponseEntity<OrderResultDto> find(@PathVariable String orderUid) {
        return resultService.findForOrder(orderUid)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Analyte definitions for building a LAB_TEST result-entry grid (empty for non-lab orders). */
    @GetMapping("/template")
    public ResponseEntity<List<AnalyteTemplateDto>> template(@PathVariable String orderUid) {
        return ResponseEntity.ok(resultService.analyteTemplate(orderUid));
    }

    @PutMapping
    public ResponseEntity<OrderResultDto> save(@PathVariable String orderUid,
                                               @Valid @RequestBody SaveResultRequest request) {
        return ResponseEntity.ok(resultService.save(orderUid, request));
    }

    @PostMapping("/finalize")
    public ResponseEntity<OrderResultDto> finalizeResult(@PathVariable String orderUid) {
        return ResponseEntity.ok(resultService.finalizeResult(orderUid));
    }

    @PutMapping("/amend")
    public ResponseEntity<OrderResultDto> amend(@PathVariable String orderUid,
                                                @Valid @RequestBody SaveResultRequest request) {
        return ResponseEntity.ok(resultService.amend(orderUid, request));
    }
}
