package com.nikitaopara.warehouseoptimizer.putaway.container.service;

import com.nikitaopara.warehouseoptimizer.common.error.ResourceNotFoundException;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContainerDataService {

    private final ContainerRepository containerRepository;
    private final WarehouseRepository warehouseRepository;
    private final StoragePlaceRepository storagePlaceRepository;

    public Container save(Container container) {
        return containerRepository.save(container);
    }

    public Container getByContainerNumberOrThrow(String containerNumber) {
        return containerRepository.findByContainerNumber(containerNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Container not found by number: " + containerNumber));
    }

    public List<Container> getAll() {
        return containerRepository.findAll(Sort.by(Sort.Direction.ASC, "containerNumber"));
    }

    public boolean existsByContainerNumber(String containerNumber) {
        return containerRepository.existsByContainerNumber(containerNumber);
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
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found by id: " + warehouseId));
    }

    public StoragePlace getStoragePlaceByWarehouseAndCodeOrThrow(Long warehouseId, String storagePlaceCode) {
        return storagePlaceRepository.findStoragePlaceByWarehouseIdAndCode(warehouseId, storagePlaceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Storage place not found by code: " + storagePlaceCode));
    }

    public List<Container> saveOnlyNew(List<Container> containers) {
        if (containers == null || containers.isEmpty()) {
            return List.of();
        }

        Map<String, Container> uniqueContainersByNumber = containers.stream()
                .collect(Collectors.toMap(
                        container -> container.getContainerNumber().trim(),
                        container -> container,
                        (firstContainer, duplicateContainer) -> firstContainer,
                        LinkedHashMap::new
                ));

        Set<String> containerNumbers = uniqueContainersByNumber.keySet();

        Set<String> existingContainerNumbers = containerRepository
                .findByContainerNumberIn(containerNumbers)
                .stream()
                .map(Container::getContainerNumber)
                .collect(Collectors.toSet());

        List<Container> containersToSave = uniqueContainersByNumber.entrySet()
                .stream()
                .filter(entry -> !existingContainerNumbers.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        if (containersToSave.isEmpty()) {
            return List.of();
        }

        return containerRepository.saveAll(containersToSave);
    }
}
