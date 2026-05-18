package com.otapp.hmis.engine.masterdata.medicine.api;

import com.otapp.hmis.engine.masterdata.medicine.application.MedicineDtos.CreateMedicineUnitRequest;
import com.otapp.hmis.engine.masterdata.medicine.application.MedicineDtos.MedicineUnitDto;
import com.otapp.hmis.engine.masterdata.medicine.application.MedicineDtos.UpdateMedicineUnitRequest;
import com.otapp.hmis.engine.masterdata.medicine.application.MedicineUnitService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Medicine units")
@RestController
@RequestMapping("/masterdata/medicines/uid/{medicineUid}/units")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA_MANAGE')")
public class MedicineUnitController {

    private final MedicineUnitService service;

    @GetMapping
    public ResponseEntity<List<MedicineUnitDto>> list(@PathVariable String medicineUid) {
        return ResponseEntity.ok(service.list(medicineUid));
    }

    @PostMapping
    public ResponseEntity<MedicineUnitDto> create(@PathVariable String medicineUid,
                                                  @Valid @RequestBody CreateMedicineUnitRequest request) {
        MedicineUnitDto created = service.create(medicineUid, request);
        URI loc = UriComponentsBuilder.fromPath("/masterdata/medicines/uid/{medicineUid}/units/uid/{unitUid}")
                .buildAndExpand(medicineUid, created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @PutMapping("/uid/{unitUid}")
    public ResponseEntity<MedicineUnitDto> update(@PathVariable String medicineUid,
                                                  @PathVariable String unitUid,
                                                  @Valid @RequestBody UpdateMedicineUnitRequest request) {
        return ResponseEntity.ok(service.update(medicineUid, unitUid, request));
    }

    @PutMapping("/uid/{unitUid}/active")
    public ResponseEntity<MedicineUnitDto> setActive(@PathVariable String medicineUid,
                                                     @PathVariable String unitUid,
                                                     @RequestBody ActiveRequest request) {
        return ResponseEntity.ok(service.setActive(medicineUid, unitUid, request.active()));
    }

    @DeleteMapping("/uid/{unitUid}")
    public ResponseEntity<Void> delete(@PathVariable String medicineUid,
                                       @PathVariable String unitUid) {
        service.delete(medicineUid, unitUid);
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {}
}
