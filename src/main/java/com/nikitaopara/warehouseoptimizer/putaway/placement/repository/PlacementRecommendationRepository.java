package com.nikitaopara.warehouseoptimizer.putaway.placement.repository;

import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementRecommendation;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementRecommendationStatus;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementRecommendationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PlacementRecommendationRepository extends JpaRepository<PlacementRecommendation, Long> {

    Optional<PlacementRecommendation> findByCode(String code);

    Optional<PlacementRecommendation> findFirstBySourceContainerIdAndStatusOrderByCreatedAtDesc(
            Long sourceContainerId,
            PlacementRecommendationStatus status
    );

    List<PlacementRecommendation> findBySourceContainerIdOrderByCreatedAtDesc(Long sourceContainerId);

    @Query("""
            select pr.recommendedStoragePlace.id
            from PlacementRecommendation pr
            where pr.warehouse.id = :warehouseId
              and pr.status = :status
              and pr.recommendedStoragePlace is not null
            """)
    Set<Long> findRecommendedStoragePlaceIdsByWarehouseIdAndStatus(
            Long warehouseId,
            PlacementRecommendationStatus status
    );

    @Query("""
            select pr.targetContainer.id
            from PlacementRecommendation pr
            where pr.warehouse.id = :warehouseId
              and pr.status = :status
              and pr.recommendationType = :recommendationType
              and pr.targetContainer is not null
            """)
    Set<Long> findTargetContainerIdsByWarehouseIdAndStatusAndRecommendationType(
            Long warehouseId,
            PlacementRecommendationStatus status,
            PlacementRecommendationType recommendationType
    );

    @Query("""
            select pr.recommendedStoragePlace.id
            from PlacementRecommendation pr
            where pr.sourceContainer.id = :sourceContainerId
              and pr.recommendedStoragePlace is not null
            """)
    Set<Long> findPreviouslyRecommendedStoragePlaceIdsBySourceContainerId(Long sourceContainerId);

    @Query("""
            select pr.targetContainer.id
            from PlacementRecommendation pr
            where pr.sourceContainer.id = :sourceContainerId
              and pr.targetContainer is not null
            """)
    Set<Long> findPreviouslyRecommendedTargetContainerIdsBySourceContainerId(Long sourceContainerId);
}