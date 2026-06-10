# Warehouse Placement Optimizer

Warehouse Placement Optimizer is a Java/Spring Boot web application for warehouse layout generation, container receiving, putaway operations, and learned demand forecasting.

The main goal of the project is to reduce warehouse picking and placement time by organizing pallet storage more intelligently. The current version implements warehouse setup, operator putaway, relocation recommendations, and Java-based ML demand prediction.

---

## Project Goal

The application is designed to help warehouse operators decide where to place received goods in a warehouse.

The long-term goal is to recommend storage places based on:

* distance from the warehouse entry point;
* available storage place dimensions and weight limits;
* article popularity;
* demand prediction;
* seasonality;
* historical usage frequency;
* warehouse layout constraints.

The current version focuses on the basic foundation:

* creating a warehouse layout;
* creating articles;
* receiving containers/pallets;
* placing containers into storage places;
* merging compatible containers;
* tracking storage place availability.

---

## Current Development Status

Implemented:

* role-based access base;
* warehouse layout generation;
* article management;
* container receiving;
* manual container placement;
* container merging;
* container removal;
* storage place status updates;
* merge-first placement recommendations;
* placement recommendation approval, rejection, and expiration;
* demand history import;
* seasonal and recency-weighted demand scoring;
* Tribuo CART demand forecast training and model versioning;
* scheduled retraining with chronological validation and baseline protection;
* scheduled warehouse optimization assessments;
* draft optimization plans with merge, move, and buffered swap steps;
* scan-validated relocation execution;
* immutable container movement history;
* Flyway database migrations.

Not implemented yet:

* order picking flow;
* advanced warehouse layout types;
* real UI/browser scanner interface.

---

## Technology Stack

* Java 26
* Spring Boot
* Spring Web
* Spring Security
* Spring Data JPA
* Hibernate
* PostgreSQL
* Flyway
* Tribuo 4.3.2
* Maven
* Docker Compose

---

## Main Roles

The system currently uses three main roles:

### ROOT_ADMIN

Root administrator has the highest access level.

Current responsibilities:

* create warehouse layout;
* manage admin/operator accounts;
* access admin functionality;
* access operator functionality.

### ADMIN

Warehouse administrator.

Current responsibilities:

* manage articles;
* perform container-related actions when needed;
* support operators in case of incorrect data or operational mistakes.

### OPERATOR

Warehouse operator.

Current responsibilities:

* receive containers;
* place containers into storage places;
* merge containers;
* work with warehouse putaway flow.

---

## Main Modules

### `account`

Responsible for user accounts and roles.

Contains:

* `User`
* `Role`
* `Status`
* account-related controller/service/repository logic

---

### `auth`

Responsible for authentication-related support logic.

Contains:

* `SecurityConfig`
* `AuthenticatedUserService`
* password configuration

The application currently uses HTTP Basic authentication.

---

### `warehouse`

Responsible for warehouse creation and layout generation.

Main entities:

* `Warehouse`
* `Aisle`
* `RackRow`
* `RackBay`
* `RackLevel`
* `StoragePlace`

Main enum types:

* `WarehouseLayoutType`
* `WarehouseStatus`
* `StoragePlaceStatus`

---

### `putaway.article`

Responsible for article catalog management.

Main entity:

* `Article`

Main enum:

* `UnitType`

Article represents a product type or SKU-like catalog item.

Example:

```text
articleNumber = 100245
name = Cardboard Box M
unitType = PCS
maxQuantityPerPallet = 24
```

---

### `putaway.container`

Responsible for physical received containers/pallets.

Main entity:

* `Container`

Main enum:

* `ContainerStatus`

Container represents a specific physical pallet/container received into the warehouse.

Example:

```text
containerNumber = CONT-001
articleNumber = 100245
quantity = 10
weightKg = 12.000
heightMm = 644
status = WAITING_FOR_PLACEMENT
```

---

## Warehouse Layout Model

The current MVP supports only one warehouse layout type:

```java
MAIN_CORRIDOR_ONE_SIDE_AISLES
```

This means:

* the warehouse has one main corridor;
* aisles are generated only on one side of the main corridor;
* the current implementation assumes a simplified front pallet rack warehouse;
* more complex warehouse layouts will be added later.

---

## Warehouse Structure

The warehouse is generated using the following hierarchy:

```text
Warehouse
└── Aisle
    └── RackRow
        └── RackBay
            └── RackLevel
                └── StoragePlace
```

### Warehouse

Represents the whole warehouse.

Important fields:

* `code`
* `name`
* `layoutType`
* `status`
* `createdBy`
* `aisles`

---

### Aisle

