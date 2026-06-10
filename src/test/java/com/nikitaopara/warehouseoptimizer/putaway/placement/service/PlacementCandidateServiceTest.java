package com.nikitaopara.warehouseoptimizer.putaway.placement.service;

import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.service.ContainerDataService;
import com.nikitaopara.warehouseoptimizer.putaway.container.service.ContainerDimensionCalculationService;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlaceStatus;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.StoragePlaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlacementCandidateServiceTest {

    @Mock
    private ContainerDataService containerDataService;

    @Mock
    private ContainerDimensionCalculationService dimensionCalculationService;

    @Mock
    private StoragePlaceRepository storagePlaceRepository;

    @Mock
    private PlacementRecommendationDataService recommendationDataService;

    @InjectMocks
    private PlacementCandidateService candidateService;

    @Test
    void reusesPreviouslyRejectedPlaceAfterAlternativesAreExhausted() {
        Warehouse warehouse = Warehouse.builder().id(1L).code("WH-1").build();
        Article article = Article.builder().id(2L).articleNumber("1001").build();
        Container sourceContainer = Container.builder()
                .id(3L)
                .warehouse(warehouse)
                .article(article)
                .weightKg(BigDecimal.valueOf(100))
                .heightMm(1_000)
                .build();
        StoragePlace onlyCandidate = StoragePlace.builder()
                .id(4L)
                .code("AA100")
                .maxWeightKg(500)
                .maxHeightMm(2_000)
                .distanceFromEntryMm(1_000)
                .status(StoragePlaceStatus.AVAILABLE)
                .build();

        when(recommendationDataService.getSuggestedStoragePlaceIds(warehouse.getId()))
                .thenReturn(Set.of());
        when(recommendationDataService.getPreviouslyRecommendedStoragePlaceIds(sourceContainer))
                .thenReturn(Set.of(onlyCandidate.getId()));
        when(storagePlaceRepository.findByWarehouseIdAndStatusOrderByDistanceFromEntryMmAsc(
                warehouse.getId(),
                StoragePlaceStatus.AVAILABLE
        )).thenReturn(List.of(onlyCandidate));

        var result = candidateService.findBestPlaceRecommendationCandidate(sourceContainer);

        assertThat(result).contains(onlyCandidate);
    }
}
