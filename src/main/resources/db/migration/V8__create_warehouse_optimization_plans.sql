CREATE SEQUENCE IF NOT EXISTS seq_warehouse_optimization_plan
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS warehouse_optimization_plans
(
    id                              BIGINT        NOT NULL DEFAULT nextval('seq_warehouse_optimization_plan'),
    code                            VARCHAR(100)  NOT NULL,
    warehouse_id                    BIGINT        NOT NULL,
    assessment_id                   BIGINT        NOT NULL,
    status                          VARCHAR(50)   NOT NULL,
    initial_score_percent           NUMERIC(5,2)  NOT NULL,
    target_score_percent            NUMERIC(5,2)  NOT NULL,
    projected_score_percent         NUMERIC(5,2)  NOT NULL,
    estimated_time_saving_seconds   BIGINT        NOT NULL,
    created_by_user_id              BIGINT        NOT NULL,
    approved_by_user_id             BIGINT,
    approved_at                     TIMESTAMP,
    completed_at                    TIMESTAMP,
    version                         BIGINT        NOT NULL DEFAULT 0,
    created_at                      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_warehouse_optimization_plans PRIMARY KEY (id),
    CONSTRAINT uk_warehouse_optimization_plans_code UNIQUE (code),
    CONSTRAINT uk_warehouse_optimization_plans_assessment UNIQUE (assessment_id),

    CONSTRAINT fk_warehouse_optimization_plans_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_warehouse_optimization_plans_assessment
        FOREIGN KEY (assessment_id) REFERENCES warehouse_optimization_assessments (id),
    CONSTRAINT fk_warehouse_optimization_plans_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_warehouse_optimization_plans_approved_by
        FOREIGN KEY (approved_by_user_id) REFERENCES users (id),

    CONSTRAINT chk_warehouse_optimization_plans_status
        CHECK (status IN ('DRAFT', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_warehouse_optimization_plans_scores
        CHECK (
            initial_score_percent BETWEEN 0 AND 100
            AND target_score_percent BETWEEN 0 AND 100
            AND projected_score_percent BETWEEN 0 AND 100
        ),
    CONSTRAINT chk_warehouse_optimization_plans_saving
        CHECK (estimated_time_saving_seconds >= 0)
);

ALTER SEQUENCE seq_warehouse_optimization_plan
    OWNED BY warehouse_optimization_plans.id;

CREATE INDEX IF NOT EXISTS idx_warehouse_optimization_plans_warehouse_status
    ON warehouse_optimization_plans (warehouse_id, status);


CREATE SEQUENCE IF NOT EXISTS seq_warehouse_relocation_step
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS warehouse_relocation_steps
(
    id                              BIGINT        NOT NULL DEFAULT nextval('seq_warehouse_relocation_step'),
    plan_id                         BIGINT        NOT NULL,
    sequence_number                 INTEGER       NOT NULL,
    step_type                       VARCHAR(50)   NOT NULL,
    status                          VARCHAR(50)   NOT NULL,
    source_container_id             BIGINT        NOT NULL,
    target_container_id             BIGINT,
    from_storage_place_id           BIGINT,
    to_storage_place_id             BIGINT,
    estimated_time_saving_seconds   BIGINT        NOT NULL,
    reason                          VARCHAR(500)  NOT NULL,
    completed_by_user_id            BIGINT,
    completed_at                    TIMESTAMP,
    version                         BIGINT        NOT NULL DEFAULT 0,
    created_at                      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_warehouse_relocation_steps PRIMARY KEY (id),
    CONSTRAINT uk_warehouse_relocation_steps_plan_sequence UNIQUE (plan_id, sequence_number),

    CONSTRAINT fk_warehouse_relocation_steps_plan
        FOREIGN KEY (plan_id) REFERENCES warehouse_optimization_plans (id),
    CONSTRAINT fk_warehouse_relocation_steps_source_container
        FOREIGN KEY (source_container_id) REFERENCES containers (id),
    CONSTRAINT fk_warehouse_relocation_steps_target_container
        FOREIGN KEY (target_container_id) REFERENCES containers (id),
    CONSTRAINT fk_warehouse_relocation_steps_from_place
        FOREIGN KEY (from_storage_place_id) REFERENCES storage_places (id),
    CONSTRAINT fk_warehouse_relocation_steps_to_place
        FOREIGN KEY (to_storage_place_id) REFERENCES storage_places (id),
    CONSTRAINT fk_warehouse_relocation_steps_completed_by
        FOREIGN KEY (completed_by_user_id) REFERENCES users (id),

    CONSTRAINT chk_warehouse_relocation_steps_type
        CHECK (step_type IN ('MOVE', 'TEMPORARY_MOVE', 'MERGE')),
    CONSTRAINT chk_warehouse_relocation_steps_status
        CHECK (status IN ('PENDING', 'READY', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_warehouse_relocation_steps_sequence
        CHECK (sequence_number > 0),
    CONSTRAINT chk_warehouse_relocation_steps_saving
        CHECK (estimated_time_saving_seconds >= 0),
    CONSTRAINT chk_warehouse_relocation_steps_merge_target
        CHECK (step_type <> 'MERGE' OR target_container_id IS NOT NULL),
    CONSTRAINT chk_warehouse_relocation_steps_move_target
        CHECK (step_type = 'MERGE' OR to_storage_place_id IS NOT NULL)
);

ALTER SEQUENCE seq_warehouse_relocation_step
    OWNED BY warehouse_relocation_steps.id;

CREATE INDEX IF NOT EXISTS idx_warehouse_relocation_steps_plan_status
    ON warehouse_relocation_steps (plan_id, status);