Represents an aisle branching from the main corridor.

Important fields:

* `code`
* `sequenceNumber`
* `widthMm`
* `lengthMm`
* `entryXMm`
* `entryYMm`
* `distanceFromEntryMm`

---

### RackRow

Represents a rack row connected to an aisle.

Important fields:

* `code`
* `sequenceNumber`
* `aisle`
* `rackBays`

---

### RackBay

Represents one bay/span inside a rack row.

Important fields:

* `code`
* `bayNumber`
* `positionsPerLevel`
* `beamLengthMm`
* `maxBayLoadKg`
* `accessXMm`
* `accessYMm`
* `distanceFromAisleStartMm`

---

### RackLevel

Represents a vertical level inside a rack bay.

Important fields:

* `code`
* `levelNumber`
* `clearHeightMm`
* `heightFromFloorMm`
* `maxLevelLoadKg`

---

### StoragePlace

Represents one exact pallet place.

Important fields:

* `code`
* `positionNumber`
* `maxWeightKg`
* `maxHeightMm`
* `accessXMm`
* `accessYMm`
* `distanceFromAisleStartMm`
* `distanceFromEntryMm`
* `status`

Current statuses:

```java
AVAILABLE
OCCUPIED
```

---

## Current Warehouse Setup Rules

The warehouse is created from a simplified template.

Admin provides:

* warehouse code;
* warehouse name;
* layout type;
* aisle count;
* rack row count;
* bays per rack row;
* pallet places per level;
* aisle width;
* level profiles;
* max bay load.

A level profile contains:

* level number;
* clear height;
* max cell load.

Example:

```json
{
  "levelNumber": 1,
  "clearHeightMm": 2100,
  "maxCellLoadKg": 800
}
```

---

## Current Warehouse Simplifications

The current MVP intentionally simplifies warehouse structure.

Current assumptions:

* only euro pallets are used;
* only front pallet rack logic is supported;
* only one main corridor with aisles on one side is supported;
* storage place dimensions are generated from template values;
* all rack rows use the same number of bays;
* all bays use the same number of pallet places per level;
* all level profiles are applied uniformly across the whole warehouse;
* one storage place can contain only one container;
* one storage place cannot mix different articles;
* storage place status is only `AVAILABLE` or `OCCUPIED`;
* advanced equipment validation is not fully implemented yet;
* different rack types are not implemented yet;
* left-side aisles and two-sided main corridor layouts are not implemented yet.

---

## Coordinate System

The system stores basic coordinates for future route and time calculation.

Current coordinate idea:

```text
X = movement along the main corridor
Y = movement inside the aisle
```

Stored coordinates:

* `Aisle.entryXMm`
* `Aisle.entryYMm`
* `RackBay.accessXMm`
* `RackBay.accessYMm`
* `StoragePlace.accessXMm`
* `StoragePlace.accessYMm`

Distance is currently calculated as a simplified Manhattan distance:

```text
distanceFromEntryMm = abs(accessXMm) + abs(accessYMm)
```

This is a base for future placement recommendations and picking time estimation.

---

## Article Model

Article represents a product catalog item.

Important fields:

* `articleNumber`
* `name`
* `unitType`
* `unitWidthMm`
* `unitLengthMm`
* `unitHeightMm`
* `unitWeightKg`
* `maxQuantityPerPallet`

### Article Rules

* `articleNumber` is unique.
* `articleNumber` is immutable.
* `articleNumber` is stored as `String`, even if it contains only digits.
* Article can be deleted only if no container has ever been created for it.
* If at least one container references the article, the article cannot be deleted.
* Article update does not allow changing `articleNumber`.

This prevents breaking container history and future analytics.

---

## Container Model

Container represents a physical pallet/container received into the warehouse.

Important fields:

* `containerNumber`
* `warehouse`
* `article`
* `quantity`
* `weightKg`
* `heightMm`
* `currentStoragePlace`
* `status`
* `mergedIntoContainer`
* `receivedAt`

Current statuses:

```java
WAITING_FOR_PLACEMENT
STORED
MERGED
REMOVED
```

### Container Status Meaning

#### `WAITING_FOR_PLACEMENT`

The container was received into the system but has not been placed into a storage place yet.

#### `STORED`

The container is currently stored in a storage place.

#### `MERGED`

The container was merged into another container.

The original container is not deleted. It keeps a reference to the target container through `mergedIntoContainer`.

#### `REMOVED`

The container was removed from storage.

This can represent that the goods were used, removed, written off, or no longer stored in the warehouse.

---

## Container Placement Rules

A container can be placed into a storage place only if:

