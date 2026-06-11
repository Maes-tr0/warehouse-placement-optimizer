package com.nikitaopara.warehouseoptimizer.eventing.model;

public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
