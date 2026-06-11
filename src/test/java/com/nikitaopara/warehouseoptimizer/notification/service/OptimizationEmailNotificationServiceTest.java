package com.nikitaopara.warehouseoptimizer.notification.service;

import com.nikitaopara.warehouseoptimizer.eventing.model.OptimizationAssessmentEventPayload;
import com.nikitaopara.warehouseoptimizer.notification.config.EmailNotificationProperties;
import com.nikitaopara.warehouseoptimizer.optimization.model.OptimizationAssessmentStatus;
import com.nikitaopara.warehouseoptimizer.optimization.model.OptimizationAssessmentTrigger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OptimizationEmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendsRecommendationSummaryToConfiguredRecipients() {
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.setFrom("optimizer@example.com");
        properties.setRecipients(List.of("admin@example.com"));
        OptimizationEmailNotificationService service = new OptimizationEmailNotificationService(
                mailSender,
                properties
        );
        OptimizationAssessmentEventPayload assessment = new OptimizationAssessmentEventPayload(
                1L,
                2L,
                "WH-1",
                OptimizationAssessmentStatus.OPTIMIZATION_RECOMMENDED,
                OptimizationAssessmentTrigger.SCHEDULED,
                BigDecimal.valueOf(52),
                BigDecimal.valueOf(60),
                BigDecimal.valueOf(8_000),
                200,
                20,
                LocalDateTime.of(2026, 6, 11, 2, 0)
        );

        service.sendOptimizationRecommended(assessment);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("admin@example.com");
        assertThat(captor.getValue().getSubject()).contains("WH-1");
        assertThat(captor.getValue().getText()).contains("52%", "60%");
    }
}