* the container status is `WAITING_FOR_PLACEMENT`;
* the storage place status is `AVAILABLE`;
* the container belongs to the same warehouse;
* the container weight does not exceed storage place max weight;
* the container height does not exceed storage place max height.

After successful placement:

* container status becomes `STORED`;
* container gets `currentStoragePlace`;
* storage place status becomes `OCCUPIED`.

---

## Container Merge Rules

Container merge is used when a newly received container can be combined with an already stored container of the same article.

Merge rules:

* source container must be `WAITING_FOR_PLACEMENT`;
* target container must be `STORED`;
* both containers must belong to the same warehouse;
* both containers must have the same article;
* target container must have enough remaining quantity capacity;
* resulting container must still fit into its current storage place by weight and height.

After successful merge:

* target container quantity is increased;
* target container weight is recalculated;
* target container height is recalculated;
* source container status becomes `MERGED`;
* source container stores reference to target container.

The source container is not physically deleted.

---

## Current API Overview

### Warehouse

Create warehouse:

```http
POST /admin/warehouses
```

---

### Article

Create article:

```http
POST /admin/articles
```

Get all articles:

```http
GET /admin/articles
```

Get article by id:

```http
GET /admin/articles/{id}
```

Get article by article number:

```http
GET /admin/articles/number/{articleNumber}
```

Update article:

```http
PATCH /admin/articles/{id}
```

Delete article:

```http
DELETE /admin/articles/{id}
```

Article deletion is allowed only if the article is not used by any container.

---

### Container

Receive container:

```http
POST /operator/containers/receive
```

Get all containers:

```http
GET /operator/containers
```

Get container by number:

```http
GET /operator/containers/{containerNumber}
```

Update container manually:

```http
PATCH /operator/containers/{containerNumber}
```

Place container:

```http
POST /operator/containers/{containerNumber}/place
```

Merge container:

```http
POST /operator/containers/{containerNumber}/merge
```

Remove container:

```http
PATCH /operator/containers/{containerNumber}/remove
```

---

## Database Migrations

Current Flyway migrations:

```text
V1__create_users_table.sql
V2__create_warehouse_layout_tables.sql
V3__create_putaway_tables.sql
V4__create_placement_recommendations_table.sql
V5__create_demand_history_tables.sql
V6__add_placement_recommendation_expiration.sql
V7__create_warehouse_optimization_assessments.sql
V8__create_warehouse_optimization_plans.sql
V9__create_container_movements.sql
V10__create_demand_forecast_models.sql
```

### V1

Creates user-related tables.

### V2

Creates warehouse layout tables:

* `warehouses`
* `aisles`
* `rack_rows`
* `rack_bays`
* `rack_levels`
* `storage_places`

### V3

Creates putaway tables:

* `articles`
* `containers`

### V4

Creates placement recommendation storage.

### V5

Creates demand history tables:

* `order_demands`
* `order_demand_items`

### V6

Adds recommendation expiration and active reservation constraints.

### V7-V9

Create optimization assessments, relocation plans, scan-validated steps, and immutable container movement history.

### V10

Creates versioned demand forecast model storage, validation metrics, and the serialized Tribuo model artifact.

---

## Local Development Notes

The project uses Flyway for database schema management.

Before starting with a new database, configure the initial root administrator:

```bash
export APP_ADMIN_EMAIL=admin@example.com
export APP_ADMIN_PASSWORD='replace-with-a-strong-password'
export APP_ADMIN_FULL_NAME='System Admin'
```

`APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD` are required only while the database
does not contain a `ROOT_ADMIN`. Placement recommendations expire after 15 minutes
by default. Override this with `PLACEMENT_RECOMMENDATION_TTL_MINUTES`.

For local testing, do not manually delete `flyway_schema_history`.

To clear test warehouse and putaway data while keeping Flyway history and users:

```sql
TRUNCATE TABLE
    containers,
    articles,
    storage_places,
    rack_levels,
    rack_bays,
    rack_rows,
    aisles,
    warehouses
RESTART IDENTITY CASCADE;
```

Do not truncate:

```text
flyway_schema_history
users
```

unless the database is being fully reset.

