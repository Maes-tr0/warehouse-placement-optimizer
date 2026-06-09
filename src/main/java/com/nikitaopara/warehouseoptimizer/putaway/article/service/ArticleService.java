package com.nikitaopara.warehouseoptimizer.putaway.article.service;

import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.auth.service.AuthenticatedUserService;
import com.nikitaopara.warehouseoptimizer.putaway.article.dto.*;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final AuthenticatedUserService authenticatedUserService;
    private final ArticleValidationService articleValidationService;
    private final ArticleNumberGeneratorService articleNumberGeneratorService;
    private final ArticleDataService articleDataService;

    @Transactional
    public ArticleResponse createArticle(CreateArticleRequest request) {
        User actor = authenticatedUserService.getCurrentUser();

        articleValidationService.validateCreateArticleRequest(actor, request);

        Article article = toArticle(request);

        Article savedArticle = articleDataService.save(article);

        return ArticleResponse.from(savedArticle);
    }

    @Transactional
    public CreateArticlesBatchResponse createArticlesBatch(CreateArticlesBatchRequest request) {
        User actor = authenticatedUserService.getCurrentUser();

        articleValidationService.validateCreateArticlesBatchRequest(actor, request);

        List<Article> articles = request.articles()
                .stream()
                .map(this::toArticle)
                .toList();

        List<Article> savedArticles = articleDataService.saveOnlyNew(articles);

        List<ArticleResponse> responses = savedArticles
                .stream()
                .map(ArticleResponse::from)
                .toList();

        return new CreateArticlesBatchResponse(
                request.articles().size(),
                savedArticles.size(),
                responses
        );
    }
    @Transactional(readOnly = true)
    public ArticleResponse getArticleById(Long id) {
        Article article = articleDataService.getByIdOrThrow(id);

        return ArticleResponse.from(article);
    }

    @Transactional(readOnly = true)
    public ArticleResponse getArticleByArticleNumber(String articleNumber) {
        Article article = articleDataService.getByArticleNumberOrThrow(articleNumber);

        return ArticleResponse.from(article);
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> getArticles() {
        return articleDataService.getAll()
                .stream()
                .map(ArticleResponse::from)
                .toList();
    }

    @Transactional
    public ArticleResponse updateArticle(Long id, UpdateArticleRequest request) {
        User actor = authenticatedUserService.getCurrentUser();

        articleValidationService.validateUpdateArticleRequest(actor, request);

        Article article = articleDataService.getByIdOrThrow(id);

        applyUpdates(article, request);

        Article savedArticle = articleDataService.save(article);

        return ArticleResponse.from(savedArticle);
    }

    @Transactional
    public void deleteArticle(Long id) {
        User actor = authenticatedUserService.getCurrentUser();

        Article article = articleDataService.getByIdOrThrow(id);

        articleValidationService.validateDeleteArticleRequest(actor, article.getId());

        articleDataService.delete(article);
    }

    private Article toArticle(CreateArticleRequest request) {
        String articleNumber = resolveArticleNumber(request.articleNumber());

        return Article.builder()
                .articleNumber(articleNumber)
                .name(request.name().trim())
                .unitType(request.unitType())
                .unitWidthMm(request.unitWidthMm())
                .unitLengthMm(request.unitLengthMm())
                .unitHeightMm(request.unitHeightMm())
                .unitWeightKg(request.unitWeightKg())
                .maxQuantityPerPallet(request.maxQuantityPerPallet())
                .build();
    }

    private String resolveArticleNumber(String articleNumber) {
        if (StringUtils.hasText(articleNumber)) {
            return articleNumber.trim();
        }

        return articleNumberGeneratorService.generateUniqueArticleNumber();
    }

    private void applyUpdates(Article article, UpdateArticleRequest request) {
        if (StringUtils.hasText(request.name())) {
            article.setName(request.name().trim());
        }

        if (request.unitType() != null) {
            article.setUnitType(request.unitType());
        }

        if (request.unitWidthMm() != null) {
            article.setUnitWidthMm(request.unitWidthMm());
        }

        if (request.unitLengthMm() != null) {
            article.setUnitLengthMm(request.unitLengthMm());
        }

        if (request.unitHeightMm() != null) {
            article.setUnitHeightMm(request.unitHeightMm());
        }

        if (request.unitWeightKg() != null) {
            article.setUnitWeightKg(request.unitWeightKg());
        }

        if (request.maxQuantityPerPallet() != null) {
            article.setMaxQuantityPerPallet(request.maxQuantityPerPallet());
        }
    }
}