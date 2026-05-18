package com.otapp.hmis.engine.masterdata.medicine.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicineUnitRepository extends JpaRepository<MedicineUnit, Long> {

    Optional<MedicineUnit> findByUid(String uid);

    Optional<MedicineUnit> findByMedicineUidAndCode(String medicineUid, String code);

    Optional<MedicineUnit> findByMedicineUidAndBaseTrue(String medicineUid);

    List<MedicineUnit> findAllByMedicineUidOrderByBaseDescCodeAsc(String medicineUid);

    boolean existsByMedicineUidAndCode(String medicineUid, String code);
}
