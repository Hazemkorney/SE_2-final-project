# ✅ Hospital System — Full Implementation Verification Prompt

> أديه للـ AI بعد ما يخلص الـ implementation وقوله:
> **"Go through every item in this checklist and verify it exists in the code. For each item, tell me: ✅ Done / ❌ Missing / ⚠️ Partially done — and show me the exact file and line where it's implemented."**

---

## 1. 🏗️ PROJECT STRUCTURE

- [ ] `eureka-server/` folder exists with `pom.xml` and `src/`
- [ ] `api-gateway/` folder exists with `pom.xml` and `src/`
- [ ] `auth-service/` folder exists with `pom.xml` and `src/`
- [ ] `main-service/` folder exists with `pom.xml` and `src/`
- [ ] `frontend/` folder exists
- [ ] `docker-compose.yml` exists at root level
- [ ] Each service has its own `Dockerfile`

---

## 2. 🔍 EUREKA SERVER

- [ ] `@EnableEurekaServer` annotation present on main class
- [ ] `application.yml` has `server.port: 8761`
- [ ] `register-with-eureka: false` and `fetch-registry: false` set (server doesn't register itself)
- [ ] All other services have `eureka-client` dependency and point to `http://eureka-server:8761/eureka/`

---

## 3. 🚪 API GATEWAY

- [ ] Routes `/auth/**` → `auth-service`
- [ ] Routes `/api/**` → `main-service`
- [ ] JWT validation filter exists (`GatewayFilter` or `GlobalFilter`)
- [ ] Filter extracts `userId` and `role` from JWT and adds them as headers: `X-User-Id`, `X-User-Role`
- [ ] `/auth/login` is whitelisted (no JWT required)
- [ ] Invalid/missing JWT returns `401 Unauthorized`

---

## 4. 🔐 AUTH SERVICE

### Database
- [ ] `users` table created with columns: `id`, `email`, `password`, `role`, `created_at`
- [ ] `email` column has UNIQUE constraint
- [ ] Password is stored **BCrypt hashed** (NOT plain text)

### Endpoints
- [ ] `POST /auth/login` — validates credentials, returns JWT token + role
- [ ] `POST /auth/register-internal` — creates user, returns userId (internal use only)
- [ ] `DELETE /auth/users/{userId}` — deletes user by ID (internal use only)

### JWT
- [ ] JWT contains: `userId`, `role`, `email`
- [ ] JWT is signed with secret key from environment variable (not hardcoded)
- [ ] JWT expiration is set

### Security
- [ ] `/auth/login` is publicly accessible
- [ ] `/auth/register-internal` and `/auth/users/**` are NOT publicly exposed (protected or internal only)

### Default Admin
- [ ] On startup, if no ADMIN exists → creates default admin (`admin@hospital.com / admin123`)

---

## 5. 🏥 MAIN SERVICE

### Entities & Database
- [ ] `Department` entity: `id`, `name`
- [ ] `Doctor` entity: `id`, `name`, `specialization`, `departmentId`, `userId`
- [ ] `Receptionist` entity: `id`, `name`, `userId`
- [ ] `Patient` entity: `id`, `name`, `phone`
- [ ] `Appointment` entity: `id`, `doctorId`, `patientId`, `appointmentDate`, `slotStart`, `slotEnd`, `status`, `version`
- [ ] `@Version` annotation on `Appointment.version` field ✅ (Optimistic Locking)
- [ ] `status` field has values: `BOOKED`, `CANCELLED`

### Admin — Department Module
- [ ] `GET /api/admin/departments` → returns list of all departments
- [ ] `POST /api/admin/departments` → creates new department
- [ ] `DELETE /api/admin/departments/{id}` → **fails with error if department has doctors**
- [ ] Only `ADMIN` role can access these endpoints

### Admin — Doctor Module
- [ ] `GET /api/admin/doctors` → returns all doctors
- [ ] `POST /api/admin/doctors` → calls auth-service to create user, saves doctor with returned userId
- [ ] `DELETE /api/admin/doctors/{id}` → calls auth-service to delete user, deletes doctor + appointments
- [ ] Only `ADMIN` role can access these endpoints

### Admin — Receptionist Module
- [ ] `GET /api/admin/receptionists` → returns all receptionists
- [ ] `POST /api/admin/receptionists` → calls auth-service to create user, saves receptionist
- [ ] `DELETE /api/admin/receptionists/{id}` → calls auth-service to delete user, deletes receptionist
- [ ] Only `ADMIN` role can access these endpoints

### Receptionist — Appointment Module
- [ ] `GET /api/receptionist/departments` → list departments
- [ ] `GET /api/receptionist/departments/{deptId}/doctors` → list doctors in department
- [ ] `GET /api/receptionist/doctors/{doctorId}/slots` → returns 16 slots for today
  - [ ] Each slot has: `slotStart`, `slotEnd`, `available` (true/false), `isPast` (true/false)
  - [ ] Slots go from `09:00` to `16:30` (every 30 min)
  - [ ] Past slots (slotEnd < now) marked as `isPast: true`
  - [ ] Booked slots marked as `available: false`
- [ ] `POST /api/receptionist/appointments` → books appointment
  - [ ] Checks slot is not already booked
  - [ ] Checks slot is not in the past
  - [ ] Reuses existing patient if same phone number exists
  - [ ] `appointmentDate` is forced to `LocalDate.now()` (ignores any date in request)
- [ ] `DELETE /api/receptionist/appointments/{id}` → sets status to `CANCELLED`
- [ ] `PUT /api/receptionist/appointments/{id}/reschedule` → cancels old, creates new appointment
- [ ] Only `RECEPTIONIST` role can access these endpoints

### Doctor Module
- [ ] `GET /api/doctor/my-appointments` → returns today's BOOKED appointments for logged-in doctor only
- [ ] Response contains: `slotStart`, `slotEnd`, `patientName`
- [ ] Doctor can ONLY see their own appointments (uses `X-User-Id` header)
- [ ] Only `DOCTOR` role can access this endpoint

---

## 6. 🔄 AOP

- [ ] Custom `@RequiresRole` annotation exists
- [ ] `RoleAuthorizationAspect` exists with `@Around` advice
- [ ] Aspect reads role from `SecurityContext` or request header
- [ ] Throws `AccessDeniedException` (403) if role doesn't match
- [ ] `@RequiresRole` is applied on **at least 4 different service methods**
- [ ] `LoggingAspect` exists with `@Around` advice
- [ ] Logging aspect logs: method name, arguments, execution time
- [ ] Logging aspect logs exceptions if they occur

---

## 7. 🔒 CONCURRENCY

- [ ] `@Version` on `Appointment` entity (already checked above)
- [ ] Booking method has `@Transactional(isolation = Isolation.SERIALIZABLE)`
- [ ] `OptimisticLockingFailureException` is caught and returns meaningful error (e.g., "Slot just got booked, please try another")

---

## 8. 🔗 INTER-SERVICE COMMUNICATION

- [ ] Main service uses `WebClient` (or `RestTemplate`) to call auth-service
- [ ] Auth service URL is injected via environment variable (NOT hardcoded as `localhost`)
- [ ] When creating doctor: auth account created FIRST, then doctor record saved
- [ ] When deleting doctor: auth account deleted, then doctor + appointments deleted
- [ ] If auth-service call fails → transaction rolls back (no orphan records)

---

## 9. 🐳 DOCKER

- [ ] `Dockerfile` in each service builds a runnable JAR
- [ ] `docker-compose.yml` includes: `sqlserver`, `eureka-server`, `auth-service`, `main-service`, `api-gateway`, `frontend`
- [ ] Services depend on each other correctly (`depends_on`)
- [ ] SQL Server password and connection strings passed via environment variables
- [ ] JWT secret passed via environment variable (same value for all services)
- [ ] `docker-compose up` starts the entire system without manual steps

---

## 10. 🖥️ FRONTEND

- [ ] Login page works for all 3 roles and redirects correctly
- [ ] JWT stored after login (localStorage or similar)
- [ ] JWT sent in `Authorization: Bearer <token>` header on every request

### Admin UI
- [ ] Can view, create, delete departments
- [ ] Cannot delete department that has doctors (shows error message)
- [ ] Can view, create, delete doctors
- [ ] Can view, create, delete receptionists

### Receptionist UI
- [ ] Department list → Doctor list → Slots grid flow works
- [ ] Slots grid shows 16 slots color-coded (available/booked/past)
- [ ] Booking modal asks for patient name + phone only
- [ ] Can cancel appointments
- [ ] Can reschedule appointments

### Doctor UI
- [ ] Shows only today's appointments
- [ ] Read-only (no buttons for cancel/modify)

---

## 11. ⚙️ CONFIGURATION & BEST PRACTICES

- [ ] No hardcoded secrets or URLs in code (all in `application.yml` or env variables)
- [ ] Each service has its own `application.yml` with correct service name
- [ ] CORS configured properly so frontend can call the gateway
- [ ] Meaningful error messages returned (not just 500 Internal Server Error)
- [ ] `@RestControllerAdvice` (Global Exception Handler) exists in main-service

---

## 📊 FINAL SCORE

بعد ما الـ AI يفحص كل حاجة، قوله:

> **"Give me a final summary: how many items are ✅ Done, ❌ Missing, ⚠️ Partial? List all missing or partial items and implement them now."**

---

*اللي عنده ✅ على كل النقط دي — عنده full implementation حقيقي.*
