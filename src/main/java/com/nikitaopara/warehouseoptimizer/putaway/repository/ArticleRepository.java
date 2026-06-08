package com.nikitaopara.warehouseoptimizer.putaway.repository;

import com.nikitaopara.warehouseoptimizer.putaway.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
}
