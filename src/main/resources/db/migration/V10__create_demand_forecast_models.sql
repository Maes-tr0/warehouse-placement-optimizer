CREATE SEQUENCE IF NOT EXISTS seq_demand_forecast_model
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS demand_forecast_models
(
    id                          BIGINT        NOT NULL DEFAULT nextval('seq_demand_forecast_model'),
    code                        VARCHAR(100)  NOT NULL,
    warehouse_id                BIGINT        NOT NULL,
    version_number              INTEGER       NOT NULL,
    status                      VARCHAR(50)   NOT NULL,
    training_trigger            VARCHAR(50)   NOT NULL,
    algorithm                   VARCHAR(100)  NOT NULL,
    feature_schema_version      INTEGER       NOT NULL,
    forecast_horizon_days       INTEGER       NOT NULL,
    training_start              DATE          NOT NULL,
    training_end                DATE          NOT NULL,
    validation_start            DATE          NOT NULL,
    validation_end              DATE          NOT NULL,
    data_cutoff                 DATE          NOT NULL,
    observation_count           INTEGER       NOT NULL,
    article_count               INTEGER       NOT NULL,
    training_sample_count       INTEGER       NOT NULL,
    validation_sample_count     INTEGER       NOT NULL,
    model_mae                   DOUBLE PRECISION,
    baseline_mae                DOUBLE PRECISION,
    model_rmse                  DOUBLE PRECISION,
    model_r2                    DOUBLE PRECISION,
    improvement_percent         DOUBLE PRECISION,
    model_artifact              BYTEA,
    error_message               VARCHAR(1000),
    trained_at                  TIMESTAMP,
    version                     BIGINT        NOT NULL DEFAULT 0,
    created_at                  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_demand_forecast_models PRIMARY KEY (id),
    CONSTRAINT uk_demand_forecast_models_code UNIQUE (code),
    CONSTRAINT uk_demand_forecast_models_warehouse_version
        UNIQUE (warehouse_id, version_number),

    CONSTRAINT fk_demand_forecast_models_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),

    CONSTRAINT chk_demand_forecast_models_status
        CHECK (status IN ('TRAINING', 'ACTIVE', 'REJECTED', 'SUPERSEDED', 'FAILED')),
    CONSTRAINT chk_demand_forecast_models_trigger
        CHECK (training_trigger IN ('MANUAL', 'SCHEDULED')),
    CONSTRAINT chk_demand_forecast_models_counts
        CHECK (
            version_number > 0
            AND feature_schema_version > 0
            AND forecast_horizon_days > 0
            AND observation_count >= 0
            AND article_count >= 0
            AND training_sample_count >= 0
            AND validation_sample_count >= 0
        ),
    CONSTRAINT chk_demand_forecast_models_ranges
        CHECK (
            training_start <= training_end
            AND validation_start <= validation_end
            AND training_end < validation_start
            AND validation_end <= data_cutoff
        )
);

ALTER SEQUENCE seq_demand_forecast_model
    OWNED BY demand_forecast_models.id;

CREATE INDEX IF NOT EXISTS idx_demand_forecast_models_warehouse_status
    ON demand_forecast_models (warehouse_id, status, trained_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_demand_forecast_models_one_active
    ON demand_forecast_models (warehouse_id)
    WHERE status = 'ACTIVE';
