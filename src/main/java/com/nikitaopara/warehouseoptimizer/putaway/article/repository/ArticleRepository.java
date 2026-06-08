package com.nikitaopara.warehouseoptimizer.putaway.article.repository;

import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findByArticleNumber(String articleNumber);

    boolean existsByArticleNumber(String articleNumber);
}