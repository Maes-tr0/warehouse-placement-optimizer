package com.nikitaopara.warehouseoptimizer.putaway.container.controller;

import com.nikitaopara.warehouseoptimizer.putaway.container.dto.ContainerResponse;
import com.nikitaopara.warehouseoptimizer.putaway.container.dto.MergeContainerRequest;
import com.nikitaopara.warehouseoptimizer.putaway.container.dto.PlaceContainerRequest;
import com.nikitaopara.warehouseoptimizer.putaway.container.dto.ReceiveContainerRequest;
import com.nikitaopara.warehouseoptimizer.putaway.container.dto.UpdateContainerRequest;
import com.nikitaopara.warehouseoptimizer.putaway.container.service.ContainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operator/containers")
@RequiredArgsConstructor
public class ContainerController {

    private final ContainerService containerService;

    @PostMapping("/receive")
    public ResponseEntity<ContainerResponse> receiveContainer(@RequestBody ReceiveContainerRequest request) {
        ContainerResponse response = containerService.receiveContainer(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{containerNumber}")
    public ResponseEntity<ContainerResponse> getContainerByNumber(@PathVariable String containerNumber) {
        ContainerResponse response = containerService.getContainerByNumber(containerNumber);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getContainers() {
        return ResponseEntity.ok(containerService.getContainers());
    }

    @PatchMapping("/{containerNumber}")
    public ResponseEntity<ContainerResponse> updateContainer(
            @PathVariable String containerNumber,
            @RequestBody UpdateContainerRequest request
    ) {
        ContainerResponse response = containerService.updateContainer(containerNumber, request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{containerNumber}/place")
    public ResponseEntity<ContainerResponse> placeContainer(
            @PathVariable String containerNumber,
            @RequestBody PlaceContainerRequest request
    ) {
        ContainerResponse response = containerService.placeContainer(containerNumber, request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{containerNumber}/merge")
    public ResponseEntity<ContainerResponse> mergeContainer(
            @PathVariable String containerNumber,
            @RequestBody MergeContainerRequest request
    ) {
        ContainerResponse response = containerService.mergeContainer(containerNumber, request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{containerNumber}/remove")
    public ResponseEntity<ContainerResponse> removeContainer(@PathVariable String containerNumber) {
        ContainerResponse response = containerService.removeContainer(containerNumber);

        return ResponseEntity.ok(response);
    }
}