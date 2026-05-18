package com.otapp.hmis.engine.encounter.operative.api;

import com.otapp.hmis.engine.encounter.operative.application.OperativeRecordDtos.OperativeRecordDto;
import com.otapp.hmis.engine.encounter.operative.application.OperativeRecordDtos.UpsertOperativeRecordRequest;
import com.otapp.hmis.engine.encounter.operative.application.OperativeRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Operative record")
@RestController
@RequestMapping("/encounters/orders/uid/{orderUid}/operative-record")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class OperativeRecordController {

    private final OperativeRecordService service;

    @GetMapping
    public ResponseEntity<OperativeRecordDto> findByOrder(@PathVariable String orderUid) {
        return ResponseEntity.ok(service.findByOrder(orderUid));
    }

    @PutMapping
    public ResponseEntity<OperativeRecordDto> upsert(@PathVariable String orderUid,
                                                     @Valid @RequestBody UpsertOperativeRecordRequest request) {
        return ResponseEntity.ok(service.upsert(orderUid, request));
    }

    @PostMapping("/lock")
    public ResponseEntity<OperativeRecordDto> lock(@PathVariable String orderUid) {
        return ResponseEntity.ok(service.lock(orderUid));
    }
}
