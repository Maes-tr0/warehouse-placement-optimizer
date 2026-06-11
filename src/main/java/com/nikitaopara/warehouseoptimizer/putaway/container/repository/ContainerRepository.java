package com.nikitaopara.warehouseoptimizer.putaway.container.repository;

import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.ContainerStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContainerRepository extends JpaRepository<Container, Long> {

    @EntityGraph(attributePaths = {"article", "warehouse", "currentStoragePlace", "mergedIntoContainer"})
    Optional<Container> findByContainerNumber(String containerNumber);

    boolean existsByContainerNumber(String containerNumber);

    boolean existsByArticleId(Long articleId);

    List<Container> findByWarehouseIdAndArticleIdAndStatus(
            Long warehouseId,
            Long articleId,
            ContainerStatus status
    );

    @EntityGraph(attributePaths = {"article", "currentStoragePlace"})
    List<Container> findByWarehouseIdAndStatus(Long warehouseId, ContainerStatus status);

    List<Container> findByContainerNumberIn(Collection<String> containerNumbers);
}
