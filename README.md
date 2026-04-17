# 🚀 Docify – DevOps CI/CD Pipeline

> Collaborative Document Editor with Real-time Sync and Version Control  
> **Full CI/CD Pipeline built on GitHub Actions + Docker + SonarCloud + Render**

---

## 📌 Problem Definition

**Docify** is a multi-user collaborative document editor (like Google Docs) with:
- Real-time collaborative editing via WebSockets (STOMP)
- Document versioning and diff-based restore
- Role-based sharing (owner / editor / viewer)
- JWT-free session authentication via Spring Security

**DevOps Challenge:** The project was developed locally with no automated build, test, or deployment pipeline. Bugs found late, no code quality checks, and manual deployment led to instability. This pipeline solves that.

---

## 🏗️ Project Architecture

```
┌─────────────┐     WebSocket/REST      ┌────────────────────┐
│  Browser    │ ─────────────────────▶ │  Spring Boot App    │
│ (HTML/JS)   │                         │  Port 8080          │
└─────────────┘                         └────────┬───────────┘
                                                  │ JPA/Hibernate
                                         ┌────────▼───────────┐
                                         │   PostgreSQL DB     │
                                         │   Port 5432         │
                                         └────────────────────┘
```

**Tech Stack:**
| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2, Java 17 |
| Real-time | WebSocket + STOMP |
| Database | PostgreSQL 15 |
| Security | Spring Security (session-based) |
| Build Tool | Maven 3.9 |
| Containerization | Docker (multi-stage) |
| CI/CD | GitHub Actions |
| Code Quality | SonarCloud + JaCoCo + Checkstyle |
| Staging Deployment | Render.com |

---

## 🔄 CI/CD Pipeline Overview

```
Developer pushes code
        │
        ▼
┌───────────────────┐
│  1. Build & Test  │  ← mvn clean test
│  (GitHub Actions) │  ← JUnit 5 + Mockito
└────────┬──────────┘
         │ pass
         ▼
┌───────────────────┐
│  2. Code Quality  │  ← SonarCloud analysis
│  (SonarCloud)     │  ← JaCoCo coverage (≥60%)
└────────┬──────────┘  ← Checkstyle validation
         │ pass
         ▼
┌───────────────────┐
│  3. Docker Build  │  ← Multi-stage Dockerfile
│  & Push to GHCR   │  ← GitHub Container Registry
└────────┬──────────┘
         │ success
         ▼
┌───────────────────┐
│  4. Deploy to     │  ← Render.com (staging)
│     Staging       │  ← Health check validation
└───────────────────┘
```

**Jobs only run on `main` branch pushes. Pull Requests only run Jobs 1 & 2.**

---

## 📁 Repository Structure

```
docify/
├── .github/
│   └── workflows/
│       └── ci-cd.yml          # GitHub Actions pipeline
├── src/
│   ├── main/
│   │   ├── java/com/docify/   # Application source
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/        # Frontend HTML/JS/CSS
│   └── test/
│       ├── java/com/docify/   # Unit & integration tests
│       └── resources/
│           └── application-test.properties  # H2 test DB config
├── k8s/
│   └── deployment.yaml        # Kubernetes manifests (optional)
├── Dockerfile                 # Multi-stage Docker build
├── docker-compose.yml         # Local dev + staging compose
├── pom.xml                    # Maven with JaCoCo + Checkstyle + Sonar
├── checkstyle.xml             # Code style rules
├── sonar-project.properties   # SonarCloud config
└── README.md
```

---

## ⚙️ Setup Instructions

### 1. Version Control – Git Branching Strategy

```
main          ← production-ready code, protected branch
  └── develop ← integration branch
        └── feature/your-feature  ← work here
```

```bash
git init
git add .
git commit -m "feat: initial project setup"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/docify.git
git push -u origin main
```

**Branch protection rules (set in GitHub → Settings → Branches):**
- Require PR before merging to `main`
- Require status checks: `build-and-test`, `code-quality` to pass

---

### 2. Automated Build Tool – Maven

