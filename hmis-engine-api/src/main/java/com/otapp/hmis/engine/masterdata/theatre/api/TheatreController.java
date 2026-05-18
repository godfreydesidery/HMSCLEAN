package com.otapp.hmis.engine.masterdata.theatre.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.theatre.application.TheatreDtos.CreateTheatreRequest;
import com.otapp.hmis.engine.masterdata.theatre.application.TheatreDtos.TheatreDto;
import com.otapp.hmis.engine.masterdata.theatre.application.TheatreDtos.UpdateTheatreRequest;
import com.otapp.hmis.engine.masterdata.theatre.application.TheatreService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Theatres")
@RestController
@RequestMapping("/masterdata/theatres")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class TheatreController {

    private final TheatreService service;

    @PostMapping
    public ResponseEntity<TheatreDto> create(@Valid @RequestBody CreateTheatreRequest request) {
        TheatreDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/theatres/uid/{theatreUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<TheatreDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, active, pageable));
    }

    @GetMapping("/uid/{theatreUid}")
    public ResponseEntity<TheatreDto> findByUid(@PathVariable String theatreUid) {
        return ResponseEntity.ok(service.findByUid(theatreUid));
    }

    @PutMapping("/uid/{theatreUid}")
    public ResponseEntity<TheatreDto> update(@PathVariable String theatreUid,
                                             @Valid @RequestBody UpdateTheatreRequest request) {
        return ResponseEntity.ok(service.update(theatreUid, request));
    }

    @PutMapping("/uid/{theatreUid}/active")
    public ResponseEntity<TheatreDto> setActive(@PathVariable String theatreUid,
                                                @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(theatreUid, request.active()));
    }

    @DeleteMapping("/uid/{theatreUid}")
    public ResponseEntity<Void> delete(@PathVariable String theatreUid) {
        service.delete(theatreUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
