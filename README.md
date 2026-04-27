# KYC Onboarding System

A beginner-friendly KYC (Know Your Customer) onboarding portal built with Spring Boot and React.

---

## Tech Stack

- **Backend**: Spring Boot 3, Spring Security, Spring Data JPA, H2 (in-memory DB), JWT
- **Frontend**: React 18, React Router v6, Axios, Tailwind CSS

---

## How to Run

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Or in IntelliJ/Eclipse: Open the project and run `KycOnboardingApplication.java`.

The backend starts on **http://localhost:8080**

### Frontend

```bash
cd frontend
npm install
npm start
```

The frontend starts on **http://localhost:3000**

> The frontend proxies API requests to port 8080 automatically (configured in `package.json`).

---

## Test Credentials

These are auto-created when the backend starts:

| Username   | Password     | Role     |
|------------|--------------|----------|
| merchant1  | password123  | MERCHANT |
| merchant2  | password123  | MERCHANT |
| reviewer1  | password123  | REVIEWER |

---

## H2 Console

Access the database directly:

**URL**: http://localhost:8080/h2-console

Connection settings:
- JDBC URL: `jdbc:h2:mem:kycdb`
- Username: `sa`
- Password: *(leave empty)*

---

## API Routes

### Auth (Public)
| Method | Endpoint               | Description       |
|--------|------------------------|-------------------|
| POST   | /api/v1/auth/signup    | Register merchant |
| POST   | /api/v1/auth/login     | Login             |

### Merchant (Requires JWT + MERCHANT role)
| Method | Endpoint                              | Description             |
|--------|---------------------------------------|-------------------------|
| GET    | /api/v1/merchant/submissions          | List my submissions     |
| GET    | /api/v1/merchant/submissions/{id}     | View one submission     |
| POST   | /api/v1/merchant/submissions          | Create draft            |
| PUT    | /api/v1/merchant/submissions/{id}     | Update draft            |
| POST   | /api/v1/merchant/submissions/{id}/submit | Submit for review    |
| POST   | /api/v1/merchant/upload               | Upload document         |

### Reviewer (Requires JWT + REVIEWER role)
| Method | Endpoint                              | Description             |
|--------|---------------------------------------|-------------------------|
| GET    | /api/v1/reviewer/queue                | View review queue       |
| GET    | /api/v1/reviewer/submissions          | All submissions         |
| GET    | /api/v1/reviewer/submissions/{id}     | View one submission     |
| PATCH  | /api/v1/reviewer/submissions/{id}/action | Take action (approve/reject/etc) |
| GET    | /api/v1/reviewer/stats                | Dashboard stats         |

---

## KYC Status Flow

```
DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED
                                 → REJECTED
                                 → MORE_INFO_REQUESTED → SUBMITTED (loop)
```

---

## Running Tests

```bash
cd backend
./mvnw test
```
