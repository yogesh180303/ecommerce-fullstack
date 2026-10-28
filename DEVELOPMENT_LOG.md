# Development Log

## Repository baseline

- The workspace already contained the Atelier React/Spring Boot application and its source files when work began.
- The directory was not a Git repository; there was no branch, commit history, or existing commit baseline to preserve.
- A new `main` branch is being initialized with a snapshot of the existing application as a baseline. This baseline commit is not counted as one of the 25 planned development milestones.
- No historical milestone commits have been created or reconstructed. The checklist below is intentionally unchecked until genuine milestone changes are committed and verified.
- The project now targets Java 21. JDK 21 is available at `C:\Program Files\Java\jdk-21`.
- Docker CLI was unavailable during this work.

## Planned milestone checklist

- [ ] Commit 01 — `chore: initialize ecommerce project structure`
- [ ] Commit 02 — `chore(backend): initialize Spring Boot application`
- [ ] Commit 03 — `feat(backend): configure MySQL and JPA persistence`
- [ ] Commit 04 — `feat(database): add ecommerce entities and relationships`
- [ ] Commit 05 — `feat(backend): add repositories and database queries`
- [ ] Commit 06 — `feat(auth): implement user registration with BCrypt`
- [ ] Commit 07 — `feat(auth): implement JWT login and token validation`
- [ ] Commit 08 — `feat(security): enforce role-based access control`
- [ ] Commit 09 — `feat(products): implement product management APIs`
- [ ] Commit 10 — `feat(products): add search filters sorting and pagination`
- [ ] Commit 11 — `feat(categories): implement category management APIs`
- [ ] Commit 12 — `feat(cart): implement persistent shopping cart APIs`
- [ ] Commit 13 — `feat(orders): implement transactional checkout`
- [ ] Commit 14 — `feat(orders): add order history and admin order management`
- [ ] Commit 15 — `feat(backend): add DTO validation and global exception handling`
- [ ] Commit 16 — `chore(frontend): initialize React and Vite application`
- [ ] Commit 17 — `feat(frontend): build home page and product catalog`
- [ ] Commit 18 — `feat(frontend): implement authentication and protected routes`
- [ ] Commit 19 — `feat(frontend): implement shopping cart experience`
- [ ] Commit 20 — `feat(frontend): implement checkout and order history`
- [ ] Commit 21 — `feat(admin): build dashboard and management interface`
- [ ] Commit 22 — `test: add automated tests and fix integration issues`
- [ ] Commit 23 — `chore(docker): containerize application with Docker Compose`
- [ ] Commit 24 — `docs: add Postman collection and setup documentation`
- [ ] Commit 25 — `chore: finalize ecommerce application release`

## Work completed in this continuation

- Added database-backed customer carts with item validation, ownership via the authenticated user, and server-calculated subtotals.
- Added `/api/orders/checkout` to create orders from the user's saved cart, lock inventory rows, reject duplicate-line overselling, and clear the cart transactionally.
- Added authenticated order details, safe cancellation/restocking, and validated order-status transitions.
- Added category CRUD and category persistence/association, plus category filtering and safe deletion checks.
- Added price filters and validated sorting/pagination for the public product catalog.
- Added an administrator dashboard API and wired its summary to the existing admin UI.
- Connected the storefront cart to server persistence after sign-in while retaining the guest cart for unauthenticated browsing.
- Centralized browser API requests through an Axios client and added Vitest/Testing Library coverage for catalog rendering and guest-cart interaction.
- Added `/api/auth/me`, explicit 401/403 JSON responses, and startup rejection of placeholder JWT signing secrets.
- Removed default application JWT/admin credentials; startup now requires an explicit JWT secret, and admin seeding is opt-in through environment variables.
- Updated the Java target to 21, environment example, README, and added a Postman collection.
- Added integration coverage for cart checkout, order ownership, inventory rollback/overselling, category authorization, dashboard authorization, status transitions, and catalog price filtering.

## Verification results

- `backend\.mvnw.cmd test` using JDK 21: **9 tests passed**, 0 failures, 0 errors.
- `frontend\npm run build`: **passed**.
- `frontend\npm test`: **2 tests passed**.
- `npm install` audit: **0 vulnerabilities reported**.
- Docker Compose: **not run**; Docker CLI was not installed.
- MySQL: **not integration-tested**; backend tests use the local H2 profile.
- Postman collection syntax: **parsed successfully as JSON**.

## Remaining work and history status

- The baseline commit is not one of the 25 milestone commits; the milestone count starts at 0/25 after the baseline. Do not construct earlier commits for work that was already present in the workspace.
- The remaining roadmap is not complete. In particular, MySQL/Docker runtime verification, broader frontend decomposition and test coverage, complete API/UI verification, and the 25 genuine milestone commits remain outstanding.
- Do not mark a checklist item complete until its implementation, verification, and one corresponding meaningful commit have all succeeded.