For a full local reset:

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;
```

After that, Flyway will recreate the schema from migrations.

---

## Example Test Flow

Current tested flow:

1. Create warehouse.
2. Create articles.
3. Receive container.
4. Place container into storage place.
5. Receive another container with the same article.
6. Merge waiting container into stored container.
7. Try invalid merge when max pallet quantity is exceeded.
8. Try invalid placement into occupied storage place.
9. Remove stored container.
10. Verify storage place becomes available again.

---

## Current Limitations

Current implementation does not yet support:

* order picking optimization;
* learning from measured picking duration;
* obstacle-aware graph routing between storage places;
* multiple containers in one storage place;
* mixed articles in one storage place;
* multiple warehouse layout strategies;
* detailed equipment-based aisle validation;
* expiry dates or batch numbers;
* full WMS functionality.

---

## Warehouse Optimization Workflow

The optimizer uses a hybrid demand model. A validated Tribuo CART regression model
predicts article demand for the next 14 completed days. Articles without enough
history and warehouses without an active ML model automatically use the explainable
recency and seasonality baseline.

Default thresholds:

* below `60%`: optimization is recommended;
* `85%` or higher: relocation planning stops;
* the thresholds are configurable through environment variables.

Workflow:

1. The scheduled analyzer evaluates active warehouses every day at `02:00`.
2. An administrator creates a draft plan from an assessment below the threshold.
3. The planner first consolidates compatible partial pallets.
4. It then creates direct moves or three-step swaps through an available buffer place.
5. An administrator approves the plan.
6. An operator scans the expected source container and target place or merge container.
7. Each completed step updates inventory and appends an immutable movement record.

Main endpoints:

```text
POST /admin/warehouses/{warehouseId}/optimization-assessments
GET  /admin/warehouses/{warehouseId}/optimization-assessments/latest
POST /admin/optimization-plans/assessments/{assessmentId}
POST /admin/optimization-plans/{planCode}/approve
GET  /operator/optimization-plans/{planCode}/steps/current
POST /operator/optimization-plans/{planCode}/steps/current/complete
GET  /admin/container-movements?warehouseId={warehouseId}
```

The next modeling stage is to record real picking operations and use measured route
duration to train and validate travel-time models.

---

## Demand Forecast Training

The training dataset is built from `order_demand_items`. Every article is converted
into a continuous daily series, so days without orders are represented as zero demand.

Current features include:

* quantity lags for 1, 7, 14, and 28 days;
* rolling quantity totals and means for 7, 28, and 90 days;
* active demand days and order count;
* days since the last demand and article history age;
* short-term versus long-term trend;
* cyclic day-of-week and day-of-year seasonality.

The target is the total article quantity ordered during the following 14 days. The
last 60 days are reserved for chronological validation. A gap equal to the forecast
horizon prevents training labels from overlapping the validation period.

The candidate model is compared with a rolling 28-day mean baseline. It becomes
`ACTIVE` only when validation MAE improves by at least 2% by default. Otherwise it is
stored as `REJECTED`, and the previous active model remains in use.

Retraining rules:

* the scheduler checks active warehouses every day at `03:30`;
* training uses completed days only;
* normal retraining cannot happen more often than every 30 days;
* 200 newly imported order item observations can trigger retraining after that interval;
* retraining is forced after 90 days;
* a failed or rejected warehouse is retried after the minimum interval;
* a training attempt left stale for 24 hours can be retried.

All thresholds and intervals are configurable with `DEMAND_FORECAST_*` environment
variables.

### Postman Requests

Use Basic Auth with an `ADMIN` or `ROOT_ADMIN` account. These requests do not require
a JSON body.

Train manually:

```http
POST http://localhost:8080/admin/warehouses/1/demand-forecast-models/train
```

Get the latest training attempt:

```http
GET http://localhost:8080/admin/warehouses/1/demand-forecast-models/latest
```

Get all model versions and their metrics:

```http
GET http://localhost:8080/admin/warehouses/1/demand-forecast-models
```

Manual training returns `400 Bad Request` when there are fewer than the configured
training or validation samples. Import more historical orders before retrying.

### Full Postman AI Cycle

Import these files into Postman:

```text
postman/warehouse-optimization-ai-cycle.postman_collection.json
postman/warehouse-optimization-ai-local.postman_environment.json
```

Set the correct root administrator credentials in the imported environment. Start the
application with the local Postman profile:

```bash
SPRING_PROFILES_ACTIVE=postman ./mvnw spring-boot:run
```

The profile checks warehouse optimization every 10 seconds and demand model training
every 15 seconds. These fast schedules are test-only and do not change the default
production schedule.

Run the entire collection through Postman Collection Runner, not by sending only one
request. Runner is required because the collection loops through all container
placements and every relocation step automatically.

The full cycle creates:

* a warehouse with 576 storage places;
* 12 articles and 16 physical containers;
* 540 days, 540 orders, and 6480 order items;
* a trained Tribuo model with validation metrics;
* an intentionally inefficient initial placement;
* an optimization assessment and relocation plan;
* scan-compatible execution of all relocation steps;
* a final assessment and immutable movement audit history.
