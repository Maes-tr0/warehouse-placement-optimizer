package com.nikitaopara.warehouseoptimizer.putaway.article.dto;

import java.util.List;

public record CreateArticlesBatchResponse(
        Integer totalArticles,
        Integer createdArticles,
        List<ArticleResponse> articles
) {
}