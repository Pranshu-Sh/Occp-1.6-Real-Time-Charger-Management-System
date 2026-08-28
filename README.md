# OCPP 1.6 Real-Time Charger Management System

A Spring Boot Central System (CSMS) implementing the OCPP 1.6-J Core Profile over WebSocket:
chargers connect, boot, heartbeat, report status, authorize tags, and run charging
transactions (Start/Stop/MeterValues) against this server.

## Architecture

```
Charge Point ──(WebSocket, HTTP Basic Auth)──> OcppHandshakeInterceptor
                                                       │
                                              OcppWebSocketConnectionHandler
                                              (1 dedicated thread per session,
                                               guarantees in-order processing)
                                                       │
                                              OcppMessageProcessor
                                              (parses CALL frames, always answers
                                               with a CallResult or CallError)
                                                       │
                                          service/ (business logic) ── repository/ (Spring Data JPA)
                                                       │
                                                     MySQL (schema managed by Flyway)
```

REST admin API (`/api/chargers`, `/api/tags`) is JWT-secured and separate from the OCPP
WebSocket, which uses its own per-charger Basic Auth (see below).

## Running locally

```bash
cp .env.example .env
# edit .env: set DB_PASSWORD, JWT_SECRET, ADMIN_PASSWORD_HASH (see .env.example for how)
docker compose --env-file .env up --build
```

The app comes up on `http://localhost:9093`. Health check: `GET /actuator/health`.

Without Docker: start a local MySQL matching `.env`, export the same variables, then
`./gradlew bootRun --args='--spring.profiles.active=dev'`.

## Configuration

All configuration is environment-variable driven (see `.env.example`); `application.yml` has
no defaults for secrets, so the app fails fast on startup if they're missing. Config files:

| File | Purpose |
|---|---|
| `application.yml` | Shared config, all secrets via `${ENV_VAR}` |
| `application-dev.yml` | Local dev overrides (verbose SQL/error logging) |
| `application-prod.yml` | Production overrides (quiet logging, file output) |

Schema is managed by Flyway (`src/main/resources/db/migration/`), not Hibernate
auto-DDL - `ddl-auto` is `validate`, so a schema drift fails startup loudly instead of
silently mutating the database.

## Authentication

There are two independent auth mechanisms:

1. **REST admin API** - `POST /auth/login` with the configured admin username/password
   returns a JWT (1h expiry by default); pass it as `Authorization: Bearer <token>` on every
   other `/api/**` call. `/actuator/health` and `/actuator/info` are the only public endpoints;
   everything else, including Swagger UI, requires a token.
2. **OCPP WebSocket** - each charger authenticates the WebSocket upgrade with HTTP Basic Auth
   (`chargeBoxId:password`), per OCPP 1.6 Security Profile 1. Register a charger and set its
   password with:
   ```
   POST /api/chargers
   Content-Type: application/json

   {"charger": "CP001", "password": "<a strong per-charger password>"}
   ```
   A charger can only connect once it's been pre-registered this way; connecting without
   credentials, with the wrong password, or as an unregistered charger id is rejected before
   the WebSocket handshake completes.

## Testing

```bash
./gradlew test
```

Tests run against an in-memory H2 database (`application-test.yml`) - no external services
needed. Coverage includes: the atomic transaction-count guard and StartTransaction
idempotency, OCPP envelope/CallError handling, per-session message ordering, the WebSocket
handshake auth, and REST-layer auth rules.

## What this is (and isn't)

Implements: BootNotification, Heartbeat, StatusNotification, Authorize, StartTransaction,
StopTransaction, MeterValues, plus the charger/tag admin REST API.

Not implemented (server is currently receive-only - it never sends a command to a charger):
RemoteStartTransaction, RemoteStopTransaction, Reset, UnlockConnector, ChangeConfiguration,
GetConfiguration, ClearCache, DataTransfer. Also out of scope: multi-instance horizontal
scaling (current session/connection state is single-instance in-memory).
