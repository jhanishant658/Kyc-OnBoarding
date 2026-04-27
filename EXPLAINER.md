# EXPLAINER.md — Technical Deep Dive

This document explains the key technical decisions in the KYC system, written for interview preparation.

---

## 1. State Machine Implementation

The KYC process has defined statuses: `DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED / REJECTED / MORE_INFO_REQUESTED`.

### How it's built

All transition logic lives in **one single class**: `KycStateMachineService.java`.

```java
private static final Map<KycStatus, Set<KycStatus>> ALLOWED_TRANSITIONS = new HashMap<>();

static {
    ALLOWED_TRANSITIONS.put(KycStatus.DRAFT, Set.of(KycStatus.SUBMITTED));
    ALLOWED_TRANSITIONS.put(KycStatus.SUBMITTED, Set.of(KycStatus.UNDER_REVIEW));
    ALLOWED_TRANSITIONS.put(KycStatus.UNDER_REVIEW, Set.of(APPROVED, REJECTED, MORE_INFO_REQUESTED));
    ALLOWED_TRANSITIONS.put(KycStatus.MORE_INFO_REQUESTED, Set.of(KycStatus.SUBMITTED));
    ALLOWED_TRANSITIONS.put(KycStatus.APPROVED, Set.of());   // terminal
    ALLOWED_TRANSITIONS.put(KycStatus.REJECTED, Set.of());   // terminal
}
```

When a transition is requested, we check if the new status is in the allowed set:

```java
Set<KycStatus> allowedNext = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
if (!allowedNext.contains(newStatus)) {
    throw new IllegalStateException("Invalid transition from " + currentStatus + " to " + newStatus);
}
```

**Why one class?** If status checks were scattered across controllers, it would be easy to miss one and create a security gap. Centralizing it means there's only one place to update or debug.

**Invalid transitions return HTTP 400** with a clear error message, handled by the controller catching `IllegalStateException`.

---

## 2. Auth Protection

### JWT-based authentication

1. User logs in → backend verifies password → returns a JWT token
2. Frontend stores token in `localStorage`
3. Every API request includes `Authorization: Bearer <token>`
4. `JwtAuthFilter` intercepts every request, validates the token, and loads the user into Spring Security's context

### Role-based access

- Merchant routes (`/api/v1/merchant/**`) are protected by authentication
- Reviewer routes (`/api/v1/reviewer/**`) are additionally restricted with `.hasAuthority("ROLE_REVIEWER")` in `SecurityConfig`
- **Ownership check** for merchants: In `KycSubmissionService`, before returning any submission, we verify:

```java
if (!sub.getMerchant().getUsername().equals(username)) {
    throw new SecurityException("Access denied: this submission does not belong to you.");
}
```

This ensures merchant1 cannot access merchant2's data even if they guess the submission ID.

---

## 3. File Validation

File validation is handled in `FileValidationService.java`:

```java
private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");

public void validate(MultipartFile file, String fieldName) {
    if (file.getSize() > MAX_SIZE_BYTES) {
        throw new IllegalArgumentException(fieldName + " exceeds 5MB limit");
    }
    if (!ALLOWED_TYPES.contains(file.getContentType())) {
        throw new IllegalArgumentException(fieldName + " has invalid type: " + file.getContentType());
    }
}
```

The controller catches `IllegalArgumentException` and returns HTTP 400 with a clear JSON error message.

**Why not rely on file extension?** A user could rename `malware.exe` to `document.pdf`. Checking `Content-Type` (MIME type) is more reliable (though still not perfect — in production you'd use a library like Apache Tika to read the actual file header bytes).

---

## 4. Reviewer Queue Query

The queue shows submissions in `SUBMITTED` or `UNDER_REVIEW` status, ordered oldest first (so no submission is left waiting indefinitely):

```java
@Query("SELECT s FROM KycSubmission s WHERE s.status IN ('SUBMITTED', 'UNDER_REVIEW') ORDER BY s.submittedAt ASC")
List<KycSubmission> findReviewQueue();
```

**Why `submittedAt` instead of `createdAt`?**  
A submission could be created as a DRAFT and stay there for days before being submitted. We want to track how long it has been *waiting for review*, not how long ago it was created.

---

## 5. SLA Tracking (Dynamic `atRisk` Field)

The `atRisk` flag tells reviewers when a submission has been waiting too long (> 24 hours).

**Important design decision: this field is NOT stored in the database.**

Instead, it is calculated every time we convert an entity to a DTO:

```java
if (sub.getSubmittedAt() != null &&
    (sub.getStatus() == SUBMITTED || sub.getStatus() == UNDER_REVIEW)) {
    long hoursInQueue = Duration.between(sub.getSubmittedAt(), LocalDateTime.now()).toHours();
    dto.setAtRisk(hoursInQueue > 24);
}
```

**Why dynamic?** A stored boolean would go stale immediately. A submission submitted 23 hours ago would have `atRisk = false` stored, but 2 hours later it becomes at risk. By computing it at query time, it's always accurate.

---

## 6. Example: AI-Generated Code That Was Buggy/Insecure

### The Bug: Missing Ownership Check

When I asked an AI assistant to generate the merchant endpoint for fetching a submission, it initially produced:

```java
// BUGGY VERSION (no ownership check)
@GetMapping("/submissions/{id}")
public ResponseEntity<?> getSubmission(@PathVariable Long id) {
    KycSubmission sub = submissionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Not found"));
    return ResponseEntity.ok(sub);
}
```

**The problem**: Any logged-in merchant could call `/api/v1/merchant/submissions/5` and retrieve merchant2's private KYC data just by guessing or incrementing IDs (known as an IDOR — Insecure Direct Object Reference attack).

### The Fix

The corrected version verifies ownership before returning data:

```java
// FIXED VERSION (with ownership check)
@GetMapping("/submissions/{id}")
public ResponseEntity<?> getSubmission(@PathVariable Long id, Authentication auth) {
    try {
        // Service layer checks that submission belongs to this merchant
        return ResponseEntity.ok(submissionService.getSubmissionForMerchant(id, auth.getName()));
    } catch (SecurityException e) {
        return ResponseEntity.status(403).body(error(e.getMessage()));
    }
}
```

And in the service:

```java
public KycSubmissionDto getSubmissionForMerchant(Long id, String username) {
    KycSubmission sub = findSubmission(id);
    if (!sub.getMerchant().getUsername().equals(username)) {
        throw new SecurityException("Access denied: this submission does not belong to you.");
    }
    return toDto(sub);
}
```

**Lesson**: AI-generated CRUD code often skips authorization checks because it doesn't know about multi-tenancy or data ownership requirements. Always review generated code for missing access control.

---

## Project Structure Summary

```
backend/
  src/main/java/com/kyc/
    config/         → SecurityConfig, DataSeeder
    controller/     → AuthController, MerchantController, ReviewerController
    dto/            → KycSubmissionDto, AuthDto, ReviewActionDto
    entity/         → User, KycSubmission, NotificationEvent
    repository/     → UserRepository, KycSubmissionRepository, NotificationEventRepository
    security/       → JwtUtil, JwtAuthFilter
    service/        → KycSubmissionService, KycStateMachineService, FileValidationService, UserDetailsServiceImpl

frontend/
  src/
    context/        → AuthContext (login/logout state)
    services/       → api.js (all API calls in one place)
    components/     → Navbar, StatusBadge
    pages/          → LoginPage, SignupPage, MerchantDashboard, ReviewerDashboard,
                       SubmissionForm, SubmissionDetail
```
