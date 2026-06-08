CREATE SEQUENCE IF NOT EXISTS seq_article
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS articles
(
    id                       BIGINT        NOT NULL DEFAULT nextval('seq_article'),
    article_number           VARCHAR(50)   NOT NULL,
    name                     VARCHAR(255)  NOT NULL,
    unit_type                VARCHAR(50)   NOT NULL,
    unit_width_mm            INTEGER       NOT NULL,
    unit_length_mm           INTEGER       NOT NULL,
    unit_height_mm           INTEGER       NOT NULL,
    unit_weight_kg           NUMERIC(10,3) NOT NULL,
    max_quantity_per_pallet  INTEGER       NOT NULL,
    version                  BIGINT        NOT NULL DEFAULT 0,
    created_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_articles PRIMARY KEY (id),

    CONSTRAINT uk_articles_article_number
    UNIQUE (article_number),

    CONSTRAINT chk_articles_unit_width_positive
    CHECK (unit_width_mm > 0),

    CONSTRAINT chk_articles_unit_length_positive
    CHECK (unit_length_mm > 0),

    CONSTRAINT chk_articles_unit_height_positive
    CHECK (unit_height_mm > 0),

    CONSTRAINT chk_articles_unit_weight_positive
    CHECK (unit_weight_kg > 0),

    CONSTRAINT chk_articles_max_quantity_per_pallet_positive
    CHECK (max_quantity_per_pallet > 0)
    );

ALTER SEQUENCE seq_article OWNED BY articles.id;


CREATE SEQUENCE IF NOT EXISTS seq_container
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS containers
(
    id                         BIGINT        NOT NULL DEFAULT nextval('seq_container'),
    container_number           VARCHAR(100)  NOT NULL,
    warehouse_id               BIGINT        NOT NULL,
    article_id                 BIGINT        NOT NULL,
    quantity                   INTEGER       NOT NULL,
    weight_kg                  NUMERIC(10,3) NOT NULL,
    height_mm                  INTEGER       NOT NULL,
    current_storage_place_id   BIGINT,
    status                     VARCHAR(50)   NOT NULL,
    merged_into_container_id   BIGINT,
    received_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                    BIGINT        NOT NULL DEFAULT 0,
    created_at                 TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_containers PRIMARY KEY (id),

    CONSTRAINT uk_containers_container_number
    UNIQUE (container_number),

    CONSTRAINT uk_containers_current_storage_place
    UNIQUE (current_storage_place_id),

    CONSTRAINT fk_containers_warehouse
    FOREIGN KEY (warehouse_id)
    REFERENCES warehouses (id),

    CONSTRAINT fk_containers_article
    FOREIGN KEY (article_id)
    REFERENCES articles (id),

    CONSTRAINT fk_containers_current_storage_place
    FOREIGN KEY (current_storage_place_id)
    REFERENCES storage_places (id),

    CONSTRAINT fk_containers_merged_into_container
    FOREIGN KEY (merged_into_container_id)
    REFERENCES containers (id),

    CONSTRAINT chk_containers_quantity_positive
    CHECK (quantity > 0),

    CONSTRAINT chk_containers_weight_positive
    CHECK (weight_kg > 0),

    CONSTRAINT chk_containers_height_positive
    CHECK (height_mm > 0)
    );

ALTER SEQUENCE seq_container OWNED BY containers.id;