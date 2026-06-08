package com.nikitaopara.warehouseoptimizer.putaway.article.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class ArticleNumberGeneratorService {

    private static final int MAX_GENERATION_ATTEMPTS = 30;
    private static final int MIN_ARTICLE_NUMBER = 100_000_000;
    private static final int MAX_ARTICLE_NUMBER = 999_999_999;

    private final SecureRandom secureRandom = new SecureRandom();

    private final ArticleDataService articleDataService;

    public String generateUniqueArticleNumber() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String articleNumber = generateArticleNumber();

            if (!articleDataService.existsByArticleNumber(articleNumber)) {
                return articleNumber;
            }
        }

        throw new IllegalStateException("Could not generate unique article number");
    }

    private String generateArticleNumber() {
        int range = MAX_ARTICLE_NUMBER - MIN_ARTICLE_NUMBER + 1;
        int value = secureRandom.nextInt(range) + MIN_ARTICLE_NUMBER;

        return String.valueOf(value);
    }
}