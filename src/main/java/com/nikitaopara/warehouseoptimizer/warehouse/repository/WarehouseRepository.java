package com.nikitaopara.warehouseoptimizer.warehouse.repository;

import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import com.nikitaopara.warehouseoptimizer.warehouse.model.WarehouseStatus;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.WarehouseSummaryResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    String SUMMARY_SELECT = """
            select new com.nikitaopara.warehouseoptimizer.warehouse.dto.WarehouseSummaryResponse(
                warehouse.id,
                warehouse.code,
                warehouse.name,
                warehouse.layoutType,
                warehouse.status,
                count(distinct aisle.id),
                count(distinct rackRow.id),
                count(distinct rackBay.id),
                count(distinct rackLevel.id),
                count(distinct storagePlace.id),
                warehouse.createdAt,
                warehouse.updatedAt
            )
            from Warehouse warehouse
            left join warehouse.aisles aisle
            left join aisle.rackRows rackRow
            left join rackRow.rackBays rackBay
            left join rackBay.rackLevels rackLevel
            left join rackLevel.storagePlaces storagePlace
            """;

    boolean existsByCode(String code);

    List<Warehouse> findByStatus(WarehouseStatus status);

    @Query(SUMMARY_SELECT + """
            group by warehouse.id, warehouse.code, warehouse.name, warehouse.layoutType,
                     warehouse.status, warehouse.createdAt, warehouse.updatedAt
            order by warehouse.code
            """)
    List<WarehouseSummaryResponse> findAllSummaries();

    @Query(SUMMARY_SELECT + """
            where warehouse.id = :id
            group by warehouse.id, warehouse.code, warehouse.name, warehouse.layoutType,
                     warehouse.status, warehouse.createdAt, warehouse.updatedAt
            """)
    Optional<WarehouseSummaryResponse> findSummaryById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select warehouse from Warehouse warehouse where warehouse.id = :id")
    Optional<Warehouse> findByIdForUpdate(Long id);
}
