package com.nikitaopara.warehouseoptimizer.putaway.container.repository;

import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContainerRepository extends JpaRepository<Container, Long> {

    boolean existsByArticleId(Long articleId);

    Optional<Container> findByArticleId(Long articleId);

}
