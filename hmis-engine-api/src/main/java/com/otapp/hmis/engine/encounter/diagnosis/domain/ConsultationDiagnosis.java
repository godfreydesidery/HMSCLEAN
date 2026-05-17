package com.otapp.hmis.engine.encounter.diagnosis.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A diagnosis (working or final) recorded against a consultation. A
 * consultation typically has several working diagnoses while the clinician
 * is reasoning through the case, and one or more final diagnoses by closure.
 */
@Entity
@Table(name = "consultation_diagnosis",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_consultation_diagnosis_entry",
               columnNames = {"consultation_uid", "kind", "diagnosis_type_uid"}),
       indexes = {
               @Index(name = "idx_consultation_diagnosis_consultation", columnList = "consultation_uid"),
               @Index(name = "idx_consultation_diagnosis_kind",         columnList = "kind")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultationDiagnosis extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consultation_uid",   nullable = false, length = 26) private String consultationUid;
    @Column(name = "diagnosis_type_uid", nullable = false, length = 26) private String diagnosisTypeUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DiagnosisKind kind;

    @Setter @Column(name = "is_primary", nullable = false) private boolean primaryDiagnosis = false;
    @Setter @Column(name = "notes", length = 1000)        private String notes;

    public ConsultationDiagnosis(String consultationUid, String diagnosisTypeUid,
                                 DiagnosisKind kind, boolean primaryDiagnosis, String notes) {
        this.consultationUid = consultationUid;
        this.diagnosisTypeUid = diagnosisTypeUid;
        this.kind = kind;
        this.primaryDiagnosis = primaryDiagnosis;
        this.notes = notes;
    }
}
