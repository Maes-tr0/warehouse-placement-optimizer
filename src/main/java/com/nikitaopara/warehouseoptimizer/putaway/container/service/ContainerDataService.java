package com.nikitaopara.warehouseoptimizer.putaway.container.service;

import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.ContainerStatus;
import com.nikitaopara.warehouseoptimizer.putaway.container.repository.ContainerRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.StoragePlaceRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContainerDataService {

    private final ContainerRepository containerRepository;
    private final WarehouseRepository warehouseRepository;
    private final StoragePlaceRepository storagePlaceRepository;

    public Container save(Container container) {
        return containerRepository.save(container);
    }

    public Container getByIdOrThrow(Long id) {
        return containerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Container not found by id: " + id));
    }

    public Container getByContainerNumberOrThrow(String containerNumber) {
        return containerRepository.findByContainerNumber(containerNumber)
                .orElseThrow(() -> new IllegalArgumentException("Container not found by number: " + containerNumber));
    }

    public List<Container> getAll() {
        return containerRepository.findAll(Sort.by(Sort.Direction.ASC, "containerNumber"));
    }

    public boolean existsByContainerNumber(String containerNumber) {
        return containerRepository.existsByContainerNumber(containerNumber);
    }

    public boolean existsByArticleId(Long articleId) {
        return containerRepository.existsByArticleId(articleId);
    }

    public List<Container> getStoredContainersByWarehouseAndArticle(Long warehouseId, Long articleId) {
        return containerRepository.findByWarehouseIdAndArticleIdAndStatus(
                warehouseId,
                articleId,
                ContainerStatus.STORED
        );
    }

    public Warehouse getWarehouseByIdOrThrow(Long warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found by id: " + warehouseId));
    }

    public StoragePlace getStoragePlaceByWarehouseAndCodeOrThrow(Long warehouseId, String storagePlaceCode) {
        return storagePlaceRepository.findStoragePlaceByWarehouseIdAndCode(warehouseId, storagePlaceCode)
                .orElseThrow(() -> new IllegalArgumentException("Storage place not found by code: " + storagePlaceCode));
    }
}