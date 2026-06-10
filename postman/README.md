# Postman API checks

The collection covers the current end-to-end workflow:

1. Authentication and role-based access.
2. Operator, warehouse, and article setup.
3. PLACE recommendation with an invalid and a valid scan.
4. MERGE recommendation, rejection, fallback, and approval.
5. Batch receiving and demand-history idempotency.
6. Structured `400`, `401`, `403`, and `404` API errors.

## Start the application

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Configure the local application and bootstrap administrator:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/warehouse_optimizer
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export APP_ADMIN_EMAIL=admin@example.com
export APP_ADMIN_PASSWORD=local-root-password
export APP_ADMIN_FULL_NAME='Local Root Admin'
./mvnw spring-boot:run
```

The credentials above are local examples only.

## Run in Postman

Import:

* `warehouse-placement-optimizer.postman_collection.json`
* `local.postman_environment.json`

Select the `Warehouse Placement Optimizer - Local` environment and run the
whole collection in its defined order. The first request creates a fresh run
identifier and resets all generated collection variables, so complete runs can
be repeated without changing request bodies manually.

Individual workflow requests depend on variables saved by earlier requests.
For a manual run, always start with `00 Access and session / Initialize run`.

## Run with Newman

The collection is compatible with Newman and can later be placed in CI:

```bash
newman run postman/warehouse-placement-optimizer.postman_collection.json \
  --environment postman/local.postman_environment.json \
  --bail
```

The collection creates test data but does not delete it. Generated identifiers
are unique for every complete run.
