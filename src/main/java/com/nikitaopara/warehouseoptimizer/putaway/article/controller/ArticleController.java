package com.nikitaopara.warehouseoptimizer.putaway.article.controller;

import com.nikitaopara.warehouseoptimizer.putaway.article.dto.*;
import com.nikitaopara.warehouseoptimizer.putaway.article.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping
    public ResponseEntity<ArticleResponse> createArticle(@RequestBody CreateArticleRequest request) {
        ArticleResponse response = articleService.createArticle(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticleById(@PathVariable Long id) {
        ArticleResponse response = articleService.getArticleById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/number/{articleNumber}")
    public ResponseEntity<ArticleResponse> getArticleByArticleNumber(@PathVariable String articleNumber) {
        ArticleResponse response = articleService.getArticleByArticleNumber(articleNumber);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ArticleResponse>> getArticles() {
        return ResponseEntity.ok(articleService.getArticles());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable Long id,
            @RequestBody UpdateArticleRequest request
    ) {
        ArticleResponse response = articleService.updateArticle(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch")
    public ResponseEntity<CreateArticlesBatchResponse> createArticlesBatch(
            @RequestBody CreateArticlesBatchRequest request
    ) {
        CreateArticlesBatchResponse response = articleService.createArticlesBatch(request);

        return ResponseEntity.ok(response);
    }
}