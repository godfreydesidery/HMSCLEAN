package com.otapp.hmis.engine.encounter.consultation.api;

import com.otapp.hmis.engine.common.api.PageResponse;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationDtos.ConsultationDto;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationService;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationTransferDtos.AcceptTransferRequest;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationTransferDtos.CancelTransferRequest;
import com.otapp.hmis.engine.encounter.consultation.application.ConsultationTransferDtos.ConsultationTransferDto;
import com.otapp.hmis.engine.encounter.consultation.domain.ConsultationTransferStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consultation-transfer receiving side (OPC-1). Reception works the pending
 * queue and either accepts a transfer (booking the receiving consultation) or
 * the initiating doctor reverts it.
 */
@Tag(name = "Consultation transfers")
@RestController
@RequestMapping("/encounters/consultations/transfers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENCOUNTER_ACCESS')")
public class ConsultationTransferController {

    private final ConsultationService consultationService;

    /** The receiving queue — pending transfers (default), newest first. */
    @GetMapping
    public ResponseEntity<PageResponse<ConsultationTransferDto>> queue(
            @RequestParam(required = false, defaultValue = "PENDING") ConsultationTransferStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(consultationService.transferQueue(status, pageable));
    }

    /** Reception accepts a pending transfer and books the receiving consultation. */
    @PostMapping("/uid/{transferUid}/accept")
    public ResponseEntity<ConsultationDto> accept(@PathVariable String transferUid,
                                                  @Valid @RequestBody AcceptTransferRequest request) {
        return ResponseEntity.ok(consultationService.acceptTransfer(transferUid, request));
    }

    /** The initiating doctor reverts a pending transfer; the source returns to IN_PROGRESS. */
    @PostMapping("/uid/{transferUid}/cancel")
    public ResponseEntity<ConsultationDto> cancel(@PathVariable String transferUid,
                                                  @Valid @RequestBody(required = false) CancelTransferRequest request) {
        return ResponseEntity.ok(consultationService.cancelTransfer(transferUid, request));
    }
}
