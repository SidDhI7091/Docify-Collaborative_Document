# Docify — Complete Project Documentation

---

## 1. What is Docify?

Docify is a collaborative document editor — think Google Docs but built from scratch. Multiple users can create, edit, and share documents in real time. It includes task management, version history, comments, an AI chat assistant, notifications, and a friends system — all in a single web application.

---

## 2. Technology Stack

### Why these technologies?

| Technology | Why Used | Why Not Alternatives |
|---|---|---|
| **Spring Boot 3.2.5** | Rapid REST API development, built-in DI, auto-configuration | Plain Spring = too much boilerplate. Node.js = weaker typing for complex domain models |
| **Java 17** | LTS release, records, pattern matching, text blocks | Java 21 had compatibility issues with some libraries at time of build |
| **PostgreSQL** | Relational data with complex joins (permissions, tasks, assignees) | MongoDB = no joins, bad for relational permission model. MySQL = less feature-rich |
| **Spring Data JPA + Hibernate** | ORM removes raw SQL for CRUD, JPQL for complex queries | MyBatis = more verbose. Raw JDBC = too much boilerplate |
| **Spring Security** | Session-based auth, CSRF protection, URL security | JWT = stateless, overkill for a single-server app. OAuth = too complex for this scope |
| **Spring WebSocket (STOMP)** | Real-time collaborative editing, live cursor, activity feed | Polling = too slow for real-time. SSE = one-directional only |
| **Spring WebFlux (WebClient)** | Non-blocking HTTP client for Gemini AI API calls | RestTemplate = deprecated. Feign = extra dependency |
| **Lombok** | Eliminates boilerplate getters/setters/builders/constructors | Manual code = hundreds of extra lines, error-prone |
| **Vanilla HTML/CSS/JS** | No build step, instant reload, full control | React/Vue = overkill for a server-rendered app, adds build complexity |
| **SockJS + STOMP.js** | WebSocket fallback for browsers that don't support native WS | Native WebSocket = no fallback, no message routing |
| **Google Gemini 1.5 Flash** | Free tier, fast, document-aware AI | OpenAI = requires billing. Anthropic = no free tier |
| **H2 (test scope)** | In-memory DB for unit tests, no PostgreSQL needed in CI | PostgreSQL in tests = slower, needs Docker |
| **JaCoCo** | Code coverage reports | No alternative needed — standard Java coverage tool |
| **Maven** | Dependency management, build lifecycle | Gradle = more complex syntax for this team |

---

## 3. Project Structure

```
demo/
├── src/
│   ├── main/
│   │   ├── java/com/docify/
│   │   │   ├── DocifyApplication.java       ← Spring Boot entry point
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java      ← HTTP security rules
│   │   │   │   └── WebSocketConfig.java     ← STOMP broker setup
│   │   │   ├── controller/                  ← HTTP request handlers (REST API)
│   │   │   ├── service/                     ← Business logic
│   │   │   ├── repository/                  ← Database queries (Spring Data JPA)
│   │   │   └── model/                       ← JPA entities (database tables)
│   │   └── resources/
│   │       ├── application.properties       ← DB config, API keys, server port
│   │       └── static/                      ← Frontend HTML/CSS/JS files
│   └── test/                                ← Unit and integration tests
├── pom.xml                                  ← Maven dependencies
├── Dockerfile                               ← Container build
├── docker-compose.yml                       ← Local dev with PostgreSQL
└── k8s/deployment.yaml                      ← Kubernetes staging config
```

---

## 4. Architecture — How It All Fits Together

```
Browser (HTML/CSS/JS)
        │
        │  HTTP REST (fetch API)
        │  WebSocket (STOMP over SockJS)
        ▼
Spring Boot Application (port 8080)
        │
        ├── Controllers  →  Services  →  Repositories  →  PostgreSQL
        │
        └── WebClient  →  Gemini AI API (external)
```

The browser talks to the backend via two channels:
- **REST API** — for all CRUD operations (create doc, save content, assign task, etc.)
- **WebSocket** — for real-time events (collaborative editing, activity feed, cursor positions)

---

## 5. Layer-by-Layer Breakdown

### 5.1 Model Layer (`model/`)

JPA entities — each class maps to a PostgreSQL table.

