# Lumoo

Spring Boot 3.5 / Java 21 marketplace for The Gambia (lumoo.my). Thymeleaf + Tailwind (CDN), MySQL 8.4,
Redis cache (prod), ModemPay payments (GMD), Resend email. Single DigitalOcean droplet via docker compose + Caddy.

## Commands
- Build + all tests (what CI runs): `./mvnw -B verify`
- One test class while working: `./mvnw -q -o test -Dtest=OrderServiceTest` (use `-o` once deps are cached)
- Migrations against real MySQL (needs Docker): `./scripts/verify-mysql-migrations.sh`
- CSS: Tailwind v3 is compiled at build time (no CDN) into `static/css/tailwind.css` (gitignored) by
  `./scripts/build-css.sh`; Maven runs it in `generate-resources`. Use `./scripts/build-css.sh --watch` while
  editing templates. Never build class names dynamically (`'bg-' + color`) — the scanner can't see them.
- Tests need no MySQL/Redis: `src/test/resources/application.properties` = H2 (MySQL mode) + Flyway + `ddl-auto=validate`.

## Layout (`src/main/java/com/example/lumoo/`)
- `domain/<feature>/` — entity, repository, service, controller together (order, payment, product, user, vendor,
  promo, pdpp, shipping, admin, blog, career, …). Business logic belongs in services; keep controllers thin.
- `infrastructure/` — security (SecurityConfig, RateLimitFilter, LoginAttemptService, DataEncryptor, …), web, email, db.
- `app/` — seeders (`DatabaseSeeder` is `@Profile("dev")`).
- Templates: `src/main/resources/templates/`, shared bits in `fragments/` (loading.html holds the confirm modal + CSRF token).

## Rules that bite
- **Schema changes need a Flyway migration** `src/main/resources/db/migration/V<n>__*.sql`. Never edit V1 (prod is
  baselined at V1). Hibernate runs `validate`, so a missing migration fails the tests.
- Money is `double` today (BigDecimal migration is planned). Round to 2 dp; split amounts with `OrderService.split`.
- Orders are never deleted — cancel via `OrderService.cancelAndRelease` / `adminCancel` (returns stock once).
- PAID transitions go through `OrderService.markPaid`; delivery through `applyDelivered` (starts payout hold, COD skipped).
- State-changing endpoints are POST + CSRF. Admin HTML is shown only through sanitised getters (`HtmlSanitizer`).
- Shipping and promo discounts are computed server-side; never trust amounts from the browser.
- Prod config comes from env vars mapped in `application-prod.properties`; compose requires them (no defaults).

## Working agreement (see ../rule.md)
- Short plan (files, change, test) → implement. Only stop to ask for decisions about money, law/regulators (PDPP),
  or production data/deploys; otherwise pick the recommended option and continue.
- Run targeted tests while working, full `./mvnw -B verify` once before reporting.
- Don't commit or push unless asked. Work on a branch, never directly on `main`.
- Commit author email must be the GitHub noreply address (already set in this repo's git config).
