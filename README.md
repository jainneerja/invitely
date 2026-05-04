# Invitely — AI Animated Invitation Platform

> Generate beautiful animated invitation pages using AI. Users describe a scene, AI generates a custom image with their event details baked in, and a video generation service brings it to life — all shareable via a single link.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3, Java 21 |
| AI Integration | Spring AI + Gemini 2.5 Flash Image (Nano Banana) |
| Database | PostgreSQL 16 + Flyway migrations |
| Async / Events | Apache Kafka |
| Asset Storage | AWS S3 |
| Video Generation | Kling AI / Runway ML (pluggable) |
| Frontend | React 18, Framer Motion |
| Notifications | SendGrid |

---

## Features

- **AI scene generation** — describe any scene and Gemini generates a custom invite image with event details baked in
- **Animated invites** — AI video generation brings the scene to life (animals walk, leaves sway, confetti bursts)
- **RSVP system** — guests respond directly on the invite page, host sees a live dashboard
- **Shareable links** — every invite gets a unique short URL (`invitely.app/i/abc123`)
- **3 prebuilt templates** — Jungle Magic, Elegant Wedding, Confetti Party
- **Event-driven architecture** — Kafka powers async video generation and email notifications

---

## Project Structure

```
invitely/
├── backend/          # Spring Boot API
│   ├── src/main/java/com/invitely/
│   │   ├── controller/     REST endpoints
│   │   ├── service/        Business logic + AI integration
│   │   ├── model/          JPA entities
│   │   ├── repository/     Spring Data repositories
│   │   ├── kafka/          Event producers + consumers
│   │   └── dto/            Request / response DTOs
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/   Flyway SQL migrations
└── frontend/         # React app
    └── src/
        ├── pages/          Home, CreateInvite, InvitePage
        ├── components/     Wizard steps, invite card, RSVP form
        ├── api/            API client
        └── store/          Zustand state
```

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 16
- Kafka (or Docker Compose below)
- Node 20+ (for frontend)

### 1. Clone the repo

```bash
git clone https://github.com/YOUR_USERNAME/invitely.git
cd invitely
```

### 2. Set up environment

Copy the example env file and fill in your keys:

```bash
cp backend/src/main/resources/application.yml \
   backend/src/main/resources/application-local.yml
```

Required environment variables:

```bash
DB_USERNAME=invitely
DB_PASSWORD=yourpassword
GOOGLE_CLOUD_PROJECT_ID=your-gcp-project
KAFKA_BOOTSTRAP=localhost:9092
AWS_S3_BUCKET=invitely-assets
AWS_ACCESS_KEY=...
AWS_SECRET_KEY=...
```

### 3. Start infrastructure (Docker)

```bash
docker compose up -d postgres kafka
```

### 4. Run the backend

```bash
cd backend
mvn spring-boot:run
```

Flyway will run all migrations automatically on startup.

### 5. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

App runs at `http://localhost:3000`, API at `http://localhost:8080`.

---

## API Overview

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/invites` | Create a new invitation |
| `GET` | `/api/invites/:slug` | Get invite by slug (guest view) |
| `POST` | `/api/invites/:id/generate-image` | Trigger AI image generation |
| `POST` | `/api/invites/:id/animate` | Trigger video animation |
| `GET` | `/api/templates` | List available templates |
| `POST` | `/api/invites/:id/rsvp` | Submit RSVP response |
| `GET` | `/api/invites/:id/rsvp` | Get RSVP responses (host) |

---

## Database Schema

```
invitations       — core invite data + AI generation state
templates         — JSON-driven template configs
assets            — uploaded + AI-generated images and videos
rsvp_responses    — guest RSVP submissions
```

---

## Architecture

```
React Frontend
      ↓ REST
Spring Boot API
  ├── Invite Service
  ├── RSVP Service
  ├── AI Service (Spring AI → Gemini)
  ├── Video Service (Kling / Runway — pluggable)
  └── Asset Service (S3)
      ↓
PostgreSQL + Kafka
      ↓
Notification Consumer (SendGrid)
      ↓
Gemini 2.5 Flash Image / Kling AI
```

---

## Roadmap

- [ ] Phase 1 — Database + entities ✅
- [ ] Phase 2 — Invite CRUD API
- [ ] Phase 3 — AI image generation (Spring AI + Gemini)
- [ ] Phase 4 — RSVP layer
- [ ] Phase 5 — Kafka + email notifications
- [ ] Phase 6 — React frontend wizard
- [ ] Phase 7 — Guest invite page + animations

---

## License

MIT