| Class | Table | Purpose |
|---|---|---|
| `User` | `users` | Registered accounts — id, name, email, hashed password |
| `Document` | `documents` | A document — title, owner (FK to User), color, deadline |
| `DocumentTab` | `document_tabs` | A tab inside a document — name, content (HTML), position |
| `DocumentVersion` | `document_versions` | Snapshot of a tab's content at a point in time |
| `DocumentPermission` | `document_permissions` | Who has access to a doc — user, doc, permissionType (VIEW/EDIT) |
| `DocumentLink` | `document_links` | Shareable link token with a role (VIEW/EDIT) |
| `Comment` | `comments` | Comment on a tab — author, content, resolved flag |
| `Task` | `tasks` | A task on a document — title, description, dueDate, status, createdBy |
| `TaskAssignee` | `task_assignees` | Junction table — which user is assigned to which task |
| `Notification` | `notifications` | In-app notification — recipient, message, type, read flag |

**Key relationships:**
- `Document` → `User` (owner) — ManyToOne
- `DocumentTab` → `Document` — ManyToOne
- `DocumentPermission` → `Document` + `User` — ManyToOne each
- `Task` → `Document` + `User` (createdBy) — ManyToOne each
- `TaskAssignee` → `Task` + `User` — ManyToOne each (junction table)

**Why `FetchType.LAZY` everywhere?**
Lazy loading means related entities are only loaded from DB when accessed. This avoids loading the entire object graph on every query. The downside: accessing lazy fields outside a Hibernate session throws `LazyInitializationException` — solved by using `JOIN FETCH` in JPQL queries.

---

### 5.2 Repository Layer (`repository/`)

Spring Data JPA interfaces. You declare method signatures or JPQL queries — Spring generates the SQL automatically.

| Repository | Key Custom Queries |
|---|---|
| `UserRepository` | `findByEmail(String)` |
| `DocumentRepository` | `findByIdWithOwner` (JOIN FETCH owner), `findSharedWithUserAndPermission` |
| `DocumentTabRepository` | `findByDocumentIdOrderByPositionAsc` |
| `DocumentVersionRepository` | `findByTabIdOrderByCreatedAtDesc`, `findByIdWithTab` (JOIN FETCH) |
| `DocumentPermissionRepository` | `findByDocumentIdAndUserId`, `findUsersISharedWith`, `findUsersWhoSharedWithMe` |
| `TaskRepository` | `findTasksAssignedToUser` (via TaskAssignee join), `findByCreatedByIdOrderByCreatedAtDesc`, `findByIdWithRelations` |
| `TaskAssigneeRepository` | `findByTaskId` (JOIN FETCH user), `existsByTaskIdAndUserId` |
| `NotificationRepository` | `findByUserIdOrderByCreatedAtDesc`, `countByUserIdAndReadFalse`, `markAllReadForUser` |

**Why JOIN FETCH?**
Standard `findById` returns a lazy proxy. When the controller tries to serialize it to JSON (outside the transaction), Hibernate throws an error. `JOIN FETCH` loads the entity and its relations in a single SQL query, inside the transaction, so serialization works safely.

---

### 5.3 Service Layer (`service/`)

Business logic lives here. Controllers call services; services call repositories. All database-mutating methods are `@Transactional` — if anything fails, the whole operation rolls back.

| Service | Responsibility |
|---|---|
| `AuthService` | Register user (BCrypt password hash), login (password verify) |
| `DocumentService` | Create/get/update/delete documents, `canAccess` check |
| `ShareService` | Share doc with user, create share links, `canEdit`/`canView` permission checks |
| `TabService` | Create/rename/delete tabs, save content (auto-snapshots version before saving) |
| `VersionService` | Get version history, restore a version (snapshots current before restoring) |
| `TaskService` | Create tasks with assignees, get my tasks, get tasks I created, toggle status |
| `NotificationService` | Create notifications on task assign/complete, mark all read |
| `ChatService` | Build prompt with document content, call Gemini API via WebClient, parse response |
| `PresenceService` | In-memory heartbeat map — tracks when each user was last active |

**Why `@Transactional` on services, not controllers?**
Spring's `@Transactional` works via AOP proxy — it only works when called through the Spring proxy, not when the annotated method calls itself. Controllers are `@RestController` beans, not transactional proxies. Putting `@Transactional` on service methods is the correct pattern.

---

### 5.4 Controller Layer (`controller/`)

Thin HTTP handlers. They validate the session, call the service, and return JSON. No business logic here.

