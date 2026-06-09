package com.nikitaopara.warehouseoptimizer.putaway.article.dto;

import java.util.List;

public record CreateArticlesBatchRequest(
        List<CreateArticleRequest> articles
) {
}