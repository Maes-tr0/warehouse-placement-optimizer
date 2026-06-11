package com.nikitaopara.warehouseoptimizer.demand.forecast.service;

import com.nikitaopara.warehouseoptimizer.demand.forecast.config.DemandForecastProperties;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastModel;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastModelStatus;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastRow;
import com.nikitaopara.warehouseoptimizer.demand.forecast.repository.DemandForecastModelRepository;
import com.nikitaopara.warehouseoptimizer.optimization.config.OptimizationProperties;
import com.nikitaopara.warehouseoptimizer.optimization.model.ArticleDemandScore;
import com.nikitaopara.warehouseoptimizer.optimization.model.DemandObservation;
import com.nikitaopara.warehouseoptimizer.optimization.service.SeasonalDemandModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tribuo.Model;
import org.tribuo.regression.Regressor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemandForecastScoringServiceTest {

    @Mock
    private DemandForecastModelRepository modelRepository;
    @Mock
    private DemandForecastDatasetBuilder datasetBuilder;
    @Mock
    private TribuoDemandForecastTrainer trainer;
    @Mock
    private Model<Regressor> tribuoModel;

    private SeasonalDemandModel seasonalDemandModel;
    private DemandForecastScoringService service;

    @BeforeEach
    void setUp() {
        DemandForecastProperties forecastProperties = new DemandForecastProperties();
        OptimizationProperties optimizationProperties = new OptimizationProperties();
        seasonalDemandModel = new SeasonalDemandModel();
        service = new DemandForecastScoringService(
                modelRepository,
                datasetBuilder,
                trainer,
                seasonalDemandModel,
                forecastProperties,
                optimizationProperties
        );
    }

    @Test
    void usesSeasonalFallbackWhenNoActiveModelExists() {
        LocalDate analysisDate = LocalDate.of(2026, 6, 10);
        List<DemandObservation> observations = List.of(observation(1L, analysisDate, 5));
        when(modelRepository.findFirstByWarehouseIdAndStatusOrderByTrainedAtDesc(
                9L,
                DemandForecastModelStatus.ACTIVE
        )).thenReturn(Optional.empty());

        Map<Long, ArticleDemandScore> result = service.calculate(9L, observations, analysisDate);

        assertThat(result.get(1L).totalQuantity()).isEqualTo(5);
        assertThat(result.get(1L).weightedDemand()).isPositive();
        verifyNoInteractions(datasetBuilder, trainer);
    }

    @Test
    void replacesMatureArticleWeightWithActiveModelForecast() {
        LocalDate analysisDate = LocalDate.of(2026, 6, 10);
        List<DemandObservation> observations = List.of(observation(1L, analysisDate, 5));
        DemandForecastModel activeModel = DemandForecastModel.builder()
                .code("DFM-1")
                .status(DemandForecastModelStatus.ACTIVE)
                .featureSchemaVersion(1)
                .forecastHorizonDays(14)
                .modelArtifact(new byte[]{1})
                .build();
        DemandForecastRow row = new DemandForecastRow(
                1L,
                analysisDate,
                0.0,
                0.0,
                new String[]{"quantity"},
                new double[]{5.0}
        );

        when(modelRepository.findFirstByWarehouseIdAndStatusOrderByTrainedAtDesc(
                9L,
                DemandForecastModelStatus.ACTIVE
        )).thenReturn(Optional.of(activeModel));
        when(trainer.deserialize(activeModel.getModelArtifact())).thenReturn(tribuoModel);
        when(datasetBuilder.buildPredictionRow(1L, observations, analysisDate)).thenReturn(row);
        when(trainer.predict(tribuoModel, row)).thenReturn(42.0);

        ArticleDemandScore result = service.calculate(9L, observations, analysisDate).get(1L);

        assertThat(result.weightedDemand()).isEqualTo(42.0);
        assertThat(result.totalQuantity()).isEqualTo(5);
        assertThat(result.orderCount()).isEqualTo(1);
    }

    @Test
    void usesHorizonScaledBaselineForArticleWithoutEnoughHistory() {
        LocalDate analysisDate = LocalDate.of(2026, 6, 10);
        List<DemandObservation> observations = List.of(observation(1L, analysisDate, 5));
        DemandForecastModel activeModel = DemandForecastModel.builder()
                .code("DFM-2")
                .featureSchemaVersion(1)
                .forecastHorizonDays(14)
                .modelArtifact(new byte[]{2})
                .build();

        when(modelRepository.findFirstByWarehouseIdAndStatusOrderByTrainedAtDesc(any(), any()))
                .thenReturn(Optional.of(activeModel));
        when(trainer.deserialize(activeModel.getModelArtifact())).thenReturn(tribuoModel);
        when(datasetBuilder.buildPredictionRow(1L, observations, analysisDate))
                .thenThrow(new IllegalArgumentException("insufficient history"));
        when(datasetBuilder.buildBaselineForecast(1L, observations, analysisDate))
                .thenReturn(2.5);

        ArticleDemandScore result = service.calculate(9L, observations, analysisDate).get(1L);

        assertThat(result.weightedDemand()).isEqualTo(2.5);
        assertThat(result.totalQuantity()).isEqualTo(5);
        verify(trainer, never()).predict(any(), any());
    }

    private DemandObservation observation(Long articleId, LocalDate date, int quantity) {
        return new DemandObservation(
                articleId,
                articleId * 10,
                LocalDateTime.from(date.atStartOfDay()),
                quantity
        );
    }
}
