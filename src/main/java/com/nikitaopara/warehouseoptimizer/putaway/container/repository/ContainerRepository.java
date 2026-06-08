package com.nikitaopara.warehouseoptimizer.putaway.container.repository;

import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContainerRepository extends JpaRepository<Container, Long> {
}
