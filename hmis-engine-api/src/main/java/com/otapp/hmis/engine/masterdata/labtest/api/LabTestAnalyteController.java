package com.otapp.hmis.engine.masterdata.labtest.api;

import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.CreateAnalyteRequest;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.CreateRangeRequest;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.LabReferenceRangeDto;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.LabTestAnalyteDto;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.UpdateAnalyteRequest;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteDtos.UpdateRangeRequest;
import com.otapp.hmis.engine.masterdata.labtest.application.LabTestAnalyteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Admin CRUD for a lab test type's analytes and their sex/age-banded reference
 * ranges. Result entry consumes these via the encounter module's result
 * template/save endpoints, so this surface stays MASTERDATA_MANAGE-gated.
 */
@Tag(name = "Lab test analytes")
@RestController
@RequestMapping("/masterdata/lab-tests")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class LabTestAnalyteController {

    private final LabTestAnalyteService service;

    // ----- analytes (nested under a lab test type) --------------------------

    @GetMapping("/uid/{labTestUid}/analytes")
    public ResponseEntity<List<LabTestAnalyteDto>> listAnalytes(@PathVariable String labTestUid) {
        return ResponseEntity.ok(service.listForType(labTestUid));
    }

    @PostMapping("/uid/{labTestUid}/analytes")
    public ResponseEntity<LabTestAnalyteDto> createAnalyte(@PathVariable String labTestUid,
                                                           @Valid @RequestBody CreateAnalyteRequest request) {
        LabTestAnalyteDto created = service.createAnalyte(labTestUid, request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/lab-tests/analytes/uid/{analyteUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping("/analytes/uid/{analyteUid}")
    public ResponseEntity<LabTestAnalyteDto> findAnalyte(@PathVariable String analyteUid) {
        return ResponseEntity.ok(service.findAnalyte(analyteUid));
    }

    @PutMapping("/analytes/uid/{analyteUid}")
    public ResponseEntity<LabTestAnalyteDto> updateAnalyte(@PathVariable String analyteUid,
                                                           @Valid @RequestBody UpdateAnalyteRequest request) {
        return ResponseEntity.ok(service.updateAnalyte(analyteUid, request));
    }

    @DeleteMapping("/analytes/uid/{analyteUid}")
    public ResponseEntity<Void> deleteAnalyte(@PathVariable String analyteUid) {
        service.deleteAnalyte(analyteUid);
        return ResponseEntity.noContent().build();
    }

    // ----- reference ranges (nested under an analyte) -----------------------

    @GetMapping("/analytes/uid/{analyteUid}/ranges")
    public ResponseEntity<List<LabReferenceRangeDto>> listRanges(@PathVariable String analyteUid) {
        return ResponseEntity.ok(service.listRanges(analyteUid));
    }

    @PostMapping("/analytes/uid/{analyteUid}/ranges")
    public ResponseEntity<LabReferenceRangeDto> addRange(@PathVariable String analyteUid,
                                                         @Valid @RequestBody CreateRangeRequest request) {
        LabReferenceRangeDto created = service.addRange(analyteUid, request);
        // Location points at the analyte's ranges collection (GET-resolvable);
        // there is no single-range GET endpoint.
        URI loc = UriComponentsBuilder.fromPath("/masterdata/lab-tests/analytes/uid/{analyteUid}/ranges")
                .buildAndExpand(analyteUid).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @PutMapping("/analyte-ranges/uid/{rangeUid}")
    public ResponseEntity<LabReferenceRangeDto> updateRange(@PathVariable String rangeUid,
                                                            @Valid @RequestBody UpdateRangeRequest request) {
        return ResponseEntity.ok(service.updateRange(rangeUid, request));
    }

    @DeleteMapping("/analyte-ranges/uid/{rangeUid}")
    public ResponseEntity<Void> deleteRange(@PathVariable String rangeUid) {
        service.deleteRange(rangeUid);
        return ResponseEntity.noContent().build();
    }
}
