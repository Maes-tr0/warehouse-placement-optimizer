package com.nikitaopara.warehouseoptimizer.demand.forecast.service;

import com.nikitaopara.warehouseoptimizer.demand.forecast.config.DemandForecastProperties;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.*;
import com.nikitaopara.warehouseoptimizer.demand.forecast.repository.DemandForecastModelRepository;
import com.nikitaopara.warehouseoptimizer.demand.repository.OrderDemandItemRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemandForecastTrainingServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private OrderDemandItemRepository orderDemandItemRepository;
    @Mock
    private DemandForecastModelRepository modelRepository;
    @Mock
    private DemandForecastDatasetBuilder datasetBuilder;
    @Mock
    private TribuoDemandForecastTrainer trainer;

    private DemandForecastProperties properties;
    private DemandForecastTrainingService service;

    @BeforeEach
    void setUp() {
        properties = new DemandForecastProperties();
        properties.setMinimumTrainingSamples(1);
        properties.setMinimumValidationSamples(1);
        properties.setMinimumImprovementPercent(2.0);
        service = new DemandForecastTrainingService(
                warehouseRepository,
                orderDemandItemRepository,
                modelRepository,
                datasetBuilder,
                trainer,
                properties
        );
    }

    @Test
    void activatesImprovedCandidateAndSupersedesPreviousModel() {
        LocalDate cutoff = LocalDate.of(2026, 6, 1);
        Warehouse warehouse = Warehouse.builder().id(7L).build();
        DemandForecastModel previous = DemandForecastModel.builder()
                .status(DemandForecastModelStatus.ACTIVE)
                .build();
        DemandForecastDataset dataset = dataset(cutoff);
        DemandForecastMetrics metrics = new DemandForecastMetrics(
                4.0,
                10.0,
                5.0,
                0.8,
                60.0
        );

        when(warehouseRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(warehouse));
        when(orderDemandItemRepository.findByWarehouseIdAndOrderDemandOrderDateTimeBetween(
                any(), any(), any()
        )).thenReturn(List.of());
        when(datasetBuilder.build(any(), eq(cutoff))).thenReturn(dataset);
        when(modelRepository.findFirstByWarehouseIdOrderByVersionNumberDesc(7L))
                .thenReturn(Optional.empty());
        when(modelRepository.findFirstByWarehouseIdAndStatusOrderByTrainedAtDesc(
                7L,
                DemandForecastModelStatus.ACTIVE
        )).thenReturn(Optional.of(previous));
        when(modelRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(trainer.train(dataset)).thenReturn(new TrainedDemandForecast(new byte[]{1, 2}, metrics));

        DemandForecastTrainingResult result = service.train(
                7L,
                DemandForecastTrainingTrigger.MANUAL,
                cutoff
        );

        assertThat(result.status()).isEqualTo(DemandForecastModelStatus.ACTIVE);
        assertThat(result.versionNumber()).isEqualTo(1);
        assertThat(result.metrics()).isEqualTo(metrics);
        assertThat(previous.getStatus()).isEqualTo(DemandForecastModelStatus.SUPERSEDED);
        verify(modelRepository, atLeastOnce()).saveAndFlush(any(DemandForecastModel.class));
    }

    @Test
    void rejectsCandidateThatDoesNotBeatBaseline() {
        LocalDate cutoff = LocalDate.of(2026, 6, 1);
        DemandForecastDataset dataset = dataset(cutoff);
        DemandForecastMetrics metrics = new DemandForecastMetrics(
                10.0,
                10.0,
                12.0,
                0.1,
                0.0
        );

        when(warehouseRepository.findByIdForUpdate(7L))
                .thenReturn(Optional.of(Warehouse.builder().id(7L).build()));
        when(orderDemandItemRepository.findByWarehouseIdAndOrderDemandOrderDateTimeBetween(
                any(), any(), any()
        )).thenReturn(List.of());
        when(datasetBuilder.build(any(), eq(cutoff))).thenReturn(dataset);
        when(modelRepository.findFirstByWarehouseIdOrderByVersionNumberDesc(7L))
                .thenReturn(Optional.empty());
        when(modelRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(trainer.train(dataset)).thenReturn(new TrainedDemandForecast(new byte[]{1}, metrics));

        DemandForecastTrainingResult result = service.train(
                7L,
                DemandForecastTrainingTrigger.SCHEDULED,
                cutoff
        );

        assertThat(result.status()).isEqualTo(DemandForecastModelStatus.REJECTED);
        assertThat(result.message()).contains("did not improve baseline");
        verify(modelRepository, never()).findFirstByWarehouseIdAndStatusOrderByTrainedAtDesc(
                any(), any()
        );
    }

    private DemandForecastDataset dataset(LocalDate cutoff) {
        DemandForecastRow trainingRow = row(LocalDate.of(2026, 1, 31));
        DemandForecastRow validationRow = row(LocalDate.of(2026, 5, 1));

        return new DemandForecastDataset(
                List.of(trainingRow),
                List.of(validationRow),
                1,
                100,
                LocalDate.of(2026, 1, 1),
                cutoff,
                validationRow.featureDate()
        );
    }

    private DemandForecastRow row(LocalDate date) {
        return new DemandForecastRow(
                1L,
                date,
                10.0,
                12.0,
                new String[]{"quantity"},
                new double[]{10.0}
        );
    }
}
