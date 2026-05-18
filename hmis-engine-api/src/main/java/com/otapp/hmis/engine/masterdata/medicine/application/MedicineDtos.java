package com.otapp.hmis.engine.masterdata.medicine.application;

import com.otapp.hmis.engine.masterdata.medicine.domain.MedicineForm;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class MedicineDtos {

    private MedicineDtos() {}

    public record MedicineDto(
            String uid, String code, String name, String genericName, String strength,
            MedicineForm form, String description,
            boolean active, Instant createdAt, Instant updatedAt) {}

    public record CreateMedicineRequest(
            @NotBlank @Size(min = 2, max = 32) @Pattern(regexp = "^[A-Z0-9._-]+$") String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 200) String genericName,
            @Size(max = 80)  String strength,
            @NotNull  MedicineForm form,
            @Size(max = 500) String description) {}

    public record UpdateMedicineRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 200) String genericName,
            @Size(max = 80)  String strength,
            @NotNull  MedicineForm form,
            @Size(max = 500) String description) {}

    // ----- units ------------------------------------------------------------

    public record MedicineUnitDto(
            String uid,
            String medicineUid,
            String code,
            String name,
            int factorToBase,
            boolean base,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {}

    public record CreateMedicineUnitRequest(
            @NotBlank @Size(min = 1, max = 16) @Pattern(regexp = "^[A-Z0-9._-]+$") String code,
            @NotBlank @Size(max = 80) String name,
            @jakarta.validation.constraints.Min(2) int factorToBase) {}

    public record UpdateMedicineUnitRequest(
            @NotBlank @Size(max = 80) String name,
            @jakarta.validation.constraints.Min(1) int factorToBase) {}
}
