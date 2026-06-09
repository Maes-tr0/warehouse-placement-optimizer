CREATE SEQUENCE IF NOT EXISTS seq_placement_recommendation
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS placement_recommendations
(
    id                              BIGINT        NOT NULL DEFAULT nextval('seq_placement_recommendation'),
    code                            VARCHAR(100)  NOT NULL,

    warehouse_id                    BIGINT        NOT NULL,
    source_container_id             BIGINT        NOT NULL,
    target_container_id             BIGINT,
    recommended_storage_place_id    BIGINT        NOT NULL,

    recommendation_type             VARCHAR(50)   NOT NULL,
    status                          VARCHAR(50)   NOT NULL,

    distance_from_entry_mm          INTEGER,
    estimated_time_seconds          INTEGER,
    score                           NUMERIC(10,3) NOT NULL,
    reason                          VARCHAR(500),

    version                         BIGINT        NOT NULL DEFAULT 0,
    created_at                      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_placement_recommendations
        PRIMARY KEY (id),

    CONSTRAINT uk_placement_recommendations_code
        UNIQUE (code),

    CONSTRAINT fk_placement_recommendations_warehouse
        FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id),

    CONSTRAINT fk_placement_recommendations_source_container
        FOREIGN KEY (source_container_id)
        REFERENCES containers (id),

    CONSTRAINT fk_placement_recommendations_target_container
        FOREIGN KEY (target_container_id)
        REFERENCES containers (id),

    CONSTRAINT fk_placement_recommendations_storage_place
        FOREIGN KEY (recommended_storage_place_id)
        REFERENCES storage_places (id),

    CONSTRAINT chk_placement_recommendations_type
        CHECK (recommendation_type IN ('MERGE', 'PLACE')),

    CONSTRAINT chk_placement_recommendations_status
        CHECK (status IN ('SUGGESTED', 'ACCEPTED', 'REJECTED')),

    CONSTRAINT chk_placement_recommendations_merge_target_required
        CHECK (
            recommendation_type <> 'MERGE'
            OR target_container_id IS NOT NULL
        ),

    CONSTRAINT chk_placement_recommendations_distance_not_negative
        CHECK (
            distance_from_entry_mm IS NULL
            OR distance_from_entry_mm >= 0
        ),

    CONSTRAINT chk_placement_recommendations_estimated_time_not_negative
        CHECK (
            estimated_time_seconds IS NULL
            OR estimated_time_seconds >= 0
        ),

    CONSTRAINT chk_placement_recommendations_score_not_negative
        CHECK (score >= 0)
);

ALTER SEQUENCE seq_placement_recommendation
    OWNED BY placement_recommendations.id;

CREATE INDEX IF NOT EXISTS idx_placement_recommendations_source_container_status
    ON placement_recommendations (source_container_id, status);

CREATE INDEX IF NOT EXISTS idx_placement_recommendations_source_container_type
    ON placement_recommendations (source_container_id, recommendation_type);

CREATE INDEX IF NOT EXISTS idx_placement_recommendations_warehouse
    ON placement_recommendations (warehouse_id);

CREATE INDEX IF NOT EXISTS idx_placement_recommendations_storage_place
    ON placement_recommendations (recommended_storage_place_id);

CREATE INDEX IF NOT EXISTS idx_placement_recommendations_target_container
    ON placement_recommendations (target_container_id);