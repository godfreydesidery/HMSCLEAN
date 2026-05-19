package com.otapp.hmis.engine.encounter.labbatch.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.encounter.labbatch.application.LabBatchDtos.AddOrderRequest;
import com.otapp.hmis.engine.encounter.labbatch.application.LabBatchDtos.CancelLabBatchRequest;
import com.otapp.hmis.engine.encounter.labbatch.application.LabBatchDtos.CreateLabBatchRequest;
import com.otapp.hmis.engine.encounter.labbatch.application.LabBatchDtos.LabBatchDto;
import com.otapp.hmis.engine.encounter.labbatch.application.LabBatchService;
import com.otapp.hmis.engine.encounter.labbatch.domain.LabBatchStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Lab batches", description = "Group same-test lab orders for a single bench run.")
@RestController
@RequestMapping("/encounters/lab-batches")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class LabBatchController {

    private final LabBatchService labBatchService;

    @PostMapping
    public ResponseEntity<LabBatchDto> create(@Valid @RequestBody CreateLabBatchRequest request) {
        LabBatchDto created = labBatchService.create(request);
        URI loc = UriComponentsBuilder.fromPath("/encounters/lab-batches/uid/{batchUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<LabBatchDto>> search(
            @RequestParam(required = false) LabBatchStatus status,
            @RequestParam(required = false) String labTestTypeUid,
            Pageable pageable) {
        return ResponseEntity.ok(labBatchService.search(status, labTestTypeUid, pageable));
    }

    @GetMapping("/uid/{batchUid}")
    public ResponseEntity<LabBatchDto> findByUid(@PathVariable String batchUid) {
        return ResponseEntity.ok(labBatchService.findByUid(batchUid));
    }

    @PostMapping("/uid/{batchUid}/orders")
    public ResponseEntity<LabBatchDto> addOrder(@PathVariable String batchUid,
                                                @Valid @RequestBody AddOrderRequest request) {
        return ResponseEntity.ok(labBatchService.addOrder(batchUid, request.orderUid()));
    }

    @DeleteMapping("/uid/{batchUid}/orders/uid/{orderUid}")
    public ResponseEntity<LabBatchDto> removeOrder(@PathVariable String batchUid,
                                                   @PathVariable String orderUid) {
        return ResponseEntity.ok(labBatchService.removeOrder(batchUid, orderUid));
    }

    @PostMapping("/uid/{batchUid}/process")
    public ResponseEntity<LabBatchDto> process(@PathVariable String batchUid) {
        return ResponseEntity.ok(labBatchService.markProcessing(batchUid));
    }

    @PostMapping("/uid/{batchUid}/complete")
    public ResponseEntity<LabBatchDto> complete(@PathVariable String batchUid) {
        return ResponseEntity.ok(labBatchService.markCompleted(batchUid));
    }

    @PostMapping("/uid/{batchUid}/cancel")
    public ResponseEntity<LabBatchDto> cancel(@PathVariable String batchUid,
                                              @Valid @RequestBody(required = false) CancelLabBatchRequest request) {
        return ResponseEntity.ok(labBatchService.cancel(batchUid, request));
    }
}
