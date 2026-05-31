package com.otapp.hmis.engine.encounter.note.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A clinician's narrative captured during a consultation. A consultation has
 * at most one note (it can be edited until the consultation is closed).
 */
@Entity
@Table(name = "clinical_note",
       uniqueConstraints = @UniqueConstraint(name = "uk_clinical_note_consultation", columnNames = "consultation_uid"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicalNote extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consultation_uid", nullable = false, length = 26)
    private String consultationUid;

    @Setter @Column(name = "chief_complaint",                length = 1000) private String chiefComplaint;
    @Setter @Column(name = "history_of_presenting_illness",  length = 4000) private String historyOfPresentingIllness;
    @Setter @Column(name = "past_medical_history",           length = 4000) private String pastMedicalHistory;
    @Setter @Column(name = "examination",                    length = 4000) private String examination;
    @Setter @Column(name = "assessment",                     length = 4000) private String assessment;
    @Setter @Column(name = "plan",                           length = 4000) private String plan;

    // ----- clinical-note-safety narrative fields (legacy ClinicalNote) ------
    // drugsAndAllergyHistory is the allergy-safety field surfaced prominently
    // to the prescriber before prescribing.
    @Setter @Column(name = "drugs_and_allergy_history",      length = 4000) private String drugsAndAllergyHistory;
    @Setter @Column(name = "family_and_social_history",      length = 4000) private String familyAndSocialHistory;
    @Setter @Column(name = "review_of_other_systems",        length = 4000) private String reviewOfOtherSystems;

    public ClinicalNote(String consultationUid) {
        this.consultationUid = consultationUid;
    }
}
