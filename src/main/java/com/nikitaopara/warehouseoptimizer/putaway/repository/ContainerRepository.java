package com.nikitaopara.warehouseoptimizer.putaway.repository;

import com.nikitaopara.warehouseoptimizer.putaway.model.Container;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContainerRepository extends JpaRepository<Container, Long> {
}
