package com.otapp.hmis.engine.masterdata.store.domain;

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

/**
 * Affiliation row binding a store keeper (an {@code iam} {@code User} with the
 * {@code STORE_PERSON} role) to a {@link Store}.
 *
 * <p>Restores the legacy {@code StorePerson.stores} many-to-many: a keeper works
 * at one or more stores, and store issue / transfer operations are restricted to
 * a keeper affiliated with the source store. Coupling to {@code iam} is by
 * identity string (canonical {@code user_uid} + denormalized {@code username}) —
 * no cross-module foreign key.
 */
@Entity
@Table(name = "md_store_staff",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_md_store_staff", columnNames = {"store_uid", "user_uid"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreStaff extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_uid", nullable = false, length = 26)
    private String storeUid;

    @Column(name = "user_uid", nullable = false, length = 26)
    private String userUid;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(nullable = false)
    private boolean active = true;

    public StoreStaff(String storeUid, String userUid, String username) {
        this.storeUid = storeUid;
        this.userUid = userUid;
        this.username = username;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
