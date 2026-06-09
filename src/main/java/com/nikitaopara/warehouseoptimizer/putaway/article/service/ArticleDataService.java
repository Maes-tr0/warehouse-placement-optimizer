package com.nikitaopara.warehouseoptimizer.putaway.article.service;

import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.putaway.article.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public List<Article> saveAll(List<Article> articles) {
        return articleRepository.saveAll(articles);
    }

    public Set<String> getExistingArticleNumbers(Set<String> articleNumbers) {
        if (articleNumbers == null || articleNumbers.isEmpty()) {
            return Set.of();
        }

        return articleRepository.findByArticleNumberIn(articleNumbers)
                .stream()
                .map(Article::getArticleNumber)
                .collect(Collectors.toSet());
    }
}