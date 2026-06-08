CREATE SEQUENCE IF NOT EXISTS seq_warehouse
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS warehouses
(
    id                  BIGINT       NOT NULL DEFAULT nextval('seq_warehouse'),
    code                VARCHAR(100) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    layout_type         VARCHAR(80)  NOT NULL,
    status              VARCHAR(50)  NOT NULL,
    created_by_user_id  BIGINT       NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_warehouses PRIMARY KEY (id),

    CONSTRAINT uk_warehouses_code
    UNIQUE (code),

    CONSTRAINT fk_warehouses_created_by_user
    FOREIGN KEY (created_by_user_id)
    REFERENCES users (id)
    );

ALTER SEQUENCE seq_warehouse OWNED BY warehouses.id;


CREATE SEQUENCE IF NOT EXISTS seq_aisle
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS aisles
(
    id                      BIGINT      NOT NULL DEFAULT nextval('seq_aisle'),
    warehouse_id            BIGINT      NOT NULL,
    code                    VARCHAR(50) NOT NULL,
    sequence_number         INTEGER     NOT NULL,
    width_mm                INTEGER     NOT NULL,
    length_mm               INTEGER     NOT NULL,
    entry_x_mm              INTEGER     NOT NULL,
    entry_y_mm              INTEGER     NOT NULL,
    distance_from_entry_mm  INTEGER     NOT NULL,
    version                 BIGINT      NOT NULL DEFAULT 0,
    created_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_aisles PRIMARY KEY (id),

    CONSTRAINT fk_aisles_warehouse
    FOREIGN KEY (warehouse_id)
    REFERENCES warehouses (id),

    CONSTRAINT uk_aisles_warehouse_code
    UNIQUE (warehouse_id, code),

    CONSTRAINT uk_aisles_warehouse_sequence
    UNIQUE (warehouse_id, sequence_number),

    CONSTRAINT chk_aisles_width_positive
    CHECK (width_mm > 0),

    CONSTRAINT chk_aisles_length_positive
    CHECK (length_mm > 0),

    CONSTRAINT chk_aisles_distance_from_entry_not_negative
    CHECK (distance_from_entry_mm >= 0)
    );

ALTER SEQUENCE seq_aisle OWNED BY aisles.id;


CREATE SEQUENCE IF NOT EXISTS seq_rack_row
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS rack_rows
(
    id               BIGINT      NOT NULL DEFAULT nextval('seq_rack_row'),
    warehouse_id     BIGINT      NOT NULL,
    aisle_id         BIGINT      NOT NULL,
    code             VARCHAR(50) NOT NULL,
    sequence_number  INTEGER     NOT NULL,
    version          BIGINT      NOT NULL DEFAULT 0,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_rack_rows PRIMARY KEY (id),

    CONSTRAINT fk_rack_rows_warehouse
    FOREIGN KEY (warehouse_id)
    REFERENCES warehouses (id),

    CONSTRAINT fk_rack_rows_aisle
    FOREIGN KEY (aisle_id)
    REFERENCES aisles (id),

    CONSTRAINT uk_rack_rows_warehouse_code
    UNIQUE (warehouse_id, code),

    CONSTRAINT uk_rack_rows_warehouse_sequence
    UNIQUE (warehouse_id, sequence_number),

    CONSTRAINT chk_rack_rows_sequence_positive
    CHECK (sequence_number > 0)
    );

ALTER SEQUENCE seq_rack_row OWNED BY rack_rows.id;


CREATE SEQUENCE IF NOT EXISTS seq_rack_bay
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS rack_bays
(
    id                             BIGINT      NOT NULL DEFAULT nextval('seq_rack_bay'),
    warehouse_id                   BIGINT      NOT NULL,
    rack_row_id                    BIGINT      NOT NULL,
    code                           VARCHAR(50) NOT NULL,
    bay_number                     INTEGER     NOT NULL,
    positions_per_level            INTEGER     NOT NULL,
    beam_length_mm                 INTEGER     NOT NULL,
    max_bay_load_kg                INTEGER     NOT NULL,
    access_x_mm                    INTEGER     NOT NULL,
    access_y_mm                    INTEGER     NOT NULL,
    distance_from_aisle_start_mm   INTEGER     NOT NULL,
    version                        BIGINT      NOT NULL DEFAULT 0,
    created_at                     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_rack_bays PRIMARY KEY (id),

    CONSTRAINT fk_rack_bays_warehouse
    FOREIGN KEY (warehouse_id)
    REFERENCES warehouses (id),

    CONSTRAINT fk_rack_bays_rack_row
    FOREIGN KEY (rack_row_id)
    REFERENCES rack_rows (id),

    CONSTRAINT uk_rack_bays_row_code
    UNIQUE (rack_row_id, code),

    CONSTRAINT uk_rack_bays_row_bay_number
    UNIQUE (rack_row_id, bay_number),

    CONSTRAINT chk_rack_bays_bay_number_positive
    CHECK (bay_number > 0),

    CONSTRAINT chk_rack_bays_positions_per_level_positive
    CHECK (positions_per_level > 0),

    CONSTRAINT chk_rack_bays_beam_length_positive
    CHECK (beam_length_mm > 0),

    CONSTRAINT chk_rack_bays_max_load_positive
    CHECK (max_bay_load_kg > 0),

    CONSTRAINT chk_rack_bays_distance_from_aisle_start_not_negative
    CHECK (distance_from_aisle_start_mm >= 0)
    );

