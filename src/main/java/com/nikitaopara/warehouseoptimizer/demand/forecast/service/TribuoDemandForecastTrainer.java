package com.nikitaopara.warehouseoptimizer.demand.forecast.service;

import com.nikitaopara.warehouseoptimizer.demand.forecast.config.DemandForecastProperties;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastDataset;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastMetrics;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastRow;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.TrainedDemandForecast;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tribuo.Example;
import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.datasource.ListDataSource;
import org.tribuo.impl.ArrayExample;
import org.tribuo.provenance.SimpleDataSourceProvenance;
import org.tribuo.regression.RegressionFactory;
import org.tribuo.regression.Regressor;
import org.tribuo.regression.rtree.CARTRegressionTrainer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TribuoDemandForecastTrainer {

    private static final String OUTPUT_NAME = "forecast_quantity";

    private final DemandForecastProperties properties;

    public TrainedDemandForecast train(DemandForecastDataset dataset) {
        RegressionFactory outputFactory = new RegressionFactory();
        MutableDataset<Regressor> trainingDataset = toDataset(
                dataset.trainingRows(),
                outputFactory,
                "warehouse demand forecast training data"
        );
        Model<Regressor> model = new CARTRegressionTrainer(
                properties.getMaximumTreeDepth()
        ).train(trainingDataset);
        DemandForecastMetrics metrics = evaluate(model, dataset.validationRows());

        return new TrainedDemandForecast(serialize(model), metrics);
    }

    public Model<Regressor> deserialize(byte[] artifact) {
        if (artifact == null || artifact.length == 0) {
            throw new IllegalArgumentException("Demand forecast model artifact is empty");
        }

        try (ByteArrayInputStream input = new ByteArrayInputStream(artifact)) {
            return Model.deserializeFromStream(input).castModel(Regressor.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot deserialize demand forecast model", exception);
        }
    }

    public double predict(Model<Regressor> model, DemandForecastRow row) {
        Regressor output = model.predict(toExample(row)).getOutput();
        return Math.max(0.0, output.getValues()[0]);
    }

    private MutableDataset<Regressor> toDataset(
            List<DemandForecastRow> rows,
            RegressionFactory outputFactory,
            String description
    ) {
        List<Example<Regressor>> examples = rows.stream()
                .map(this::toExample)
                .map(example -> (Example<Regressor>) example)
                .toList();
        ListDataSource<Regressor> dataSource = new ListDataSource<>(
                examples,
                outputFactory,
                new SimpleDataSourceProvenance(description, outputFactory)
        );

        return new MutableDataset<>(dataSource);
    }

    private ArrayExample<Regressor> toExample(DemandForecastRow row) {
        return new ArrayExample<>(
                new Regressor(OUTPUT_NAME, row.targetQuantity()),
                row.featureNames(),
                row.featureValues()
        );
    }

    private DemandForecastMetrics evaluate(
            Model<Regressor> model,
            List<DemandForecastRow> validationRows
    ) {
        double modelAbsoluteError = 0.0;
        double baselineAbsoluteError = 0.0;
        double modelSquaredError = 0.0;
        double targetSum = validationRows.stream()
                .mapToDouble(DemandForecastRow::targetQuantity)
                .sum();
        double targetMean = targetSum / validationRows.size();
        double totalTargetVariance = 0.0;

        for (DemandForecastRow row : validationRows) {
            double prediction = predict(model, row);
            double modelError = prediction - row.targetQuantity();
            double baselineError = row.baselineQuantity() - row.targetQuantity();
            modelAbsoluteError += Math.abs(modelError);
            baselineAbsoluteError += Math.abs(baselineError);
            modelSquaredError += modelError * modelError;
            double centeredTarget = row.targetQuantity() - targetMean;
            totalTargetVariance += centeredTarget * centeredTarget;
        }

        double size = validationRows.size();
        double modelMae = modelAbsoluteError / size;
        double baselineMae = baselineAbsoluteError / size;
        double rmse = Math.sqrt(modelSquaredError / size);
        double r2 = totalTargetVariance == 0.0
                ? (modelSquaredError == 0.0 ? 1.0 : 0.0)
                : 1.0 - modelSquaredError / totalTargetVariance;
        double improvementPercent = baselineMae == 0.0
                ? (modelMae == 0.0 ? 0.0 : -100.0)
                : (baselineMae - modelMae) / baselineMae * 100.0;

        return new DemandForecastMetrics(
                modelMae,
                baselineMae,
                rmse,
                r2,
                improvementPercent
        );
    }

    private byte[] serialize(Model<Regressor> model) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            model.serializeToStream(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot serialize demand forecast model", exception);
        }
    }
}
