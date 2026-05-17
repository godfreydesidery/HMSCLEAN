package com.otapp.hmis.engine.encounter.note.application;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ClinicalNoteDtos {

    private ClinicalNoteDtos() {}

    public record ClinicalNoteDto(
            String uid,
            String consultationUid,
            String chiefComplaint,
            String historyOfPresentingIllness,
            String pastMedicalHistory,
            String examination,
            String assessment,
            String plan,
            Instant createdAt,
            Instant updatedAt) {}

    public record SaveClinicalNoteRequest(
            @Size(max = 1000) String chiefComplaint,
            @Size(max = 4000) String historyOfPresentingIllness,
            @Size(max = 4000) String pastMedicalHistory,
            @Size(max = 4000) String examination,
            @Size(max = 4000) String assessment,
            @Size(max = 4000) String plan) {}
}
