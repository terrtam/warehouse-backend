# Warehouse Backend

## Customer Sync Guide for Consumer Services

Use this sequence to keep a consumer read model in sync with `/api/customers` and `/topic/customers`.

1. Initial fetch:
   - Call `GET /api/customers?page=0&size=200` and keep paging until done.
   - Sort is by `updatedAt` ascending by default.
   - Persist records and store a checkpoint timestamp equal to the max `updatedAt` seen.
2. Live updates:
   - Open STOMP websocket connection to `/ws`.
   - Subscribe to `/topic/customers`.
   - Handle events with schema:
     - `eventType`: `CUSTOMER_CREATED` or `CUSTOMER_UPDATED`
     - `customer`: full `CustomerDto`
     - `occurredAt`: ISO-8601 timestamp
   - Upsert `customer` by `id`.
   - Move checkpoint to `max(checkpoint, customer.updatedAt)`.
3. Reconnect replay:
   - On disconnect/reconnect, call `GET /api/customers?updatedAfter=<checkpoint>&page=0&size=200`.
   - Apply returned rows in order and advance checkpoint.
   - Re-subscribe to `/topic/customers`.
4. Idempotency:
   - Use `id` as primary key and `version` for last-write-wins checks.
   - Ignore stale rows/events where incoming `version` is lower than stored `version`.
