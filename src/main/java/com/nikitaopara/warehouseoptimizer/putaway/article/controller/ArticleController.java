package com.nikitaopara.warehouseoptimizer.putaway.article.controller;

import com.nikitaopara.warehouseoptimizer.putaway.article.dto.ArticleResponse;
import com.nikitaopara.warehouseoptimizer.putaway.article.dto.CreateArticleRequest;
import com.nikitaopara.warehouseoptimizer.putaway.article.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}