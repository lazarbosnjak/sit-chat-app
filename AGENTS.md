# Repository Guidelines

## Project Structure & Module Organization

This repository is split into a Spring Boot backend and an Angular frontend.
Backend Java code lives in `backend/src/main/java/ftn/svt`, organized by layer:
`controller`, `service`, `repository`, `model`, `model/dto`, `config`, and
`exception`. Backend configuration is in `backend/src/main/resources/application.yaml`;
backend tests belong under `backend/src/test/java`.

Frontend code lives in `frontend/src/app`. Use `core` for singleton services,
guards, interceptors, constants, and app-wide infrastructure; `features` for routed
areas such as `auth`, `chat`, `profile`, and `admin`; and `shared` for reusable
components and types. Static frontend assets go in `frontend/public`.

## Build, Test, and Development Commands

- `cd backend && ./mvnw spring-boot:run`: run the API locally.
- `cd backend && ./mvnw test`: run backend tests.
- `cd backend && ./mvnw package`: compile, test, and build the backend artifact.
- `cd frontend && pnpm install`: install frontend dependencies using the pinned
  package manager.
- `cd frontend && pnpm start`: run the Angular dev server.
- `cd frontend && pnpm build`: create a production frontend build.
- `cd frontend && pnpm test`: run Angular/Vitest tests.

## Coding Style & Naming Conventions

Follow `frontend/.editorconfig`: UTF-8, two-space indentation, final newline, and
trimmed trailing whitespace. TypeScript uses single quotes. Prettier is configured
in `frontend/.prettierrc` with `printWidth: 100` and Angular HTML parsing.

Use conventional Angular names such as `*.component.ts`, `*.service.ts`,
`*.guard.ts`, and colocated `*.component.html`. Java packages should remain under
`ftn.svt`; use PascalCase for classes and lower camelCase for methods and fields.

## Testing Guidelines

Frontend unit tests use spec files such as `app.spec.ts`; place new tests next to
the code they exercise as `*.spec.ts`. Backend tests should be named `*Test.java`
under `backend/src/test/java`. Add or update tests when changing services,
controllers, guards, or user-visible workflows.

## Commit & Pull Request Guidelines

Recent history uses concise Conventional Commit-style messages, for example
`feat(frontend): add profile page`, `feat(api): add updating user info endpoint`,
and `fix: fix files not using enviornment dependent content`. Keep commits focused
and prefer scopes like `frontend`, `api`, or `auth` when helpful.

Pull requests should include a short description, the commands you ran, linked
issues when applicable, and screenshots for UI changes. Mention configuration or
database assumptions explicitly.

## Security & Configuration Tips

Do not commit secrets or local credentials. Keep environment-dependent values in
configuration files or environment-specific frontend files under
`frontend/src/environments`.

## Agent-Specific Instructions

Do not run project tests, builds, or dev servers yourself unless explicitly asked.
Modify the requested files only, then report what changed and which tests the user
should run.

Treat `./Specifikacija.txt` as the end goal for the project. The user will request
work feature by feature; implement each feature in a way that leaves clear
extension points for later requirements that support that specification.