```bash
# Compile
mvn clean compile

# Run all tests
mvn test

# Build JAR (skip tests)
mvn package -DskipTests

# Run checkstyle
mvn checkstyle:check

# Full verify (compile + test + coverage + checkstyle)
mvn verify
```

---

### 3. Continuous Integration – GitHub Actions

Triggers automatically on every push. No manual steps needed.

**Required GitHub Secrets** (Settings → Secrets and Variables → Actions):

| Secret Name | Value |
|---|---|
| `SONAR_TOKEN` | Token from sonarcloud.io |
| `SONAR_PROJECT_KEY` | Your SonarCloud project key |
| `SONAR_ORGANIZATION` | Your SonarCloud org name |
| `RENDER_API_KEY` | From Render dashboard → Account Settings |
| `RENDER_SERVICE_ID` | From your Render service URL |

---

### 4. Static Code Analysis – SonarCloud

1. Go to [sonarcloud.io](https://sonarcloud.io) → Sign in with GitHub
2. Create a new project → link your repository
3. Copy your `SONAR_TOKEN`, `organization`, and `projectKey`
4. Update `sonar-project.properties` with your values
5. Analysis runs automatically in Job 2 of the pipeline

**Quality Gate (auto-configured in SonarCloud):**
- Code coverage ≥ 60%
- No critical/blocker bugs
- No high-severity vulnerabilities

---

### 5. Docker – Containerization

```bash
# Build image locally
docker build -t docify:local .

# Run with docker-compose (app + postgres)
docker-compose up --build

# Stop
docker-compose down
```

The `Dockerfile` uses a **multi-stage build**:
- **Stage 1 (build):** Maven + JDK 17 → compiles and packages
- **Stage 2 (runtime):** JRE 17 Alpine → minimal, secure image (~180MB)
- Runs as **non-root user** for security

---

### 6. Staging Deployment – Render.com

1. Create a [Render](https://render.com) account
2. New → Web Service → Connect your GitHub repo
3. Configure:
   - **Runtime:** Docker
   - **Branch:** `main`
   - **Environment Variables:** Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
4. Also create a **PostgreSQL** service in Render and link its URL
5. Copy your Service ID from the Render dashboard URL
6. Add `RENDER_API_KEY` and `RENDER_SERVICE_ID` to GitHub Secrets

After setup, every push to `main` → auto-deploys to staging!

---

## 🧪 Test Coverage

| Test Class | Type | What it tests |
|---|---|---|
| `DocumentServiceTest` | Unit | Create, access control, rename, delete logic |
| `AuthServiceTest` | Unit | Register, login, password encoding |
| `DocumentControllerIntegrationTest` | Integration | Full HTTP layer via MockMvc |

Run tests and generate coverage report:
```bash
mvn verify
open target/site/jacoco/index.html
```

---

## 🐳 Running Locally with Docker

```bash
# Clone the repo
git clone https://github.com/YOUR_USERNAME/docify.git
cd docify

# Start everything (app + postgres)
docker-compose up --build

# Visit
open http://localhost:8080
```

---

## 📊 Monitoring

The app exposes Spring Actuator endpoints:
- `GET /actuator/health` → health status (used by Docker & Kubernetes healthchecks)
- `GET /actuator/info` → build info
- `GET /actuator/metrics` → JVM and HTTP metrics

---

## 🔒 Security Notes

- Passwords hashed with BCrypt via Spring Security
- Sessions expire after 24 hours
- Docker container runs as non-root user
- Secrets stored in GitHub Secrets (never hardcoded)
- `.gitignore` excludes `.env` and `application-local.properties`

---

## 👥 Team / Submission Info

| Field | Value |
|---|---|
| Project Name | Docify |
| Course | Full Stack Development |
| Tech Stack | Spring Boot · PostgreSQL · WebSocket · Docker · GitHub Actions · SonarCloud |
| Live Staging URL | https://docify-staging.onrender.com |
| Repository | https://github.com/YOUR_USERNAME/docify |
#   t r i g g e r   C I 
 
 #   t r i g g e r   C I 
 
 