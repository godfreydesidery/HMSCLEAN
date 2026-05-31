package com.otapp.hmis.engine.procurement.supplier.domain;

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

@Entity
@Table(name = "supplier",
       uniqueConstraints = @UniqueConstraint(name = "uk_supplier_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Supplier extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32) private String code;

    @Setter @Column(nullable = false, length = 200) private String name;
    @Setter @Column(name = "contact_name", length = 120) private String contactName;
    @Setter @Column(length = 32) private String phone;
    @Setter @Column(length = 120) private String email;
    @Setter @Column(length = 255) private String address;
    @Setter @Column(name = "tax_id", length = 64) private String taxId;
    @Setter @Column(length = 500) private String notes;

    // --- VAT, contract terms and bank-account block (legacy Supplier parity) ---
    // These print on the LPO / cheque / remittance documents. All nullable.
    @Setter @Column(length = 32)                            private String vrn;
    @Setter @Column(name = "terms_of_contract", length = 1000) private String termsOfContract;
    @Setter @Column(name = "bank_name", length = 120)       private String bankName;
    @Setter @Column(name = "bank_account_name", length = 200) private String bankAccountName;
    @Setter @Column(name = "bank_account_no", length = 64)  private String bankAccountNo;

    @Setter @Column(nullable = false) private boolean active = true;

    public Supplier(String code, String name, String contactName, String phone, String email,
                    String address, String taxId, String notes) {
        this.code = code;
        this.name = name;
        this.contactName = contactName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.taxId = taxId;
        this.notes = notes;
    }

    public void activate()   { this.active = true; }
    public void deactivate() { this.active = false; }
}
