package com.nikitaopara.warehouseoptimizer.putaway.article.repository;

import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findByArticleNumber(String articleNumber);

    boolean existsByArticleNumber(String articleNumber);

    List<Article> findByArticleNumberIn(Set<String> articleNumbers);

    List<Article> findByArticleNumberIn(Collection<String> articleNumbers);
}