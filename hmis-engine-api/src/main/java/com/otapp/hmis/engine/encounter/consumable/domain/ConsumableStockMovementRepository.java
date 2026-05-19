package com.otapp.hmis.engine.encounter.consumable.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumableStockMovementRepository extends JpaRepository<ConsumableStockMovement, Long> {

    List<ConsumableStockMovement> findAllBySourceKindAndSourceUidAndConsumableUidOrderByOccurredAtDesc(
            ConsumableSourceKind sourceKind, String sourceUid, String consumableUid);
}
