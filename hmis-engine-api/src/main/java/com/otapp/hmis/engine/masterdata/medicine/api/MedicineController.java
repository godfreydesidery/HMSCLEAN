package com.otapp.hmis.engine.masterdata.medicine.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.masterdata.medicine.application.MedicineDtos.CreateMedicineRequest;
import com.otapp.hmis.engine.masterdata.medicine.application.MedicineDtos.MedicineDto;
import com.otapp.hmis.engine.masterdata.medicine.application.MedicineDtos.UpdateMedicineRequest;
import com.otapp.hmis.engine.masterdata.medicine.application.MedicineService;
import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineForm;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Medicines")
@RestController
@RequestMapping("/masterdata/medicines")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class MedicineController {

    private final MedicineService service;

    @PostMapping
    public ResponseEntity<MedicineDto> create(@Valid @RequestBody CreateMedicineRequest request) {
        MedicineDto created = service.create(request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/medicines/{uid}").buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<MedicineDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) MedicineForm form,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(query, active, form, pageable));
    }

    @GetMapping("/{uid}")
    public ResponseEntity<MedicineDto> findByUid(@PathVariable String uid) {
        return ResponseEntity.ok(service.findByUid(uid));
    }

    @PutMapping("/{uid}")
    public ResponseEntity<MedicineDto> update(@PathVariable String uid, @Valid @RequestBody UpdateMedicineRequest request) {
        return ResponseEntity.ok(service.update(uid, request));
    }

    @PutMapping("/{uid}/active")
    public ResponseEntity<MedicineDto> setActive(@PathVariable String uid, @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(uid, request.active()));
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> delete(@PathVariable String uid) {
        service.delete(uid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
