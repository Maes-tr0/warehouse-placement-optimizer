package com.nikitaopara.warehouseoptimizer.putaway.article.repository;

import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
}
