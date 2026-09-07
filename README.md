# Backend

## Overview

Spring Boot backend that provides the Warehouse Management System (WMS) APIs, authentication, reporting, and realtime updates.

Repository:

- Frontend: [warehouse-frontend](https://github.com/terrtam/warehouse-frontend)

## Tech Stack

- Java 17
- Spring Boot (Web MVC, Security, Validation, WebSocket, Mail)
- Spring Data JPA + Hibernate
- PostgreSQL
- Flyway migrations
- Maven (wrapper)

## Architecture

- `controller` - REST endpoints
- `service` - business logic and workflows
- `repository` - JPA repositories
- `entity` - database entities
- `dto` - request/response models
- `event` + `realtime` - domain events and STOMP topic publishing
- `security` - JWT auth, filters, and access control
- `config` - Spring configuration (including WebSocket)
- `src/main/resources/db/migration` - Flyway SQL migrations

## Features

- JWT-based login endpoint
- Products, categories, customers, and suppliers management
- Sales and purchase order workflows
- Inventory adjustments and transaction history
- Reporting endpoints for sales, purchasing, supplier performance, velocity, and low-stock trends
- Audit log and communication log queries
- Realtime updates over WebSocket/STOMP

## API Endpoints

- `POST /auth/login`
- `GET /api/products`
- `GET /api/products/{id}`
- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`
- `GET /api/categories`
- `POST /api/categories`
- `PUT /api/categories/{id}`
- `GET /api/customers`
- `GET /api/customers/{id}`
- `POST /api/customers`
- `PUT /api/customers/{id}`
- `DELETE /api/customers/{id}`
- `GET /api/suppliers`
- `GET /api/suppliers/{id}`
- `POST /api/suppliers`
- `PUT /api/suppliers/{id}`
- `DELETE /api/suppliers/{id}`
- `GET /api/inventory`
- `POST /api/inventory/adjustments`
- `GET /api/inventory/transactions`
- `GET /api/sales-orders`
- `POST /api/sales-orders`
- `POST /api/sales-orders/{id}/confirm`
- `POST /api/sales-orders/{id}/ship`
- `POST /api/sales-orders/{id}/cancel`
- `GET /api/purchase-orders`
- `POST /api/purchase-orders`
- `POST /api/purchase-orders/{id}/order`
- `POST /api/purchase-orders/{id}/receive`
- `POST /api/purchase-orders/{id}/cancel`
- `GET /api/reports/sales-by-product`
- `GET /api/reports/sales-by-category`
- `GET /api/reports/purchase-cost-tracking`
- `GET /api/reports/supplier-performance`
- `GET /api/reports/velocity`
- `GET /api/reports/low-stock-trends`
- `GET /api/audit-log`
- `GET /api/communications`

## Environment Variables

```env
WMS_DB_URL=jdbc:postgresql://localhost:5432/warehouse
WMS_DB_USERNAME=postgres
WMS_DB_PASSWORD=your_database_password

WMS_JWT_SECRET=replace_with_at_least_32_ascii_characters
WMS_JWT_EXPIRATION_MS=3600000

WMS_COMMUNICATION_EMAIL_ENABLED=false

WMS_SMTP_HOST=localhost
WMS_SMTP_PORT=1025
WMS_SMTP_USERNAME=
WMS_SMTP_PASSWORD=
WMS_SMTP_AUTH=false
WMS_SMTP_STARTTLS_ENABLE=false

WMS_EMAIL_FROM=no-reply@warehouse.local
```

### AWS Production Variables

For ECS/Fargate production, set the same variables through task definition environment variables or, preferably, inject them from AWS Secrets Manager / SSM Parameter Store.

Recommended production mapping:

- `WMS_DB_URL` -> RDS PostgreSQL JDBC URL
- `WMS_DB_USERNAME` -> RDS master or app user
- `WMS_DB_PASSWORD` -> AWS secret value
- `WMS_JWT_SECRET` -> AWS secret value
- `WMS_COMMUNICATION_EMAIL_ENABLED` -> `true` only if SES/SMTP is configured
- `WMS_SMTP_HOST` / `WMS_SMTP_PORT` / `WMS_SMTP_USERNAME` / `WMS_SMTP_PASSWORD` / `WMS_SMTP_AUTH` / `WMS_SMTP_STARTTLS_ENABLE` -> SES SMTP or another mail provider
- `WMS_EMAIL_FROM` -> verified sender address

Recommended ECS runtime settings:

- Send app logs to CloudWatch Logs
- Expose container port `8080`
- Place the service behind an Application Load Balancer
- Allow security-group egress to RDS and SMTP endpoints
- Keep Flyway enabled so schema migrations apply on startup

## Setup Instructions

1. Ensure PostgreSQL is running and the database in `WMS_DB_URL` exists.
2. Copy `backend/.env.example` to `backend/.env` and update values as needed.
3. Start the service:

```bash
./mvnw spring-boot:run
```

## Docker Setup

This repository includes Docker support for local development.

Services:

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:3000`
- PostgreSQL: `localhost:5432`

Run the full stack:

```bash
docker compose up --build
```

Quick smoke check once the stack is up:

```bash
curl http://localhost:8080/health
```

Expected response:

```json
{"status":"UP","service":"warehouse-backend","timestamp":"..."}
```

If you want a deeper verification, log in and hit a protected API next:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin\"}"
```

## Notes

- `.env` is loaded automatically via `spring.config.import`.
- The backend is configured to read all runtime settings from environment variables, which makes it suitable for ECS task definitions, Secrets Manager, or SSM Parameter Store.
- WebSocket endpoint: `/ws`
- STOMP topics: `/topic/products`, `/topic/categories`, `/topic/inventory`, `/topic/orders`, `/topic/customers`, `/topic/suppliers`, `/topic/communications`

## AWS Deployment Checklist

1. Create an RDS PostgreSQL instance and initialize the target database.
2. Create an ECR repository for the backend image.
3. Create an ECS cluster with a Fargate service and task definition.
4. Attach the task to an Application Load Balancer listening on HTTP/HTTPS.
5. Store secrets in AWS Secrets Manager or SSM Parameter Store and reference them in the task definition.
6. Configure a CloudWatch log group for backend container logs.
7. If production email is required, verify the sender identity in SES and use SES SMTP credentials or another approved SMTP provider.
8. Deploy the backend image from the main-branch GitHub Actions workflow and confirm Flyway migrations run successfully on startup.
