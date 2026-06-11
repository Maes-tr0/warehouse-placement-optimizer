package com.nikitaopara.warehouseoptimizer;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import com.nikitaopara.warehouseoptimizer.account.model.Status;
import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.account.repository.UserRepository;
import com.nikitaopara.warehouseoptimizer.optimization.dto.CompleteRelocationStepRequest;
import com.nikitaopara.warehouseoptimizer.optimization.model.*;
import com.nikitaopara.warehouseoptimizer.optimization.repository.WarehouseOptimizationAssessmentRepository;
import com.nikitaopara.warehouseoptimizer.optimization.repository.WarehouseOptimizationPlanRepository;
import com.nikitaopara.warehouseoptimizer.optimization.service.WarehouseOptimizationPlanService;
import com.nikitaopara.warehouseoptimizer.optimization.service.WarehouseRelocationExecutionService;
import com.nikitaopara.warehouseoptimizer.putaway.article.dto.CreateArticleRequest;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.UnitType;
import com.nikitaopara.warehouseoptimizer.putaway.article.repository.ArticleRepository;
import com.nikitaopara.warehouseoptimizer.putaway.article.service.ArticleService;
import com.nikitaopara.warehouseoptimizer.putaway.container.dto.PlaceContainerRequest;
import com.nikitaopara.warehouseoptimizer.putaway.container.dto.ReceiveContainerRequest;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.repository.ContainerRepository;
import com.nikitaopara.warehouseoptimizer.putaway.container.service.ContainerService;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.CreateRackLevelProfileRequest;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.CreateWarehouseRequest;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import com.nikitaopara.warehouseoptimizer.warehouse.model.WarehouseLayoutType;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.StoragePlaceRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.WarehouseRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.service.WarehouseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class OptimizationReservationIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private StoragePlaceRepository storagePlaceRepository;
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private ContainerRepository containerRepository;
    @Autowired
    private WarehouseOptimizationAssessmentRepository assessmentRepository;
    @Autowired
    private WarehouseOptimizationPlanRepository planRepository;
    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private ContainerService containerService;
    @Autowired
    private WarehouseOptimizationPlanService planService;
    @Autowired
    private WarehouseRelocationExecutionService executionService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reservesApprovedPlanResourcesAndReleasesThemAfterExecution() {
        User actor = authenticateRootAdmin();
        Long warehouseId = createWarehouse();
        articleService.createArticle(new CreateArticleRequest(
                "100001",
                "Integration article",
                UnitType.PCS,
                200,
                300,
                200,
                BigDecimal.valueOf(2),
                100
        ));
        Article article = articleRepository.findByArticleNumber("100001").orElseThrow();
        List<StoragePlace> places = storagePlaceRepository
                .findByWarehouseIdOrderByDistanceFromEntryMmAsc(warehouseId);
        StoragePlace targetPlace = places.getFirst();
        StoragePlace sourcePlace = places.getLast();

        containerService.receiveContainer(new ReceiveContainerRequest(
                warehouseId,
                "INT-C-1",
                article.getArticleNumber(),
                40,
                BigDecimal.valueOf(80),
                1_000
        ));
        containerService.placeContainer(
                "INT-C-1",
                new PlaceContainerRequest(sourcePlace.getCode())
        );

        Warehouse warehouse = warehouseRepository.findById(warehouseId).orElseThrow();
        Container source = containerRepository.findByContainerNumber("INT-C-1").orElseThrow();
        sourcePlace = storagePlaceRepository.findStoragePlaceByWarehouseIdAndCode(
                warehouseId,
                sourcePlace.getCode()
        ).orElseThrow();
        targetPlace = storagePlaceRepository.findStoragePlaceByWarehouseIdAndCode(
                warehouseId,
                targetPlace.getCode()
        ).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        WarehouseOptimizationAssessment assessment = assessmentRepository.saveAndFlush(
                WarehouseOptimizationAssessment.builder()
                        .warehouse(warehouse)
                        .status(OptimizationAssessmentStatus.OPTIMIZATION_RECOMMENDED)
                        .trigger(OptimizationAssessmentTrigger.MANUAL)
                        .scorePercent(BigDecimal.valueOf(40))
                        .thresholdPercent(BigDecimal.valueOf(60))
                        .weightedAverageDistanceMm(BigDecimal.valueOf(9_000))
                        .lookbackStart(now.minusDays(30))
                        .analyzedAt(now)
                        .demandObservationCount(100)
                        .analyzedContainerCount(1)
                        .demandMatchedContainerCount(1)
                        .build()
        );
        WarehouseOptimizationPlan plan = WarehouseOptimizationPlan.builder()
                .code("OPT-INTEGRATION-1")
                .warehouse(warehouse)
                .assessment(assessment)
                .status(OptimizationPlanStatus.DRAFT)
                .initialScorePercent(BigDecimal.valueOf(40))
                .targetScorePercent(BigDecimal.valueOf(85))
                .projectedScorePercent(BigDecimal.valueOf(90))
                .estimatedTimeSavingSeconds(30L)
                .createdBy(actor)
                .build();
        plan.addStep(WarehouseRelocationStep.builder()
                .sequenceNumber(1)
                .type(RelocationStepType.MOVE)
                .status(RelocationStepStatus.PENDING)
                .sourceContainer(source)
                .fromStoragePlace(sourcePlace)
                .toStoragePlace(targetPlace)
                .estimatedTimeSavingSeconds(30L)
                .reason("Integration relocation")
                .build());
        planRepository.saveAndFlush(plan);

        planService.approvePlan(plan.getCode());

        Container reserved = containerRepository.findByContainerNumber("INT-C-1").orElseThrow();
        assertThat(reserved.getOptimizationReservationCode()).isEqualTo(plan.getCode());
        assertThatThrownBy(() -> containerService.removeContainer("INT-C-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reserved by optimization plan");

        var execution = executionService.completeCurrentStep(
                plan.getCode(),
                new CompleteRelocationStepRequest(
                        "INT-C-1",
                        targetPlace.getCode(),
                        null
                )
        );

        assertThat(execution.planStatus()).isEqualTo(OptimizationPlanStatus.COMPLETED);
        Container completed = containerRepository.findByContainerNumber("INT-C-1").orElseThrow();
        assertThat(completed.getCurrentStoragePlace().getCode()).isEqualTo(targetPlace.getCode());
        assertThat(completed.getOptimizationReservationCode()).isNull();
        assertThat(storagePlaceRepository.findStoragePlaceByWarehouseIdAndCode(
                warehouseId,
                sourcePlace.getCode()
        ).orElseThrow().getOptimizationReservationCode()).isNull();
        assertThat(storagePlaceRepository.findStoragePlaceByWarehouseIdAndCode(
                warehouseId,
                targetPlace.getCode()
        ).orElseThrow().getOptimizationReservationCode()).isNull();
    }

    private User authenticateRootAdmin() {
        User actor = userRepository.saveAndFlush(User.builder()
                .email("integration-admin@example.com")
                .passwordHash("not-used")
                .fullName("Integration Admin")
                .role(Role.ROOT_ADMIN)
                .status(Status.ACTIVE)
                .build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        actor.getEmail(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ROOT_ADMIN"))
                )
        );
        return actor;
    }

    private Long createWarehouse() {
        return warehouseService.createWarehouse(new CreateWarehouseRequest(
                "INT-WH-1",
                "Integration warehouse",
                WarehouseLayoutType.MAIN_CORRIDOR_ONE_SIDE_AISLES,
                1,
                1,
                2,
                1,
                3_500,
                List.of(new CreateRackLevelProfileRequest(1, 1_800, 1_000)),
                2_000
        )).id();
    }
}
