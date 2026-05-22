package com.otapp.hmis.engine.encounter.operative.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class OperativeRecordDtos {

    private OperativeRecordDtos() {}

    public record OperativeRecordDto(
            String uid,
            String orderUid,
            String findings,
            String technique,
            String instruments,
            String complications,
            String specimens,
            String surgeonUsername,
            String assistants,
            String anaesthetistUsername,
            String anaesthesiaType,
            String scrubNurse,
            String circulatingNurse,
            Instant startedAt,
            Instant endedAt,
            String authoredByUsername,
            Instant authoredAt,
            Instant lockedAt,
            String lockedByUsername,
            Instant createdAt,
            Instant updatedAt) {}

    /**
     * Create or update the editable fields. Same shape used for both
     * because each field is optional; the service decides whether to
     * create-or-update based on whether a record already exists.
     */
    public record UpsertOperativeRecordRequest(
            @Size(max = 4000) String findings,
            @Size(max = 4000) String technique,
            @Size(max = 2000) String instruments,
            @Size(max = 2000) String complications,
            @Size(max = 2000) String specimens,
            @Size(max = 64)   String surgeonUsername,
            @Size(max = 500)  String assistants,
            @Size(max = 64)   String anaesthetistUsername,
            @Size(max = 64)   String anaesthesiaType,
            @Size(max = 120)  String scrubNurse,
            @Size(max = 120)  String circulatingNurse,
            Instant startedAt,
            Instant endedAt) {}

    /** Append-only addendum to a locked operative record. Reason required. */
    public record CreateAmendmentRequest(
            @NotBlank @Size(max = 1000) String reason,
            @Size(max = 4000) String findings,
            @Size(max = 4000) String technique,
            @Size(max = 2000) String instruments,
            @Size(max = 2000) String complications,
            @Size(max = 2000) String specimens,
            @Size(max = 500)  String assistants,
            @Size(max = 64)   String anaesthesiaType,
            @Size(max = 120)  String scrubNurse,
            @Size(max = 120)  String circulatingNurse) {}

    public record AmendmentDto(
            String uid,
            String operativeRecordUid,
            int amendmentNo,
            String reason,
            String findings,
            String technique,
            String instruments,
            String complications,
            String specimens,
            String assistants,
            String anaesthesiaType,
            String scrubNurse,
            String circulatingNurse,
            String authoredByUsername,
            Instant authoredAt) {}
}
