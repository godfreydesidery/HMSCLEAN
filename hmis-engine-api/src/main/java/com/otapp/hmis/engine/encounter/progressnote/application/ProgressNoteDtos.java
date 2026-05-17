package com.otapp.hmis.engine.encounter.progressnote.application;

import com.otapp.hmis.engine.encounter.progressnote.domain.ProgressNoteKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ProgressNoteDtos {

    private ProgressNoteDtos() {}

    public record ProgressNoteDto(
            String uid,
            String admissionUid,
            ProgressNoteKind kind,
            String authorUsername,
            String authorName,
            Instant recordedAt,
            String body,
            boolean deleted,
            Instant deletedAt,
            String deletedBy,
            String deletedReason) {}

    public record CreateProgressNoteRequest(
            @NotNull ProgressNoteKind kind,
            @NotBlank @Size(max = 8000) String body) {}

    public record DeleteProgressNoteRequest(@Size(max = 255) String reason) {}
}
