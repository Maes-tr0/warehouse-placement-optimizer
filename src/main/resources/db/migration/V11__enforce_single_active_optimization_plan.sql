CREATE UNIQUE INDEX IF NOT EXISTS uk_optimization_plans_active_warehouse
    ON warehouse_optimization_plans (warehouse_id)
    WHERE status IN ('DRAFT', 'APPROVED', 'IN_PROGRESS');
