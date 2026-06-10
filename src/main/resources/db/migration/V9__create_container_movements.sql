CREATE SEQUENCE IF NOT EXISTS seq_container_movement
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS container_movements
(
    id                          BIGINT        NOT NULL DEFAULT nextval('seq_container_movement'),
    warehouse_id                BIGINT        NOT NULL,
    container_id                BIGINT        NOT NULL,
    target_container_id         BIGINT,
    from_storage_place_id       BIGINT,
    to_storage_place_id         BIGINT,
    optimization_plan_id        BIGINT,
    relocation_step_id          BIGINT,
    movement_type               VARCHAR(50)   NOT NULL,
    container_number            VARCHAR(100)  NOT NULL,
    article_number              VARCHAR(50)   NOT NULL,
    target_container_number     VARCHAR(100),
    from_storage_place_code     VARCHAR(50),
    to_storage_place_code       VARCHAR(50),
    quantity                    INTEGER       NOT NULL,
    performed_by_user_id        BIGINT        NOT NULL,
    performed_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_container_movements PRIMARY KEY (id),
    CONSTRAINT uk_container_movements_relocation_step UNIQUE (relocation_step_id),

    CONSTRAINT fk_container_movements_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_container_movements_container
        FOREIGN KEY (container_id) REFERENCES containers (id),
    CONSTRAINT fk_container_movements_target_container
        FOREIGN KEY (target_container_id) REFERENCES containers (id),
    CONSTRAINT fk_container_movements_from_place
        FOREIGN KEY (from_storage_place_id) REFERENCES storage_places (id),
    CONSTRAINT fk_container_movements_to_place
        FOREIGN KEY (to_storage_place_id) REFERENCES storage_places (id),
    CONSTRAINT fk_container_movements_optimization_plan
        FOREIGN KEY (optimization_plan_id) REFERENCES warehouse_optimization_plans (id),
    CONSTRAINT fk_container_movements_relocation_step
        FOREIGN KEY (relocation_step_id) REFERENCES warehouse_relocation_steps (id),
    CONSTRAINT fk_container_movements_performed_by
        FOREIGN KEY (performed_by_user_id) REFERENCES users (id),

    CONSTRAINT chk_container_movements_type
        CHECK (movement_type IN ('PUTAWAY', 'RELOCATION', 'TEMPORARY_RELOCATION', 'MERGE', 'REMOVAL')),
    CONSTRAINT chk_container_movements_quantity
        CHECK (quantity > 0)
);

ALTER SEQUENCE seq_container_movement
    OWNED BY container_movements.id;

CREATE INDEX IF NOT EXISTS idx_container_movements_warehouse_performed_at
    ON container_movements (warehouse_id, performed_at DESC);

CREATE INDEX IF NOT EXISTS idx_container_movements_container_performed_at
    ON container_movements (container_id, performed_at DESC);

CREATE INDEX IF NOT EXISTS idx_container_movements_plan
    ON container_movements (optimization_plan_id);