| Controller | Base URL | What it handles |
|---|---|---|
| `AuthController` | `/api/auth` | Register, login, logout, `/me` (current user) |
| `PageController` | `/`, `/dashboard`, `/editor/{id}`, `/tasks`, `/share/{token}` | Serves HTML pages, redirects unauthenticated users |
| `DocumentController` | `/api/documents` | CRUD for documents, color, deadline, `canEdit` in response |
| `TabController` | `/api/documents/{docId}/tabs` | CRUD for tabs, save content |
| `VersionController` | `/api/versions` | List versions, restore version |
| `ShareController` | `/api/share` | Share with user, create link, get permissions, `/my-permission` |
| `CommentController` | `/api/comments` | Post/list comments, replies, resolve |
| `TaskController` | `/api/tasks` | Create task, list by doc/user/creator, toggle status, delete |
| `NotificationController` | `/api/notifications` | List, unread count, mark all read |
| `FriendsController` | `/api/friends` | Get friends list (from shared docs), heartbeat for presence |
| `ChatController` | `/api/chat` | Receive message + doc content, call ChatService, return AI response |
| `WebSocketController` | STOMP `/app/...` | Route edit events, cursor events, tab events to subscribers |

---

### 5.5 Config Layer (`config/`)

**`SecurityConfig.java`**
- Disables CSRF (safe for session-based API)
- Permits all requests (`/**`) — auth is handled manually via `session.getAttribute("userId")`
- Disables Spring's default form login and logout
- Provides `BCryptPasswordEncoder` bean for password hashing

**`WebSocketConfig.java`**
- Registers `/ws` as the STOMP endpoint with SockJS fallback
- Enables simple in-memory broker on `/topic`
- Sets `/app` as the application destination prefix (messages sent to `/app/edit/123` are routed to `@MessageMapping("/edit/{tabId}")`)

---

## 6. Feature Deep-Dives

### 6.1 Authentication

**Flow:**
1. User submits email + password to `POST /api/auth/login`
2. `AuthService.login()` finds user by email, verifies BCrypt hash
3. On success, `userId` and `userName` are stored in the HTTP session
4. Every subsequent request checks `session.getAttribute("userId")` — if null, returns 401
5. Logout calls `session.invalidate()`

**Why session-based, not JWT?**
This is a server-rendered single-server app. Sessions are simpler, more secure (no token storage on client), and easier to invalidate. JWT makes sense for distributed/stateless APIs — not needed here.

---

### 6.2 Document & Tab System

A **Document** is the top-level container (like a Google Doc file). Inside each document are **Tabs** (like sheets in a spreadsheet). Each tab has its own HTML content.

**Creating a document:**
1. `POST /api/documents` → `DocumentService.createDocument()` saves the doc and auto-creates "Tab 1"
2. Browser redirects to `/editor/{id}`

**Saving content:**
1. User types in the `contenteditable` div
2. `onEditorInput()` fires, debounces 2 seconds, then calls `PUT /api/documents/{id}/tabs/{tabId}/content`
3. `TabService.saveContent()` — if the tab already has content, it snapshots it as a `DocumentVersion` first, then saves the new content
4. Simultaneously, the content is broadcast via WebSocket to all other users viewing the same tab

---

### 6.3 Real-Time Collaboration (WebSocket)

**Technology:** STOMP protocol over SockJS (WebSocket with HTTP fallback)

**How it works:**
1. When the editor loads, `connectWS()` creates a STOMP connection to `/ws`
2. It subscribes to `/topic/tab/{tabId}` for the current tab
3. When user A types, `onEditorInput()` sends the full HTML content to `/app/edit/{tabId}`
4. `WebSocketController.handleEdit()` receives it and broadcasts to `/topic/tab/{tabId}`
5. User B's subscription receives it, updates their editor (skipping if it's their own message via `userId` check)

**Tab events** (add/delete/rename tab) are broadcast on `/topic/tabs/{docId}` so all collaborators see the sidebar update.

---

### 6.4 Permission System

Two levels of access: **VIEW** and **EDIT**.

**How permissions are stored:**
- `document_permissions` table: `(document_id, user_id, permission_type)`
- The document owner is NOT in this table — ownership is tracked via `documents.owner_id`

**How `canEdit` is determined (in order):**
1. Is `userId == document.owner_id`? → always EDIT
2. Is there a row in `document_permissions` with `permission_type = 'EDIT'`? → EDIT
3. Is there any row in `document_permissions`? → VIEW
4. None of the above → no access

**Frontend enforcement:**
- `GET /api/documents/{id}` returns `canEdit: true/false`
- Editor sets `contenteditable = "true"/"false"` based on this
- Toolbar buttons are disabled for viewers
- Backend also checks `shareService.canEdit()` on every write endpoint

**Share links:**
- `POST /api/share/{docId}/link` generates a UUID token stored in `document_links`
- Anyone visiting `/share/{token}` gets access at the link's permission level

---

### 6.5 Version History

