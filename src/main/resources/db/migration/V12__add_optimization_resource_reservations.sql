ALTER TABLE containers
    ADD COLUMN IF NOT EXISTS optimization_reservation_code VARCHAR(100);

ALTER TABLE storage_places
    ADD COLUMN IF NOT EXISTS optimization_reservation_code VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_containers_optimization_reservation
    ON containers (optimization_reservation_code)
    WHERE optimization_reservation_code IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_storage_places_optimization_reservation
    ON storage_places (optimization_reservation_code)
    WHERE optimization_reservation_code IS NOT NULL;
