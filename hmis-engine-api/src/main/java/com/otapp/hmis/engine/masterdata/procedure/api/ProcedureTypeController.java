package com.otapp.hmis.engine.masterdata.procedure.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.procedure.application.ProcedureTypeDtos.CreateProcedureTypeRequest;
import com.otapp.hmis.engine.masterdata.procedure.application.ProcedureTypeDtos.ProcedureTypeDto;
import com.otapp.hmis.engine.masterdata.procedure.application.ProcedureTypeDtos.UpdateProcedureTypeRequest;
import com.otapp.hmis.engine.masterdata.procedure.application.ProcedureTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Procedures")
@RestController
@RequestMapping("/masterdata/procedures")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class ProcedureTypeController {

    private final ProcedureTypeService service;

    @PostMapping
    public ResponseEntity<ProcedureTypeDto> create(@Valid @RequestBody CreateProcedureTypeRequest request) {
        ProcedureTypeDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/procedures/uid/{procedureUid}").buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProcedureTypeDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, active, pageable));
    }

    @GetMapping("/uid/{procedureUid}")
    public ResponseEntity<ProcedureTypeDto> findByUid(@PathVariable String procedureUid) {
        return ResponseEntity.ok(service.findByUid(procedureUid));
    }

    @PutMapping("/uid/{procedureUid}")
    public ResponseEntity<ProcedureTypeDto> update(@PathVariable String procedureUid, @Valid @RequestBody UpdateProcedureTypeRequest request) {
        return ResponseEntity.ok(service.update(procedureUid, request));
    }

    @PutMapping("/uid/{procedureUid}/active")
    public ResponseEntity<ProcedureTypeDto> setActive(@PathVariable String procedureUid, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(procedureUid, request.active()));
    }

    @DeleteMapping("/uid/{procedureUid}")
    public ResponseEntity<Void> delete(@PathVariable String procedureUid) {
        service.delete(procedureUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
