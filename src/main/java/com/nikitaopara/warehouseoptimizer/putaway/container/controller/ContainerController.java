package com.nikitaopara.warehouseoptimizer.putaway.container.controller;

import com.nikitaopara.warehouseoptimizer.putaway.container.dto.ContainerResponse;
import com.nikitaopara.warehouseoptimizer.putaway.container.dto.ReceiveContainerRequest;
import com.nikitaopara.warehouseoptimizer.putaway.container.serivce.ContainerService;
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
}