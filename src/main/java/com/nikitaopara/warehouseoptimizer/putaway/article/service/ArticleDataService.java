package com.nikitaopara.warehouseoptimizer.putaway.article.service;

import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.putaway.article.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleDataService {

    private final ArticleRepository articleRepository;

    public Article save(Article article) {
        return articleRepository.save(article);
    }

    public Article getByIdOrThrow(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found by id: " + id));
    }

    public Article getByArticleNumberOrThrow(String articleNumber) {
        return articleRepository.findByArticleNumber(articleNumber)
                .orElseThrow(() -> new IllegalArgumentException("Article not found by article number: " + articleNumber));
    }

    public List<Article> getAll() {
        return articleRepository.findAll(Sort.by(Sort.Direction.ASC, "articleNumber"));
    }

    public boolean existsByArticleNumber(String articleNumber) {
        return articleRepository.existsByArticleNumber(articleNumber);
    }

    public void delete(Article article) {
        articleRepository.delete(article);
    }
}