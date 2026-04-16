# 🚀 Docify – Collaborative Editor with CI/CD Pipeline

> Real-time document collaboration platform with a production-grade DevOps pipeline using GitHub Actions, Docker, SonarCloud, and Render.

---

## 📖 Overview

**Docify** is a multi-user collaborative document editor (similar to Google Docs) that enables:
- ✨ Real-time editing using WebSockets (STOMP)
- 📝 Document versioning with rollback support
- 🔐 Role-based access control (Owner / Editor / Viewer)
- 🔑 Secure session-based authentication using Spring Security

⚙️ **Key Focus:**  
This project demonstrates a **complete CI/CD pipeline** that automates build, testing, code quality checks, containerization, and deployment.

---

## ❗ Problem Statement

The application was initially developed without:
- ❌ Automated testing pipeline  
- ❌ Code quality checks  
- ❌ Continuous deployment  
- ❌ Reliable release process  

✅ This project solves these issues by implementing a **fully automated DevOps workflow**.

---

## 🏗️ Architecture

```
Client (Browser)
       │
       │ REST / WebSocket
       ▼
Spring Boot Application (Port 8080)
       │
       │ JPA / Hibernate
       ▼
PostgreSQL Database (Port 5432)
```

---

## 🧰 Tech Stack

| Layer | Technology |
|------|-----------|
| Backend | Spring Boot 3.2, Java 17 |
| Real-Time | WebSocket + STOMP |
| Database | PostgreSQL 15 |
| Security | Spring Security (Session-based) |
| Build Tool | Maven |
| Containerization | Docker (Multi-stage) |
| CI/CD | GitHub Actions |
| Code Quality | SonarCloud, JaCoCo, Checkstyle |
| Deployment | Render (Staging) |

---

## 🔄 CI/CD Pipeline

```
Push to main
     │
     ▼
1. Build & Test (JUnit + Mockito)
     │
     ▼
2. Code Quality (SonarCloud + JaCoCo + Checkstyle)
     │
     ▼
3. Docker Build & Push (GitHub Container Registry)
     │
     ▼
4. Deploy to Render (Staging)
```

### 📌 Workflow Rules
- `main` → Production-ready branch  
- Pull Requests → Run build & quality checks only  
- Deployment → Triggered only on `main`  

---

## 📁 Project Structure

```
docify/
├── .github/workflows/ci-cd.yml
├── src/
│   ├── main/java/com/docify/
│   ├── main/resources/
│   └── test/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── checkstyle.xml
├── sonar-project.properties
└── README.md
```

---

## ⚙️ Getting Started

### 🔹 Clone Repository

```bash
git clone https://github.com/YOUR_USERNAME/docify.git
cd docify
```

### 🔹 Run Locally (Docker)

```bash
docker-compose up --build
```

📍 Access the app at:  
http://localhost:8080

---

## 🧪 Testing & Coverage

Run all tests and checks:

```bash
mvn verify
```

Includes:
- Unit Tests (JUnit, Mockito)
- Integration Tests (MockMvc)
- Code Coverage (JaCoCo ≥ 60%)

---

## 🐳 Docker Setup

- Multi-stage build for optimized image size  
- Lightweight runtime image  
- Runs as **non-root user** for better security  

```bash
docker build -t docify .
docker-compose up
```

---

## ☁️ Deployment (Render)

1. Connect your GitHub repository to Render  
2. Choose **Docker deployment**  
3. Add environment variables:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`  
4. Add GitHub secrets:
   - `RENDER_API_KEY`
   - `RENDER_SERVICE_ID`  

🚀 Every push to `main` automatically deploys to staging.

---

## 📊 Monitoring

Spring Boot Actuator endpoints:
- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`

---

## 🔒 Security

- BCrypt password hashing  
- Session-based authentication  
- Secrets managed via GitHub Secrets  
- Sensitive files excluded using `.gitignore`  

---

## 🔗 Links

- 🌐 **Live App:** https://docify-staging.onrender.com  
- 📦 **Repository:** https://github.com/YOUR_USERNAME/docify  

---

## 👥 Project Information

| Field | Details |
|------|--------|
| Project | Docify |
| Domain | Full Stack + DevOps |
| Focus | CI/CD, Automation, Scalability |

---

## 💡 Key Takeaway

This project demonstrates how to build and deploy a **production-ready application** using modern DevOps practices:
- Automated CI/CD pipeline  
- Code quality enforcement  
- Containerized deployment  
- Continuous delivery  

---

## ⭐ Future Improvements

- Add real-time conflict resolution (OT/CRDT)
- Improve UI/UX
- Add production monitoring (Prometheus + Grafana)
- Kubernetes-based deployment
