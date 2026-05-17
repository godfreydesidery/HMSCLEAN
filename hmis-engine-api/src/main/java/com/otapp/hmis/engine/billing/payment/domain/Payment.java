package com.otapp.hmis.engine.billing.payment.domain;

import com.otapp.hmis.engine.common.persistence.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment", uniqueConstraints = @UniqueConstraint(name = "uk_payment_no", columnNames = "payment_no"),
       indexes = {
               @Index(name = "idx_payment_invoice", columnList = "invoice_uid"),
               @Index(name = "idx_payment_received_at", columnList = "received_at")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_no", nullable = false, length = 32) private String paymentNo;
    @Column(name = "invoice_uid", nullable = false, length = 26) private String invoiceUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentMethod method;

    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;

    @Column(length = 80) private String reference;
    @Column(length = 255) private String note;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    public Payment(String paymentNo, String invoiceUid, PaymentMethod method,
                   BigDecimal amount, String currency, String reference, String note) {
        this.paymentNo = paymentNo;
        this.invoiceUid = invoiceUid;
        this.method = method;
        this.amount = amount;
        this.currency = currency;
        this.reference = reference;
        this.note = note;
        this.receivedAt = Instant.now();
    }
}
