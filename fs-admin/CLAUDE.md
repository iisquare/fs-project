# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Dev Commands

```sh
pnpm install          # install dependencies
pnpm run dev          # dev server (vite, --host enabled, mode=develoment)
pnpm run build        # production build (type-check + vite build)
pnpm run build-only   # vite build without type-check
pnpm run type-check   # vue-tsc --build
```

Required toolchain: Node v20, pnpm 10.x.

## Project Overview

Vue 3 + TypeScript admin panel ("FS Project") for a Java backend (`fs-java`). Uses Element Plus as UI framework, Pinia for state management, Vue Router with hash-based history. Chinese-locale UI (`zh-cn`).

The build output goes to `../fs-java/web/admin/src/main/resources/static` and copies `index.html` to the templates directory for server-side rendering integration.

## Architecture

### API Proxy Pattern

All backend calls go through a **proxy gateway** — the frontend never calls microservice APIs directly. Each service module (Member, LM, KG, OA, Spider, etc.) has an `src/api/{module}/Api.ts` that wraps calls through `src/core/Api.ts`:

```
View → ServiceApi.method() → ModuleApi.get/post() → core/Api.post('/proxy/get', { app, uri, data })
```

- `src/core/Api.ts` — Axios wrapper with unified error handling, auto-notification via ElMessage/ElNotification, and session expiry detection (403 → redirect to login).
- `src/core/FetchEventSource.ts` — SSE streaming wrapper that also goes through the proxy (`/proxy/getSSE`, `/proxy/postSSE`).
- API response format: `{ code: number, message: string, data: any }` — code 0 means success. `ApiUtil` provides helpers to read these fields.

### Routing

Routes are defined in `src/router/modules/{module}.ts`, each exporting `{ blanks, layouts }`. Blanks are standalone pages; layouts are nested under the main layout shell. All module routes are aggregated in `src/router/index.ts`.

Route meta supports: `title` (page title), `fit` (remove padding), `to` (breadcrumb link), `permit` (array of permission strings, any-match).

### Auth & Permissions

- `src/stores/user.ts` — User store. On startup, calls `RbacApi.login()` to load user info, menu tree, and resource permissions.
- Permission check: `user.hasPermit(permitArray)` — returns true if user has any of the listed resources.
- `v-permit` directive (`src/core/directive.ts`) — removes element from DOM if user lacks permission.
- Router guard enforces: unsynced state → startup page, not logged in → login page, logged in without permission → 403.

### Stores

- `user` — auth state, menu, resource permissions
- `cache` — generic async cache with TTL support for deduplicating API calls
- `counter` — UI flags (`routing`, `fetching`)

### Designers (Visual Editors)

`src/designer/` contains visual drag-and-drop editors built on AntV X6:
- `X6/` — shared X6 graph container and node components
- `FlexForm/` — form designer with draggable form fields
- `TaskFlow/` — task flow designer
- `KnowledgeGraph/` — knowledge graph editor
- `Agentic/` — AI agent workflow designer
- `Spider/` — web crawler configuration panels
- `Workflow/` — OA workflow designer

### Shared Components

`src/components/` provides reusable Element Plus wrappers: Button variants, Form helpers (search, date picker, password, autocomplete), Table (pagination, column settings, radio), Dictionary (select, cascader, tag, group), Layout (designer shell, toolbar, property panel), Chat, CodeEditor, ContextMenu.

Auto-imported via `unplugin-vue-components` with ElementPlusResolver — no manual imports needed for Element Plus components.

### SCSS

Global mixins in `src/assets/mixin.scss` are auto-injected into every component. Available: `flex-center`, `flex-end`, `flex-start`, `flex-between`, `flex-column`, `flex-row`, `flex-wrap`, `flex-start-column`, `flex-end-column`, `text-wrap`, `line-wrap($line)`.

## Conventions

- Path alias: `@` maps to `src/`.
- Environment: `VITE_APP_API_URL` controls backend base URL (empty string in production = same-origin).
- The `.env.develoment` filename has a typo (missing 'p') — this is intentional and matches the vite `--mode develoment` flag. Do not rename.
- Editor tab size: 2 spaces.
