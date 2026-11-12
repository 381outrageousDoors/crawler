# HN Front Page Tracker

Scans the Hacker News front page on a schedule, stores the stories in
PostgreSQL, and serves a live htmx table with filtering, sorting, and
one-click presets.

## Stack

- Java 25, Spring Boot 4.1 (web, data-jpa, thymeleaf, flyway, docker-compose)
- PostgreSQL 16 via podman/docker compose (started automatically by Boot)
- htmx 2 (webjar) for polling and fragment swaps, jsoup for scraping

## Features

- Scheduled scans with exponential backoff; manual "Scan now" is debounced
- Filter by title word count, points, or comment count (`>`, `>=`, `<`, `<=`, `=`)
- Sort by rank, title, points, or comment count, asc/desc
- Presets: **Ask 1** (titles > 5 words, most-commented first),
  **Ask 2** (titles <= 5 words, most-points first)
- Word count computed once at write time and stored per story
- The active filter/sort survives the 30 s poll; Clear resets the view
- Usage tracking: every page request logs endpoint, browser, filter, sort,
  result count, and duration

## Requirements

- JDK 25 (the Maven wrapper is checked in)
- podman or docker with a compose provider

## Setup

1. Create a `.env` in the project root with the compose credentials:

   ```
   DB_USER=scanner
   DB_PASSWORD=***
   ```

2. Run the app (compose DB starts with it):

   ```
   DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock ./mvnw spring-boot:run
   ```

   With docker instead of podman, drop `DOCKER_HOST`. Open http://localhost:8080.

3. Run the tests (Testcontainers needs the same daemon env):

   ```
   DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true ./mvnw test
   ```

## HTTP surface

| Route | Purpose |
| --- | --- |
| `GET /` | Full page: scan button, Ask presets, filter form, story table |
| `GET /stories` | View fragment (form + status + table); params `field`, `op`, `value`, `sort`, `dir`, `preset` — invalid values fall back to defaults |
| `POST /scan` | Debounced scan, then re-renders the view with default ordering |

Configuration keys (`application.yaml`, env-overridable): `scanner.target-url`
(`SCANNER_TARGET_URL`), `scanner.min-interval-seconds`, `scanner.max-interval-seconds`.

## Database

Flyway migrations in `src/main/resources/db/migration` (story, scan_state,
request/usage tables). The scanner keeps exactly the current front page:
rows not seen in the latest scan are deleted.

## License

MIT — see [LICENSE](LICENSE).
