# Development Log

## Repository baseline

- The workspace contained an existing full-stack Atelier application before Git initialization. No prior history was present.
- Baseline snapshot on `main`: Commit 01, `10bd65f9abfc67cc1b2c41d566deae0bed88d7b1`.
- The baseline includes the existing React storefront, Spring Boot API, cart and checkout implementation, category and admin endpoints, Postman collection, and initial tests. These features are not being recreated.
- Java 21 and Node.js are available. Docker and a local MySQL server/CLI were not available during the initial verification.
- Never rewrite the baseline or push without explicit authorization.

## Commit checklist

- [x] Commit 01 — Existing project baseline — `chore: establish existing ecommerce app baseline` — `10bd65f9abfc67cc1b2c41d566deae0bed88d7b1`
- [ ] Commit 02 — Reproducible verification and accurate baseline record — `test: add reproducible project verification`
- [ ] Commit 03 — Product response DTOs — `refactor(api): return product response DTOs`
- [ ] Commit 04 — Product lifecycle and validation — `feat(products): harden product lifecycle and validation`
- [ ] Commit 05 — Category association integrity — `feat(categories): enforce category association integrity`
- [ ] Commit 06 — Registration and current-user flow — `feat(auth): harden registration and current-user flow`
- [ ] Commit 07 — Security access/error contracts — `feat(security): cover access rules and API error contracts`
- [ ] Commit 08 — Cart concurrency and ownership — `feat(cart): harden cart concurrency and ownership`
- [ ] Commit 09 — Checkout service boundary — `refactor(orders): move checkout rules into a service`
- [ ] Commit 10 — Typed order statuses and transitions — `feat(orders): model order status transitions explicitly`
- [ ] Commit 11 — Transaction rollback and oversell tests — `test(orders): verify rollback and oversell protection`
- [ ] Commit 12 — MySQL integration profile and schema checks — `test(database): add MySQL integration test profile`
- [ ] Commit 13 — Health and readiness endpoint — `feat(backend): add application health endpoint`
- [ ] Commit 14 — OpenAPI REST contract — `feat(api): document REST contract with OpenAPI`
- [ ] Commit 15 — Frontend API modules and auth failures — `refactor(frontend): centralize API modules and auth failures`
- [ ] Commit 16 — Restore authentication from backend — `feat(frontend): restore authentication from backend`
- [ ] Commit 17 — Server-driven catalog controls — `feat(frontend): add server-driven catalog controls`
- [ ] Commit 18 — Product details experience — `feat(frontend): add product details experience`
- [ ] Commit 19 — Cart state and stock error handling — `feat(frontend): make cart state and stock errors consistent`
- [ ] Commit 20 — Checkout and order details — `feat(frontend): complete checkout and order details`
- [ ] Commit 21 — Admin dashboard and management states — `feat(admin): complete dashboard and management states`
- [ ] Commit 22 — Frontend critical-flow tests — `test(frontend): cover authentication catalog cart and admin`
- [ ] Commit 23 — Docker health checks and Compose startup — `chore(docker): add health checks and reliable Compose startup`
- [ ] Commit 24 — Continuous integration checks — `ci: verify backend and frontend on pull requests`
- [ ] Commit 25 — Verified API/runtime documentation and release — `docs: finalize verified API and runtime guide`

Commit 01 is the existing baseline. The 24 future items must each contain a genuine implementation, test, documentation, or configuration improvement; do not make placeholder or empty commits. Validate each milestone before committing. Record each resulting hash in the following milestone's log update; the final hash will be reported from verified Git history because a commit cannot contain its own hash.

## Prior implementation and verification

Before Commit 01 was created, the workspace was extended with persistent carts, transactional cart checkout, category APIs, product price filters, order ownership and status transitions, an admin summary endpoint, frontend Axios integration, a Postman collection, and tests.

Last recorded verification before Commit 02:

- Backend Maven tests on JDK 21: **9 passed**, 0 failures, 0 errors.
- Frontend Vitest: **2 passed**.
- Frontend production build: **passed**.
- Postman collection JSON: **valid**.
- Docker Compose and live MySQL: **not run**; tools/services were unavailable.

## Commit 02 — Reproducible project verification

- Added `scripts/verify.ps1` to verify JDK 21 and run backend tests, frontend tests, frontend build, and Postman JSON parsing.
- Updated README with the verification script and its real verification boundary (H2 tests are not live-MySQL verification).
- Corrected the baseline to count as Commit 01 and replaced the former roadmap checklist with the approved Commit 02–25 work plan.
- Verification results and this commit hash are recorded after commit creation and will be included in the next milestone log update.

## Final status

- Verified total commits so far: **1 / 25** before Commit 02; do not claim completion until `git rev-list --count main` confirms 25 and all milestones are real.
- Current branch: `main`.
- Remote push: not authorized; none performed.
- Docker/MySQL runtime validation and subsequent approved milestones remain outstanding.
