package com.otapp.hmis.engine.masterdata.dosingfrequency.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.dosingfrequency.application.DosingFrequencyDtos.CreateDosingFrequencyRequest;
import com.otapp.hmis.engine.masterdata.dosingfrequency.application.DosingFrequencyDtos.DosingFrequencyDto;
import com.otapp.hmis.engine.masterdata.dosingfrequency.application.DosingFrequencyDtos.UpdateDosingFrequencyRequest;
import com.otapp.hmis.engine.masterdata.dosingfrequency.application.DosingFrequencyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Dosing frequencies")
@RestController
@RequestMapping("/masterdata/dosing-frequencies")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class DosingFrequencyController {

    private final DosingFrequencyService service;

    @PostMapping
    public ResponseEntity<DosingFrequencyDto> create(@Valid @RequestBody CreateDosingFrequencyRequest request) {
        DosingFrequencyDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/dosing-frequencies/uid/{frequencyUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<DosingFrequencyDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, active, pageable));
    }

    @GetMapping("/uid/{frequencyUid}")
    public ResponseEntity<DosingFrequencyDto> findByUid(@PathVariable String frequencyUid) {
        return ResponseEntity.ok(service.findByUid(frequencyUid));
    }

    @PutMapping("/uid/{frequencyUid}")
    public ResponseEntity<DosingFrequencyDto> update(@PathVariable String frequencyUid,
                                                     @Valid @RequestBody UpdateDosingFrequencyRequest request) {
        return ResponseEntity.ok(service.update(frequencyUid, request));
    }

    @PutMapping("/uid/{frequencyUid}/active")
    public ResponseEntity<DosingFrequencyDto> setActive(@PathVariable String frequencyUid,
                                                        @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(frequencyUid, request.active()));
    }

    @DeleteMapping("/uid/{frequencyUid}")
    public ResponseEntity<Void> delete(@PathVariable String frequencyUid) {
        service.delete(frequencyUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