ALTER SEQUENCE seq_rack_bay OWNED BY rack_bays.id;


CREATE SEQUENCE IF NOT EXISTS seq_rack_level
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS rack_levels
(
    id                    BIGINT      NOT NULL DEFAULT nextval('seq_rack_level'),
    warehouse_id          BIGINT      NOT NULL,
    rack_bay_id           BIGINT      NOT NULL,
    code                  VARCHAR(50) NOT NULL,
    level_number          INTEGER     NOT NULL,
    clear_height_mm       INTEGER     NOT NULL,
    height_from_floor_mm  INTEGER     NOT NULL,
    max_level_load_kg     INTEGER     NOT NULL,
    version               BIGINT      NOT NULL DEFAULT 0,
    created_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_rack_levels PRIMARY KEY (id),

    CONSTRAINT fk_rack_levels_warehouse
    FOREIGN KEY (warehouse_id)
    REFERENCES warehouses (id),

    CONSTRAINT fk_rack_levels_rack_bay
    FOREIGN KEY (rack_bay_id)
    REFERENCES rack_bays (id),

    CONSTRAINT uk_rack_levels_bay_code
    UNIQUE (rack_bay_id, code),

    CONSTRAINT uk_rack_levels_bay_level_number
    UNIQUE (rack_bay_id, level_number),

    CONSTRAINT chk_rack_levels_level_number_positive
    CHECK (level_number > 0),

    CONSTRAINT chk_rack_levels_clear_height_positive
    CHECK (clear_height_mm > 0),

    CONSTRAINT chk_rack_levels_height_from_floor_not_negative
    CHECK (height_from_floor_mm >= 0),

    CONSTRAINT chk_rack_levels_max_load_positive
    CHECK (max_level_load_kg > 0)
    );

ALTER SEQUENCE seq_rack_level OWNED BY rack_levels.id;


CREATE SEQUENCE IF NOT EXISTS seq_storage_place
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS storage_places
(
    id                              BIGINT      NOT NULL DEFAULT nextval('seq_storage_place'),
    warehouse_id                    BIGINT      NOT NULL,
    rack_row_id                     BIGINT      NOT NULL,
    rack_bay_id                     BIGINT      NOT NULL,
    rack_level_id                   BIGINT      NOT NULL,
    code                            VARCHAR(50) NOT NULL,
    position_number                 INTEGER     NOT NULL,
    max_weight_kg                   INTEGER     NOT NULL,
    max_height_mm                   INTEGER     NOT NULL,
    access_x_mm                     INTEGER     NOT NULL,
    access_y_mm                     INTEGER     NOT NULL,
    distance_from_aisle_start_mm    INTEGER     NOT NULL,
    distance_from_entry_mm          INTEGER     NOT NULL,
    status                          VARCHAR(50) NOT NULL,
    version                         BIGINT      NOT NULL DEFAULT 0,
    created_at                      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_storage_places PRIMARY KEY (id),

    CONSTRAINT fk_storage_places_warehouse
    FOREIGN KEY (warehouse_id)
    REFERENCES warehouses (id),

    CONSTRAINT fk_storage_places_rack_row
    FOREIGN KEY (rack_row_id)
    REFERENCES rack_rows (id),

    CONSTRAINT fk_storage_places_rack_bay
    FOREIGN KEY (rack_bay_id)
    REFERENCES rack_bays (id),

    CONSTRAINT fk_storage_places_rack_level
    FOREIGN KEY (rack_level_id)
    REFERENCES rack_levels (id),

    CONSTRAINT uk_storage_places_warehouse_code
    UNIQUE (warehouse_id, code),

    CONSTRAINT uk_storage_places_level_position
    UNIQUE (rack_level_id, position_number),

    CONSTRAINT chk_storage_places_position_positive
    CHECK (position_number > 0),

    CONSTRAINT chk_storage_places_max_weight_positive
    CHECK (max_weight_kg > 0),

    CONSTRAINT chk_storage_places_max_height_positive
    CHECK (max_height_mm > 0),

    CONSTRAINT chk_storage_places_distance_from_aisle_start_not_negative
    CHECK (distance_from_aisle_start_mm >= 0),

    CONSTRAINT chk_storage_places_distance_from_entry_not_negative
    CHECK (distance_from_entry_mm >= 0)
    );

ALTER SEQUENCE seq_storage_place OWNED BY storage_places.id;