Every time content is saved, the **previous** content is snapshotted as a `DocumentVersion`.

**Restore flow:**
1. User clicks "Restore this" on a version
2. `POST /api/versions/{versionId}/restore`
3. `VersionService.restoreVersion()`:
   - Fetches the version with `JOIN FETCH` (eager load tab + document)
   - Snapshots the current content as "Before restore"
   - Sets `tab.content = version.content`
   - Saves and returns `{id, content}` as plain Map (not JPA entity — avoids serialization issues)
4. Frontend sets `editor.innerHTML = restoredContent`

---

### 6.6 Task Management

**Data model:**
- `Task` — belongs to a document, created by a user
- `TaskAssignee` — junction table linking tasks to assigned users (many-to-many)

**Creating a task:**
1. User opens Tasks panel in editor, fills modal, adds assignee emails, clicks "Create Task"
2. `submitTask()` auto-captures any email still in the input field (UX fix)
3. `POST /api/tasks/document/{docId}` → `TaskService.createTask()`
4. Task is saved, then for each email: find user → save `TaskAssignee` → create `Notification`
5. All inside `@Transactional` so if any step fails, everything rolls back

**Dashboard widgets:**
- **My Tasks** — calls `GET /api/tasks/my` → `findTasksAssignedToUser(userId)` via TaskAssignee join
- **Tasks Assigned By Me** — calls `GET /api/tasks/created-by-me` → `findByCreatedByIdOrderByCreatedAtDesc(userId)`
- **Tasks Kanban page** (`/tasks`) — combines both, groups by document, shows Pending/Completed columns

---

### 6.7 Notifications

**Triggered automatically:**
- When a task is assigned → assignee gets: `"[Name] assigned you '[task]' in '[doc]' · Deadline: [date]"`
- When an assignee marks a task done → task creator gets: `"[Name] completed '[task]' in '[doc]'"`

**How the bell works:**
1. Dashboard calls `GET /api/notifications/unread-count` on load and every 30 seconds
2. If count > 0, the amber dot appears on the bell
3. Clicking the bell opens the panel, calls `GET /api/notifications`
4. Clicking a notification opens the document (`documentId` stored on each notification)
5. "Mark all read" calls `POST /api/notifications/mark-all-read`

---

### 6.8 AI Chat Assistant

**Technology:** Google Gemini 1.5 Flash via REST API (WebClient — non-blocking)

**Flow:**
1. User types in the chat panel and clicks send
2. `POST /api/chat` with `{message, documentContent, documentId}`
3. `ChatService.chat()` builds a system prompt:
   - Includes the document's plain text (HTML stripped, max 800 chars)
   - Adds the user's message
4. Sends to `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent`
5. API key sent in `x-goog-api-key` header (never exposed to browser)
6. Response parsed, returned to frontend
7. Frontend renders with bold/newline formatting

**Quick action buttons:** Summarize, Improve, Key points, Continue writing, Fix grammar — each pre-fills a prompt and sends immediately.

**Rate limiting:** Free tier = 15 req/min. Send button disabled for 4 seconds after each request.

---

### 6.9 Voice Dictation

**Technology:** Web Speech API (built into Chrome/Edge — zero backend, zero API key)

**How it works:**
1. User clicks "Dictate" in the toolbar
2. `SpeechRecognition` object created with `continuous: true` and `interimResults: true`
3. While speaking: interim (partial) text shown in grey italic in the editor
4. When a sentence is finalized: `document.execCommand('insertText')` inserts it permanently
5. Auto-saves via the normal debounce mechanism
6. Click "Stop" to end

**Why Web Speech API?**
Free, no server round-trip, works offline, built into the browser. Whisper/Deepgram would require a backend proxy and API costs.

---

### 6.10 Friends & Online Presence

**Friends definition:** Anyone you've shared a doc with, or who shared a doc with you. No separate friends table — derived from `document_permissions`.

**Queries:**
- People I shared with: `SELECT user FROM DocumentPermission WHERE document.owner = me`
- People who shared with me: `SELECT document.owner FROM DocumentPermission WHERE user = me`

**Online presence:**
- `PresenceService` holds a `ConcurrentHashMap<Long, Instant>` (userId → last heartbeat)
- Dashboard calls `POST /api/friends/heartbeat` on load and every 60 seconds
- `isOnline()` = heartbeat within last 5 minutes
- `GET /api/friends` returns each friend with `online: true/false` and `lastSeenSeconds`

**Why in-memory, not database?**
Presence is ephemeral — it resets on server restart, which is fine. Writing to DB every 60 seconds per user would be wasteful.

---

### 6.11 Comments

