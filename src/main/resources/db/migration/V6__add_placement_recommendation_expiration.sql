ALTER TABLE placement_recommendations
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

UPDATE placement_recommendations
SET expires_at = created_at + INTERVAL '15 minutes'
WHERE expires_at IS NULL;

ALTER TABLE placement_recommendations
    ALTER COLUMN expires_at SET DEFAULT (CURRENT_TIMESTAMP + INTERVAL '15 minutes'),
    ALTER COLUMN expires_at SET NOT NULL;

ALTER TABLE placement_recommendations
    DROP CONSTRAINT IF EXISTS chk_placement_recommendations_status;

ALTER TABLE placement_recommendations
    ADD CONSTRAINT chk_placement_recommendations_status
        CHECK (status IN ('SUGGESTED', 'ACCEPTED', 'REJECTED', 'EXPIRED'));

WITH duplicate_sources AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY source_container_id
               ORDER BY created_at DESC, id DESC
           ) AS row_number
    FROM placement_recommendations
    WHERE status = 'SUGGESTED'
)
UPDATE placement_recommendations
SET status = 'EXPIRED',
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    SELECT id
    FROM duplicate_sources
    WHERE row_number > 1
);

WITH duplicate_storage_places AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY recommended_storage_place_id
               ORDER BY created_at DESC, id DESC
           ) AS row_number
    FROM placement_recommendations
    WHERE status = 'SUGGESTED'
)
UPDATE placement_recommendations
SET status = 'EXPIRED',
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    SELECT id
    FROM duplicate_storage_places
    WHERE row_number > 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_placement_recommendations_suggested_source
    ON placement_recommendations (source_container_id)
    WHERE status = 'SUGGESTED';

CREATE UNIQUE INDEX IF NOT EXISTS uk_placement_recommendations_suggested_storage_place
    ON placement_recommendations (recommended_storage_place_id)
    WHERE status = 'SUGGESTED';

CREATE INDEX IF NOT EXISTS idx_placement_recommendations_status_expires_at
    ON placement_recommendations (status, expires_at);
