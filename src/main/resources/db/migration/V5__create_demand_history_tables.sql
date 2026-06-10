CREATE SEQUENCE IF NOT EXISTS seq_order_demand
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS order_demands
(
    id               BIGINT       NOT NULL DEFAULT nextval('seq_order_demand'),
    warehouse_id     BIGINT       NOT NULL,
    order_number     VARCHAR(100) NOT NULL,
    order_date_time  TIMESTAMP    NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_order_demands PRIMARY KEY (id),

    CONSTRAINT uk_order_demands_warehouse_order_number
        UNIQUE (warehouse_id, order_number),

    CONSTRAINT fk_order_demands_warehouse
        FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id)
);

ALTER SEQUENCE seq_order_demand OWNED BY order_demands.id;

CREATE INDEX IF NOT EXISTS idx_order_demands_warehouse_date
    ON order_demands (warehouse_id, order_date_time);

CREATE INDEX IF NOT EXISTS idx_order_demands_order_number
    ON order_demands (order_number);


CREATE SEQUENCE IF NOT EXISTS seq_order_demand_item
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS order_demand_items
(
    id               BIGINT  NOT NULL DEFAULT nextval('seq_order_demand_item'),
    warehouse_id     BIGINT  NOT NULL,
    order_demand_id  BIGINT  NOT NULL,
    article_id       BIGINT  NOT NULL,
    quantity         INTEGER NOT NULL,
    version          BIGINT  NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_order_demand_items PRIMARY KEY (id),

    CONSTRAINT uk_order_demand_items_order_article
        UNIQUE (order_demand_id, article_id),

    CONSTRAINT fk_order_demand_items_warehouse
        FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id),

    CONSTRAINT fk_order_demand_items_order
        FOREIGN KEY (order_demand_id)
        REFERENCES order_demands (id),

    CONSTRAINT fk_order_demand_items_article
        FOREIGN KEY (article_id)
        REFERENCES articles (id),

    CONSTRAINT chk_order_demand_items_quantity_positive
        CHECK (quantity > 0)
);

ALTER SEQUENCE seq_order_demand_item OWNED BY order_demand_items.id;

CREATE INDEX IF NOT EXISTS idx_order_demand_items_article
    ON order_demand_items (article_id);

CREATE INDEX IF NOT EXISTS idx_order_demand_items_warehouse_article
    ON order_demand_items (warehouse_id, article_id);