- Comments are scoped to a **tab** (not the whole document)
- Each comment can have **replies** (nested one level)
- Comments can be **resolved** (marked done, shown with a green badge)
- All stored in `comments` table with `parent_id` for replies and `resolved` boolean

---

## 7. Frontend Pages

| File | Route | Purpose |
|---|---|---|
| `login.html` | `/login` | Login form |
| `register.html` | `/register` | Registration form |
| `dashboard.html` | `/dashboard` | Home — my docs, shared docs, friends, tasks, notifications |
| `editor.html` | `/editor/{id}` | Full document editor with tabs, toolbar, AI chat, voice |
| `tasks.html` | `/tasks` | Kanban board of all tasks assigned to/by me |
| `shared-view.html` | `/share/{token}` | Read-only or edit view via share link |

**No framework — why?**
All pages are plain HTML with inline JavaScript. No React, Vue, or Angular. This means:
- No build step (no webpack, no npm run dev)
- Instant page loads
- Full control over every pixel
- Easy to deploy (just static files served by Spring Boot)

The tradeoff is more verbose JS for DOM manipulation, but for this project size it's the right call.

---

## 8. Database Schema Summary

```
users
  id, name, email, password, created_at

documents
  id, owner_id → users, title, color, deadline, created_at, updated_at

document_tabs
  id, document_id → documents, name, content (TEXT), position

document_versions
  id, document_id → documents, tab_id → document_tabs,
  saved_by → users, content (TEXT), version_name, created_at

document_permissions
  id, document_id → documents, user_id → users,
  permission_type (VIEW/EDIT), granted_by → users, created_at
  UNIQUE(document_id, user_id)

document_links
  id, document_id → documents, token (unique), permission_type, is_active, created_at

comments
  id, tab_id → document_tabs, author_id → users,
  content, parent_id → comments (self-ref for replies), resolved, created_at

tasks
  id, document_id → documents, created_by → users,
  title, description, due_date, status (PENDING/COMPLETED), created_at

task_assignees
  id, task_id → tasks, user_id → users
  UNIQUE(task_id, user_id)

notifications
  id, user_id → users, type, message, task_id, document_id, is_read, created_at
```

---

## 9. Deployment Setup

**Local development:**
```bash
./mvnw spring-boot:run -Dmaven.test.skip=true
```
Runs on `http://localhost:8080`. Requires PostgreSQL running locally.

**Docker:**
```bash
docker-compose up
```
Starts PostgreSQL + the Spring Boot app together. App connects to DB via `docify-db` hostname.

**Dockerfile (multi-stage):**
1. Stage 1 (`maven:3.9.6`): Compiles the JAR
2. Stage 2 (`eclipse-temurin:17-jre-alpine`): Runs the JAR as a non-root user

**Kubernetes (`k8s/deployment.yaml`):**
- 2 replicas of the app with rolling update strategy
- PostgreSQL with PersistentVolumeClaim for data
- Secrets for DB credentials
- LoadBalancer service on port 80 → 8080

**CI/CD (`.github/workflows/ci-cd.yml`):**
1. Build & test (H2 in-memory DB, no PostgreSQL needed)
2. SonarCloud code quality analysis
3. Build Docker image, push to GitHub Container Registry
4. Trigger Render deploy via API

---

## 10. Security Considerations

- Passwords hashed with **BCrypt** (cost factor 10) — never stored in plain text
- Session-based auth — no tokens in localStorage (XSS-safe)
- CSRF disabled — acceptable for a session-based API with same-origin requests
- All write endpoints check `canEdit()` before executing
- Gemini API key stored in `application.properties` — never sent to the browser
- Docker runs as non-root user (`docify` user in Alpine)
- Share link tokens are UUID v4 (128-bit random — practically unguessable)

---

## 11. Key Design Decisions & Tradeoffs

| Decision | Reason | Tradeoff |
|---|---|---|
| Session auth over JWT | Simpler, server-controlled invalidation | Doesn't scale horizontally without sticky sessions |
| In-memory presence | No DB writes for heartbeats | Resets on server restart |
| HTML content in DB | Preserves formatting exactly | Large content, harder to search |
| Lazy JPA loading + JOIN FETCH | Avoids N+1 queries | Must remember to use JOIN FETCH for every endpoint that serializes |
| Single-server WebSocket | Simple, no Redis broker needed | Doesn't scale to multiple instances |
| Vanilla JS frontend | No build step, fast iteration | More verbose DOM code |
| `@Transactional` on services only | Correct Spring AOP proxy pattern | Controllers must not have `@Transactional` |
