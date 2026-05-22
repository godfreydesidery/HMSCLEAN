package com.otapp.hmis.engine.patient.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "patient",
       uniqueConstraints = {
               @UniqueConstraint(name = "uk_patient_no", columnNames = "patient_no")
       },
       indexes = {
               @Index(name = "idx_patient_last_name",  columnList = "last_name"),
               @Index(name = "idx_patient_phone_no",   columnList = "phone_no"),
               @Index(name = "idx_patient_national_id",columnList = "national_id")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Patient extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable patient number, e.g. PT-2026-000123. */
    @Column(name = "patient_no", nullable = false, length = 32)
    private String patientNo;

    @Setter @Column(name = "first_name",  nullable = false, length = 80) private String firstName;
    @Setter @Column(name = "middle_name", length = 80)                   private String middleName;
    @Setter @Column(name = "last_name",   nullable = false, length = 80) private String lastName;

    @Setter
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Gender gender;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "patient_type", nullable = false, length = 16)
    private PatientType type = PatientType.OUTPATIENT;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 16)
    private PaymentType paymentType = PaymentType.CASH;

    // Insurance link (loose coupling — masterdata module owns the plan aggregate)
    @Setter @Column(name = "insurance_plan_uid", length = 26)         private String insurancePlanUid;
    @Setter @Column(name = "membership_no",      length = 64)         private String membershipNo;

    // Contact + identifiers
    @Setter @Column(name = "phone_no",     length = 40)  private String phoneNo;
    @Setter @Column(length = 120) private String email;
    @Setter @Column(length = 255) private String address;
    @Setter @Column(length = 80)  private String nationality;
    @Setter @Column(name = "national_id",  length = 64)  private String nationalId;
    @Setter @Column(name = "passport_no",  length = 64)  private String passportNo;

    // Next of kin — legacy supports up to 3 contacts; columns kept on Patient
    // to avoid a child table for what's almost always 0..3 short rows.
    @Setter @Column(name = "kin_full_name",    length = 160) private String kinFullName;
    @Setter @Column(name = "kin_relationship", length = 80)  private String kinRelationship;
    @Setter @Column(name = "kin_phone_no",     length = 40)  private String kinPhoneNo;

    @Setter @Column(name = "kin2_full_name",    length = 160) private String kin2FullName;
    @Setter @Column(name = "kin2_relationship", length = 80)  private String kin2Relationship;
    @Setter @Column(name = "kin2_phone_no",     length = 40)  private String kin2PhoneNo;

    @Setter @Column(name = "kin3_full_name",    length = 160) private String kin3FullName;
    @Setter @Column(name = "kin3_relationship", length = 80)  private String kin3Relationship;
    @Setter @Column(name = "kin3_phone_no",     length = 40)  private String kin3PhoneNo;

    /** Set by ConsultationService.book / AdmissionService.admit so the registry can show recency. */
    @Setter @Column(name = "last_visit_at") private java.time.Instant lastVisitAt;

    @Setter @Column(nullable = false) private boolean active = true;

    public Patient(String patientNo, String firstName, String middleName, String lastName,
                   LocalDate dateOfBirth, Gender gender, PatientType type, PaymentType paymentType) {
        this.patientNo = patientNo;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.type = type;
        this.paymentType = paymentType;
    }

    public String fullName() {
        if (middleName == null || middleName.isBlank()) {
            return firstName + " " + lastName;
        }
        return firstName + " " + middleName + " " + lastName;
    }

    public void activate()   { this.active = true; }
    public void deactivate() { this.active = false; }
}
