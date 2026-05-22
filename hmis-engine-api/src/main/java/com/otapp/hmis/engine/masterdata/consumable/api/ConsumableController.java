package com.otapp.hmis.engine.masterdata.consumable.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.consumable.application.ConsumableDtos.ConsumableDto;
import com.otapp.hmis.engine.masterdata.consumable.application.ConsumableDtos.CreateConsumableRequest;
import com.otapp.hmis.engine.masterdata.consumable.application.ConsumableDtos.UpdateConsumableRequest;
import com.otapp.hmis.engine.masterdata.consumable.application.ConsumableService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Consumables")
@RestController
@RequestMapping("/masterdata/consumables")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class ConsumableController {

    private final ConsumableService service;

    @PostMapping
    public ResponseEntity<ConsumableDto> create(@Valid @RequestBody CreateConsumableRequest request) {
        ConsumableDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/consumables/uid/{consumableUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ConsumableDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, active, pageable));
    }

    @GetMapping("/uid/{consumableUid}")
    public ResponseEntity<ConsumableDto> findByUid(@PathVariable String consumableUid) {
        return ResponseEntity.ok(service.findByUid(consumableUid));
    }

    @PutMapping("/uid/{consumableUid}")
    public ResponseEntity<ConsumableDto> update(@PathVariable String consumableUid,
                                                @Valid @RequestBody UpdateConsumableRequest request) {
        return ResponseEntity.ok(service.update(consumableUid, request));
    }

    @PutMapping("/uid/{consumableUid}/active")
    public ResponseEntity<ConsumableDto> setActive(@PathVariable String consumableUid,
                                                   @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(consumableUid, request.active()));
    }

    @DeleteMapping("/uid/{consumableUid}")
    public ResponseEntity<Void> delete(@PathVariable String consumableUid) {
        service.delete(consumableUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
