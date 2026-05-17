package com.otapp.hmis.engine.encounter.diagnosis.application;

import com.otapp.hmis.engine.encounter.diagnosis.domain.DiagnosisKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ConsultationDiagnosisDtos {

    private ConsultationDiagnosisDtos() {}

    public record ConsultationDiagnosisDto(
            String uid,
            String consultationUid,
            DiagnosisKind kind,
            String diagnosisTypeUid,
            String diagnosisCode,
            String diagnosisName,
            boolean primaryDiagnosis,
            String notes,
            Instant createdAt,
            Instant updatedAt) {}

    public record AddDiagnosisRequest(
            @NotNull DiagnosisKind kind,
            @NotBlank @Size(min = 26, max = 26) String diagnosisTypeUid,
            boolean primaryDiagnosis,
            @Size(max = 1000) String notes) {}
}
