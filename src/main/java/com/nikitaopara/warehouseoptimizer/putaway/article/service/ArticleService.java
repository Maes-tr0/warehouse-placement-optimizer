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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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

        String articleNumber = resolveArticleNumber(request.articleNumber());

        Article article = Article.builder()
                .articleNumber(articleNumber)
                .name(request.name().trim())
                .unitType(request.unitType())
                .unitWidthMm(request.unitWidthMm())
                .unitLengthMm(request.unitLengthMm())
                .unitHeightMm(request.unitHeightMm())
                .unitWeightKg(request.unitWeightKg())
                .maxQuantityPerPallet(request.maxQuantityPerPallet())
                .build();

        Article savedArticle = articleDataService.save(article);

        return ArticleResponse.from(savedArticle);
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

    @Transactional
    public CreateArticlesBatchResponse createArticlesBatch(CreateArticlesBatchRequest request) {
        User actor = authenticatedUserService.getCurrentUser();

        articleValidationService.validateCreateArticlesBatchRequest(actor, request);

        Set<String> requestedArticleNumbers = extractProvidedArticleNumbers(request);

        Set<String> existingArticleNumbers = articleDataService.getExistingArticleNumbers(
                requestedArticleNumbers
        );

        if (!existingArticleNumbers.isEmpty()) {
            throw new IllegalArgumentException(
                    "Articles already exist: " + new TreeSet<>(existingArticleNumbers)
            );
        }

        Set<String> usedArticleNumbers = new TreeSet<>();

        List<Article> articlesToSave = request.articles()
                .stream()
                .map(articleRequest -> toArticle(articleRequest, usedArticleNumbers))
                .toList();

        List<Article> savedArticles = articleDataService.saveAll(articlesToSave);

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

    private Set<String> extractProvidedArticleNumbers(CreateArticlesBatchRequest request) {
        return request.articles()
                .stream()
                .map(CreateArticleRequest::articleNumber)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Article toArticle(
            CreateArticleRequest request,
            Set<String> usedArticleNumbers
    ) {
        String articleNumber = resolveBatchArticleNumber(
                request.articleNumber(),
                usedArticleNumbers
        );

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

    private String resolveBatchArticleNumber(
            String articleNumber,
            Set<String> usedArticleNumbers
    ) {
        if (StringUtils.hasText(articleNumber)) {
            String normalizedArticleNumber = articleNumber.trim();

            if (!usedArticleNumbers.add(normalizedArticleNumber)) {
                throw new IllegalArgumentException(
                        "Duplicate article number in request: " + normalizedArticleNumber
                );
            }

            return normalizedArticleNumber;
        }

        String generatedArticleNumber;

        do {
            generatedArticleNumber = articleNumberGeneratorService.generateUniqueArticleNumber();
        } while (!usedArticleNumbers.add(generatedArticleNumber));

        return generatedArticleNumber;
    }
}