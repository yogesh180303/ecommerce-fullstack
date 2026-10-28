# Atelier — considered objects for everyday life

A full-stack storefront built with React, Spring Boot, Spring Security/JWT, Spring Data JPA, and MySQL. The shop includes searchable collections, category filters, sorting, a persistent cart, account registration and sign-in, checkout, order history, and role-protected studio tools for products and order status.

## Run locally

Requirements: JDK 21 and Node.js 20+. The Maven Wrapper downloads the configured Maven release on first use.

1. Configure a signing key and (optionally) local administrator credentials, then start the API. It uses an in-memory H2 database and seeds the sample collection:
   ```powershell
   cd backend
   $env:JWT_SECRET = "local-development-signing-key-change-this-value"
   $env:ADMIN_EMAIL = "admin@atelier.local"
   $env:ADMIN_PASSWORD = "choose-a-local-only-password"
   .\mvnw.cmd spring-boot:run
   ```
2. In another terminal, start the storefront:
   ```powershell
   cd frontend
   npm install
   npm run dev
   ```
3. Open http://localhost:5173. The API is available at http://localhost:8080/api.

The local development administrator is seeded only when both `ADMIN_EMAIL` and `ADMIN_PASSWORD` are explicitly set; there are no default administrator credentials. The Compose environment also leaves both optional values empty. Set `JWT_SECRET` in every runtime environment; the application rejects the placeholder value in `.env.example`. Never reuse local credentials or a development signing key in a deployed environment.

## MySQL with Docker Compose

Copy `.env.example` to `.env`, replace every `CHANGE_ME` value with a unique secret, then run:

```powershell
docker compose up --build
```

The storefront is served on http://localhost:3000 and the API on http://localhost:8080. Compose runs MySQL 8.4 and starts the API with the `mysql` profile. Generate a unique JWT signing secret of at least 32 bytes; provide unique database and administrator passwords before deployment.

## API overview

| Method | Endpoint | Access |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Public |
| `POST` | `/api/auth/login` | Public |
| `GET` | `/api/auth/me` | Signed-in user |
| `GET` | `/api/products?page=0&size=24&search=&categoryId=&category=&minPrice=&maxPrice=&sortBy=&direction=` | Public |
| `GET` | `/api/products/{id}` | Public |
| `POST`, `PUT`, `DELETE` | `/api/admin/products[/{id}]` | Admin |
| `GET` | `/api/categories[/{id}]` | Public |
| `POST`, `PUT`, `DELETE` | `/api/admin/categories[/{id}]` | Admin |
| `GET`, `DELETE` | `/api/cart` | Signed-in user |
| `POST` | `/api/cart/items` | Signed-in user |
| `PUT`, `DELETE` | `/api/cart/items/{itemId}` | Signed-in user |
| `POST` | `/api/orders/checkout` | Signed-in user |
| `GET` | `/api/orders[/{id}]` | Order owner |
| `POST` | `/api/orders` | Signed-in user (legacy item-list checkout) |
| `GET` | `/api/admin/dashboard` | Admin |
| `GET` | `/api/admin/orders` | Admin |
| `PATCH` | `/api/admin/orders/{id}/status` | Admin |

Order totals and item prices are calculated from the database, not from client-submitted prices. Product stock is checked and decremented with database row locks when an order is placed. Authenticated customers have a persistent server-side cart and can only view their own orders. The admin dashboard and category management endpoints are administrator-only.

JWT tokens are kept in browser `localStorage` for this portfolio demo, which makes them accessible to JavaScript and therefore vulnerable to theft if a cross-site scripting flaw is introduced. Use HttpOnly secure cookies and a CSRF design for a production deployment. Logout clears the client-side token; bearer tokens remain valid until expiration.

Send bearer tokens in `Authorization: Bearer <token>` for protected endpoints. Cart endpoints include `GET /api/cart`, `POST /api/cart/items` with `{ "productId": 1, "quantity": 2 }`, `PUT /api/cart/items/{itemId}` with `{ "quantity": 3 }`, and delete operations for a cart item or the full cart.

The recommended checkout endpoint accepts only `{ "address": "..." }`; it uses the authenticated customer's persistent cart. Product rows are locked while stock is checked and decremented. Status updates are limited to `processing → shipped → delivered` or `processing → cancelled`; cancelling restores inventory.

Product search is performed by the backend (`search` is preferred; `q` remains supported). Filters accept `categoryId` or the legacy `category` name, plus `minPrice` and `maxPrice`. `sortBy` accepts `id`, `name`, or `price`; `direction` accepts `asc` or `desc`; page sizes are limited to 100. Category management rejects deletion while products still reference that category.

The Postman collection is in [`postman/atelier-store.postman_collection.json`](./postman/atelier-store.postman_collection.json). Set the collection variables before use; authentication requests save tokens automatically.

## Verification

On Windows, with JDK 21 and Node.js installed:

```powershell
cd backend
.\mvnw.cmd test
cd ..\frontend
npm run build
```

The backend tests use H2 in local mode and do not verify a live MySQL server. Docker Compose was not run in the current environment because Docker CLI is unavailable.

## Notes

- Products created through studio tools can introduce categories by entering a new category name.
- Product image values accept an Unsplash photo ID (for example `photo-1507473885765-e6ed057f782c`) or an absolute image URL.
- The local H2 console is available at http://localhost:8080/h2-console while using the local profile.
- Checkout records an order but does not process real payments; integrate a payment provider before accepting live orders.
