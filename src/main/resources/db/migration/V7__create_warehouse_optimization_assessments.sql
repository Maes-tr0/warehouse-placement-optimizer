CREATE SEQUENCE IF NOT EXISTS seq_warehouse_optimization_assessment
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS warehouse_optimization_assessments
(
    id                              BIGINT        NOT NULL DEFAULT nextval('seq_warehouse_optimization_assessment'),
    warehouse_id                    BIGINT        NOT NULL,
    status                          VARCHAR(50)   NOT NULL,
    analysis_trigger                VARCHAR(50)   NOT NULL,
    score_percent                   NUMERIC(5,2),
    threshold_percent               NUMERIC(5,2)  NOT NULL,
    weighted_average_distance_mm    NUMERIC(14,2),
    lookback_start                  TIMESTAMP     NOT NULL,
    analyzed_at                     TIMESTAMP     NOT NULL,
    demand_observation_count        INTEGER       NOT NULL,
    analyzed_container_count        INTEGER       NOT NULL,
    demand_matched_container_count  INTEGER       NOT NULL,
    version                         BIGINT        NOT NULL DEFAULT 0,
    created_at                      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_warehouse_optimization_assessments PRIMARY KEY (id),

    CONSTRAINT fk_warehouse_optimization_assessments_warehouse
        FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id),

    CONSTRAINT chk_warehouse_optimization_assessments_status
        CHECK (status IN ('INSUFFICIENT_DATA', 'HEALTHY', 'OPTIMIZATION_RECOMMENDED')),

    CONSTRAINT chk_warehouse_optimization_assessments_trigger
        CHECK (analysis_trigger IN ('MANUAL', 'SCHEDULED')),

    CONSTRAINT chk_warehouse_optimization_assessments_score
        CHECK (score_percent IS NULL OR score_percent BETWEEN 0 AND 100),

    CONSTRAINT chk_warehouse_optimization_assessments_threshold
        CHECK (threshold_percent BETWEEN 0 AND 100),

    CONSTRAINT chk_warehouse_optimization_assessments_counts
        CHECK (
            demand_observation_count >= 0
            AND analyzed_container_count >= 0
            AND demand_matched_container_count >= 0
        )
);

ALTER SEQUENCE seq_warehouse_optimization_assessment
    OWNED BY warehouse_optimization_assessments.id;

CREATE INDEX IF NOT EXISTS idx_warehouse_optimization_assessments_latest
    ON warehouse_optimization_assessments (warehouse_id, analyzed_at DESC);
