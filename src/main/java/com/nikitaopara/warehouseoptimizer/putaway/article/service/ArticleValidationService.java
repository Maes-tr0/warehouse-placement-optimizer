package com.nikitaopara.warehouseoptimizer.putaway.article.service;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.putaway.article.dto.CreateArticleRequest;
import com.nikitaopara.warehouseoptimizer.putaway.article.dto.CreateArticlesBatchRequest;
import com.nikitaopara.warehouseoptimizer.putaway.article.dto.UpdateArticleRequest;
import com.nikitaopara.warehouseoptimizer.putaway.container.repository.ContainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class ArticleValidationService {

    private final ArticleDataService articleDataService;
    private final ContainerRepository containerRepository;

    public void validateCreateArticleRequest(User actor, CreateArticleRequest request) {
        validateAdminActor(actor);
        validateCreateArticleFields(request);
    }

    public void validateCreateArticlesBatchRequest(
            User actor,
            CreateArticlesBatchRequest request
    ) {
        validateAdminActor(actor);

        if (request == null) {
            throw new IllegalArgumentException("Create articles batch request cannot be null");
        }

        if (request.articles() == null || request.articles().isEmpty()) {
            throw new IllegalArgumentException("Articles are required");
        }

        Set<String> articleNumbers = new TreeSet<>();

        for (CreateArticleRequest articleRequest : request.articles()) {
            validateCreateArticleFields(articleRequest);
            validateArticleNumberIsUniqueInBatch(articleRequest, articleNumbers);
        }
    }

    public void validateUpdateArticleRequest(User actor, UpdateArticleRequest request) {
        validateAdminActor(actor);

        if (request == null) {
            throw new IllegalArgumentException("Update article request cannot be null");
        }

        if (request.name() != null && !StringUtils.hasText(request.name())) {
            throw new IllegalArgumentException("Article name cannot be blank");
        }

        if (request.unitWidthMm() != null) {
            validatePositiveInteger(request.unitWidthMm(), "Unit width must be greater than zero");
        }

        if (request.unitLengthMm() != null) {
            validatePositiveInteger(request.unitLengthMm(), "Unit length must be greater than zero");
        }

        if (request.unitHeightMm() != null) {
            validatePositiveInteger(request.unitHeightMm(), "Unit height must be greater than zero");
        }

        if (request.unitWeightKg() != null) {
            validatePositiveBigDecimal(request.unitWeightKg(), "Unit weight must be greater than zero");
        }

        if (request.maxQuantityPerPallet() != null) {
            validatePositiveInteger(
                    request.maxQuantityPerPallet(),
                    "Max quantity per pallet must be greater than zero"
            );
        }
    }

    public void validateDeleteArticleRequest(User actor, Long articleId) {
        validateAdminActor(actor);

        if (articleId == null) {
            throw new IllegalArgumentException("Article id is required");
        }

        if (containerRepository.existsByArticleId(articleId)) {
            throw new IllegalArgumentException(
                    "Article cannot be deleted because it is already used by containers"
            );
        }
    }

    public void validateAdminActor(User actor) {
        if (actor == null || actor.getRole() == null) {
            throw new AccessDeniedException("Authenticated user is required");
        }

        if (actor.getRole() != Role.ROOT_ADMIN && actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN or ROOT_ADMIN can manage articles");
        }
    }

    private void validateCreateArticleFields(CreateArticleRequest request) {
        validateCreateRequestExists(request);
        validateArticleNumberForCreate(request.articleNumber());
        validateRequiredText(request.name());
        validateRequiredObject(request.unitType());

        validatePositiveInteger(request.unitWidthMm(), "Unit width must be greater than zero");
        validatePositiveInteger(request.unitLengthMm(), "Unit length must be greater than zero");
        validatePositiveInteger(request.unitHeightMm(), "Unit height must be greater than zero");
        validatePositiveBigDecimal(request.unitWeightKg(), "Unit weight must be greater than zero");
        validatePositiveInteger(
                request.maxQuantityPerPallet(),
                "Max quantity per pallet must be greater than zero"
        );
    }

    private void validateCreateRequestExists(CreateArticleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Create article request cannot be null");
        }
    }

    private void validateArticleNumberForCreate(String articleNumber) {
        if (!StringUtils.hasText(articleNumber)) {
            return;
        }

        String normalizedArticleNumber = articleNumber.trim();

        validateArticleNumberFormat(normalizedArticleNumber);

        if (articleDataService.existsByArticleNumber(normalizedArticleNumber)) {
            throw new IllegalArgumentException(
                    "Article with this article number already exists: " + normalizedArticleNumber
            );
        }
    }

    private void validateArticleNumberFormat(String articleNumber) {
        if (!StringUtils.hasText(articleNumber)) {
            return;
        }

        String normalizedArticleNumber = articleNumber.trim();

        if (!normalizedArticleNumber.matches("\\d+")) {
            throw new IllegalArgumentException("Article number must contain only digits");
        }
    }

    private void validateArticleNumberIsUniqueInBatch(
            CreateArticleRequest request,
            Set<String> articleNumbers
    ) {
        if (!StringUtils.hasText(request.articleNumber())) {
            return;
        }

        String normalizedArticleNumber = request.articleNumber().trim();

        if (!articleNumbers.add(normalizedArticleNumber)) {
            throw new IllegalArgumentException(
                    "Duplicate article number in request: " + normalizedArticleNumber
            );
        }
    }

    private void validateRequiredText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Article name is required");
        }
    }

    private void validateRequiredObject(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Unit type is required");
        }
    }

    private void validatePositiveInteger(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validatePositiveBigDecimal(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}