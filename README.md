# SQL Query Explainer + Optimizer

A runnable Spring Boot backend that runs `EXPLAIN ANALYZE` against a user-supplied PostgreSQL database, parses the JSON plan, detects likely bottlenecks, and asks Anthropic Claude to explain the plan and suggest query or index improvements.

## Prerequisites

- Java 17
- Maven
- Docker, optional but recommended for the demo database
- `ANTHROPIC_API_KEY`

## Start the Demo Database

```bash
docker compose up -d
```

The compose file starts Postgres 16 on port `5432` with database `demo`, username `demo`, and password `demo`.

The seed data creates:

- `customers`: 5,000 rows
- `orders`: 300,000 rows

`orders.customer_id` is deliberately not indexed, so this demo query should produce a sequential scan bottleneck:

```sql
SELECT * FROM orders WHERE customer_id = 42
```

## Run the API

```bash
export ANTHROPIC_API_KEY=your_key_here
mvn spring-boot:run
```

On Windows PowerShell:

```powershell
$env:ANTHROPIC_API_KEY = "your_key_here"
mvn spring-boot:run
```

The API runs on `http://localhost:8080`.

The app stores its own history in a local file-based H2 database at `./sql-explainer-history`. The target Postgres connection is supplied per request and is only used for analysis.

## Analyze a Query

```bash
curl -X POST http://localhost:8080/api/v1/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "connection": {
      "host": "localhost",
      "port": 5432,
      "database": "demo",
      "username": "demo",
      "password": "demo",
      "sslMode": "disable"
    },
    "sql": "SELECT * FROM orders WHERE customer_id = 42",
    "allowNonSelect": false,
    "includeBuffers": true
  }'
```

Response fields include `id`, `sql`, `planningTimeMs`, `executionTimeMs`, a recursive `planTree`, `bottlenecks`, `explanation`, `suggestions`, and `createdAt`.

## History Endpoints

```bash
curl "http://localhost:8080/api/v1/history?page=0&size=20"
curl "http://localhost:8080/api/v1/history/1"
curl -X DELETE "http://localhost:8080/api/v1/history/1"
```

## Rollback Safety Model

`EXPLAIN ANALYZE` actually executes the query. To reduce risk, the backend opens the target JDBC connection with `autoCommit(false)`, runs `SET LOCAL statement_timeout = '30s'`, executes the explain, and always rolls back in a `finally` block.

The validator rejects empty SQL, multiple statements, and dangerous DDL/admin operations including `DROP`, `TRUNCATE`, `ALTER`, `GRANT`, `REVOKE`, `VACUUM`, and `CREATE EXTENSION`.

By default only `SELECT` and `WITH` queries are accepted. Setting `allowNonSelect` to `true` permits other statement types, but triggers and other side effects can still fire during execution even though the transaction is rolled back.

## CORS

CORS is open on `/api/**`, so a future VS Code extension webview or standalone web UI can call this backend directly without further backend changes.
