package com.nikitaopara.warehouseoptimizer.putaway.article.service;

import com.nikitaopara.warehouseoptimizer.common.error.ResourceNotFoundException;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.putaway.article.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Article not found by id: " + id
                ));
    }

    public Article getByArticleNumberOrThrow(String articleNumber) {
        return articleRepository.findByArticleNumber(articleNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Article not found by article number: " + articleNumber
                ));
    }

    public List<Article> getAll() {
        return articleRepository.findAll(
                Sort.by(Sort.Direction.ASC, "articleNumber")
        );
    }

    public boolean existsByArticleNumber(String articleNumber) {
        return articleRepository.existsByArticleNumber(articleNumber);
    }

    public void delete(Article article) {
        articleRepository.delete(article);
    }

    public List<Article> saveOnlyNew(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return List.of();
        }

        Map<String, Article> uniqueArticlesByNumber = articles.stream()
                .collect(Collectors.toMap(
                        article -> article.getArticleNumber().trim(),
                        article -> article,
                        (firstArticle, duplicateArticle) -> firstArticle,
                        LinkedHashMap::new
                ));

        Set<String> articleNumbers = uniqueArticlesByNumber.keySet();

        Set<String> existingArticleNumbers = articleRepository.findByArticleNumberIn(articleNumbers)
                .stream()
                .map(Article::getArticleNumber)
                .collect(Collectors.toSet());

        List<Article> articlesToSave = uniqueArticlesByNumber.entrySet()
                .stream()
                .filter(entry -> !existingArticleNumbers.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        if (articlesToSave.isEmpty()) {
            return List.of();
        }

        return articleRepository.saveAll(articlesToSave);
    }
}
