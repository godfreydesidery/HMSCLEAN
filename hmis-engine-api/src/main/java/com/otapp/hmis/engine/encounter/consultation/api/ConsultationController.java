package com.otapp.hmis.engine.encounter.consultation.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.CancelConsultationRequest;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.ConsultationDto;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.ConsultationSummary;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.StartConsultationRequest;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.TransferConsultationRequest;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationTransferDtos.ConsultationTransferDto;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationService;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Consultations")
@RestController
@RequestMapping("/encounters/consultations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping
    public ResponseEntity<ConsultationDto> book(@Valid @RequestBody StartConsultationRequest request) {
        ConsultationDto created = consultationService.book(request);
        URI loc = UriComponentsBuilder.fromPath("/encounters/consultations/uid/{consultationUid}")
                .buildAndExpand(created.uid()).toUri();
        return ResponseEntity.created(loc).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ConsultationSummary>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ConsultationStatus status,
            @RequestParam(required = false) String clinicUid,
            @RequestParam(required = false) String patientUid,
            @RequestParam(required = false) String clinicianUsername,
            Pageable pageable) {
        return ResponseEntity.ok(consultationService.search(query, status, clinicUid, patientUid, clinicianUsername, pageable));
    }

    /** The signed-in clinician's "from reception" queue — fee-settled BOOKED consultations. */
    @GetMapping("/reception-queue")
    public ResponseEntity<PageResponse<ConsultationSummary>> receptionQueue(Pageable pageable) {
        return ResponseEntity.ok(consultationService.receptionQueue(pageable));
    }

    /**
     * The signed-in clinician's own open consultations (OPC-4) — their active
     * encounters (BOOKED, IN_PROGRESS or TRANSFERRED), newest first.
     */
    @Operation(summary = "List my open consultations (BOOKED / IN_PROGRESS / TRANSFERRED)")
    @GetMapping("/my-open")
    public ResponseEntity<PageResponse<ConsultationSummary>> myOpenConsultations(Pageable pageable) {
        return ResponseEntity.ok(consultationService.myOpenConsultations(pageable));
    }

    @GetMapping("/uid/{consultationUid}")
    public ResponseEntity<ConsultationDto> findByUid(@PathVariable String consultationUid) {
        return ResponseEntity.ok(consultationService.findByUid(consultationUid));
    }

    @PostMapping("/uid/{consultationUid}/start")
    public ResponseEntity<ConsultationDto> start(@PathVariable String consultationUid) {
        return ResponseEntity.ok(consultationService.start(consultationUid));
    }

    @PostMapping("/uid/{consultationUid}/complete")
    public ResponseEntity<ConsultationDto> complete(@PathVariable String consultationUid) {
        return ResponseEntity.ok(consultationService.complete(consultationUid));
    }

    @PostMapping("/uid/{consultationUid}/cancel")
    public ResponseEntity<ConsultationDto> cancel(@PathVariable String consultationUid,
                                                  @Valid @RequestBody(required = false) CancelConsultationRequest request) {
        return ResponseEntity.ok(consultationService.cancel(consultationUid, request));
    }

    /**
     * Raise a PENDING transfer to another clinic (OPC-1). Returns the created
     * transfer request — reception books the receiving consultation later via the
     * transfer queue.
     */
    @PostMapping("/uid/{consultationUid}/transfer")
    public ResponseEntity<ConsultationTransferDto> transfer(@PathVariable String consultationUid,
                                                            @Valid @RequestBody TransferConsultationRequest request) {
        return ResponseEntity.ok(consultationService.transfer(consultationUid, request));
    }

    @GetMapping("/by-patient/uid/{patientUid}/recent")
    public ResponseEntity<List<ConsultationSummary>> recentForPatient(@PathVariable String patientUid) {
        return ResponseEntity.ok(consultationService.recentForPatient(patientUid));
    }
}
