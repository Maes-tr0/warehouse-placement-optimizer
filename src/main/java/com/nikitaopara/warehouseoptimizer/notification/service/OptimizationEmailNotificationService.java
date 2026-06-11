package com.nikitaopara.warehouseoptimizer.notification.service;

import com.nikitaopara.warehouseoptimizer.eventing.model.OptimizationAssessmentEventPayload;
import com.nikitaopara.warehouseoptimizer.notification.config.EmailNotificationProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.notifications",
        name = "email-enabled",
        havingValue = "true"
)
public class OptimizationEmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(
            OptimizationEmailNotificationService.class
    );

    private final JavaMailSender mailSender;
    private final EmailNotificationProperties properties;

    public void sendOptimizationRecommended(OptimizationAssessmentEventPayload assessment) {
        String[] recipients = properties.getRecipients().stream()
                .filter(recipient -> recipient != null && !recipient.isBlank())
                .map(String::trim)
                .toArray(String[]::new);
        if (recipients.length == 0) {
            log.warn("Optimization e-mail skipped because no recipients are configured");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(recipients);
        message.setSubject("Warehouse " + assessment.warehouseCode() + " needs optimization");
        message.setText("""
                Warehouse optimization assessment recommends relocation planning.
                
                Warehouse: %s
                Score: %s%%
                Threshold: %s%%
                Analyzed containers: %d
                Demand observations: %d
                Analyzed at: %s
                """.formatted(
                assessment.warehouseCode(),
                assessment.scorePercent(),
                assessment.thresholdPercent(),
                assessment.analyzedContainerCount(),
                assessment.demandObservationCount(),
                assessment.analyzedAt()
        ));
        mailSender.send(message);
    }
}
