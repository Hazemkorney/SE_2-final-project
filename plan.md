# 🏥 Hospital Appointment Booking System — Full Implementation Prompt

> **Give this entire document to an AI (e.g., Claude, ChatGPT, Cursor, GitHub Copilot) and ask it to implement the system step by step.**

---

## 🎯 PROJECT GOAL

Build a **production-ready, microservices-based Hospital Appointment Booking System** using the exact tech stack and business rules described below. Implement everything from scratch: backend microservices, database schemas, Docker setup, and a working frontend.

---

## 🏗️ ARCHITECTURE OVERVIEW

```
┌─────────────────────────────────────────────────────────┐
│                    FRONTEND (Any tech)                  │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP
┌────────────────────────▼────────────────────────────────┐
│              API GATEWAY (Spring Cloud Gateway)         │
│                      PORT: 8080                         │
└──────────┬──────────────────────────┬───────────────────┘
           │                          │
┌──────────▼──────────┐   ┌───────────▼───────────────────┐
│    AUTH SERVICE     │   │        MAIN SERVICE            │
│     PORT: 8081      │   │          PORT: 8082            │
│   DB: auth_db       │   │        DB: main_db             │
└──────────┬──────────┘   └───────────┬───────────────────┘
           │                          │
           └──────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│           EUREKA SERVICE DISCOVERY (PORT: 8761)         │
└─────────────────────────────────────────────────────────┘
```

**Services:**
1. `eureka-server` — Service Discovery
2. `api-gateway` — Spring Cloud Gateway (routes all requests)
3. `auth-service` — Authentication & Authorization (JWT)
4. `main-service` — Business Logic (departments, doctors, appointments)

---

## ⚙️ TECHNOLOGY STACK

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Build Tool | Maven |
| Backend Framework | Spring Boot 3.x |
| Microservices | Spring Cloud (Eureka + Gateway) |
| Authentication | JWT + Spring Security |
| Database | Microsoft SQL Server (separate DB per service) |
| AOP | Spring AOP |
| Concurrency | Optimistic Locking (`@Version`) + `SERIALIZABLE` isolation |
| Containerization | Docker + Docker Compose |
| Frontend | React  |

---

## 📁 PROJECT STRUCTURE

```
hospital-system/
├── docker-compose.yml
├── eureka-server/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── api-gateway/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── auth-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── main-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
└── frontend/
    ├── Dockerfile
    └── src/
```

---

## 👥 ROLES & PERMISSIONS

| Role | Permissions |
|---|---|
| **Admin** | Create/delete department (only if no doctors exist in it), Create/delete doctor account (linked to department), Create/delete receptionist account. **CANNOT make bookings.** |
| **Doctor** | View today's booked appointments only (time slot + patient name). **Cannot modify, cancel, or confirm anything.** |
| **Receptionist** | View departments → doctors → available slots for today, Book appointment (patient name + phone only), Cancel appointment, Reschedule appointment (change time or doctor) |

> ⚠️ Admin cannot book. Doctor cannot modify anything. Only Receptionist books.

---

## 🗃️ DATABASE SCHEMAS

### Auth Service DB (`auth_db`)

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY IDENTITY,
    email NVARCHAR(255) UNIQUE NOT NULL,
    password NVARCHAR(255) NOT NULL,      -- BCrypt hashed
    role NVARCHAR(50) NOT NULL,           -- ADMIN, DOCTOR, RECEPTIONIST
    created_at DATETIME2 DEFAULT GETDATE()
);
```

### Main Service DB (`main_db`)

```sql
CREATE TABLE departments (
    id BIGINT PRIMARY KEY IDENTITY,
    name NVARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE doctors (
    id BIGINT PRIMARY KEY IDENTITY,
    name NVARCHAR(255) NOT NULL,
    specialization NVARCHAR(255),
    department_id BIGINT NOT NULL REFERENCES departments(id),
    user_id BIGINT NOT NULL UNIQUE        -- FK to auth_service users.id
);

CREATE TABLE receptionists (
    id BIGINT PRIMARY KEY IDENTITY,
    name NVARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL UNIQUE        -- FK to auth_service users.id
);

CREATE TABLE patients (
    id BIGINT PRIMARY KEY IDENTITY,
    name NVARCHAR(255) NOT NULL,
    phone NVARCHAR(50) NOT NULL
);

CREATE TABLE appointments (
    id BIGINT PRIMARY KEY IDENTITY,
    doctor_id BIGINT NOT NULL REFERENCES doctors(id),
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    appointment_date DATE NOT NULL,       -- Always today's date
    slot_start TIME NOT NULL,             -- e.g. 09:00, 09:30 ... 16:30
    slot_end TIME NOT NULL,               -- slot_start + 30 minutes
    status NVARCHAR(50) DEFAULT 'BOOKED', -- BOOKED, CANCELLED
    version BIGINT DEFAULT 0              -- For Optimistic Locking (@Version)
);
```

---

## 🔌 API ENDPOINTS

### AUTH SERVICE (`/auth/**`)

```
POST /auth/login
  Body: { "email": "...", "password": "..." }
  Response: { "token": "JWT_TOKEN", "role": "ADMIN|DOCTOR|RECEPTIONIST" }

POST /auth/register-internal   (called internally by main-service only, not exposed publicly)
  Body: { "email": "...", "password": "...", "role": "..." }
  Response: { "userId": 1 }

DELETE /auth/users/{userId}    (called internally by main-service only)
```

### MAIN SERVICE — Admin Endpoints (`/api/admin/**`)

```
# Department Management
GET    /api/admin/departments              → List all departments
POST   /api/admin/departments              → Create department
       Body: { "name": "Cardiology" }
DELETE /api/admin/departments/{id}         → Delete (fails if has doctors)

# Doctor Management
GET    /api/admin/doctors                  → List all doctors
POST   /api/admin/doctors                  → Create doctor + auth account
       Body: { "name": "...", "email": "...", "password": "...", "specialization": "...", "departmentId": 1 }
DELETE /api/admin/doctors/{id}             → Delete doctor + auth account

# Receptionist Management
GET    /api/admin/receptionists            → List all receptionists
POST   /api/admin/receptionists            → Create receptionist + auth account
       Body: { "name": "...", "email": "...", "password": "..." }
DELETE /api/admin/receptionists/{id}       → Delete receptionist + auth account
```

### MAIN SERVICE — Receptionist Endpoints (`/api/receptionist/**`)

```
GET  /api/receptionist/departments                           → All departments
GET  /api/receptionist/departments/{deptId}/doctors          → Doctors in department
GET  /api/receptionist/doctors/{doctorId}/slots              → Available slots for today
     Response: [{ "slotStart": "09:00", "slotEnd": "09:30", "available": true/false, "isPast": true/false }]

POST /api/receptionist/appointments                          → Book appointment
     Body: { "doctorId": 1, "slotStart": "10:00", "patientName": "...", "patientPhone": "..." }

DELETE /api/receptionist/appointments/{id}                   → Cancel appointment

PUT  /api/receptionist/appointments/{id}/reschedule          → Reschedule
     Body: { "newDoctorId": 2, "newSlotStart": "14:00" }
```

### MAIN SERVICE — Doctor Endpoints (`/api/doctor/**`)

```
GET /api/doctor/my-appointments    → Today's booked appointments for logged-in doctor
    Response: [{ "slotStart": "09:00", "slotEnd": "09:30", "patientName": "..." }]
```

---

## ⏰ SLOT GENERATION LOGIC

```
Working hours: 09:00 to 17:00
Slot duration: 30 minutes
Total slots per doctor per day: 16

Slots: 09:00-09:30, 09:30-10:00, 10:00-10:30, ..., 16:30-17:00

When Receptionist views slots:
  - Show ALL 16 slots
  - Mark slot as "BOOKED" if an appointment exists for that doctor + today + that slot
  - Mark slot as "PAST" if slot_end < current time (disable it in UI)
  - Only AVAILABLE (not booked, not past) slots can be selected for booking
```

---

## 🔐 SECURITY IMPLEMENTATION

### JWT Flow
```
1. User POSTs credentials to /auth/login
2. Auth Service validates → returns JWT token
3. Client sends token in header: Authorization: Bearer <token>
4. API Gateway forwards token to microservices
5. Each microservice validates JWT and extracts role
```

### Spring Security Config (per microservice)
```java
// Permit: /auth/login
// All other endpoints require authentication
// Role-based access via @PreAuthorize or custom AOP
```

---

## 🔄 AOP IMPLEMENTATION

### 1. Custom `@RequiresRole` Annotation
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {
    String[] value(); // e.g., {"ADMIN"}, {"RECEPTIONIST"}
}
```

### 2. Security Aspect
```java
@Aspect
@Component
public class RoleAuthorizationAspect {
    @Around("@annotation(requiresRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequiresRole requiresRole) {
        // Extract role from SecurityContext
        // If role not in requiresRole.value() → throw AccessDeniedException
        // Else → proceed
    }
}
```

### 3. Logging Aspect
```java
@Aspect
@Component
public class LoggingAspect {
    @Around("execution(* com.hospital..*Service.*(..))")
    public Object logExecution(ProceedingJoinPoint joinPoint) {
        // Log: method name, arguments, execution time, result/exception
    }
}
```

### Usage Example
```java
@RequiresRole({"ADMIN"})
public DepartmentResponse createDepartment(DepartmentRequest request) { ... }

@RequiresRole({"RECEPTIONIST"})
public AppointmentResponse bookAppointment(BookingRequest request) { ... }
```

---

## 🔒 CONCURRENCY HANDLING

### Problem
Multiple receptionists may try to book the same slot for the same doctor simultaneously.

### Solution: Optimistic Locking + SERIALIZABLE Transaction

```java
// Appointment entity
@Entity
public class Appointment {
    @Version
    private Long version; // Optimistic locking
    // ... other fields
}

// Booking service method
@Transactional(isolation = Isolation.SERIALIZABLE)
public AppointmentResponse bookAppointment(BookingRequest request) {
    // 1. Check if slot is already booked (within SERIALIZABLE transaction)
    // 2. If booked → throw SlotAlreadyBookedException
    // 3. If available → save new appointment
    // 4. If OptimisticLockingFailureException caught → retry or return error
}
```

---

## 🐳 DOCKER SETUP

### `docker-compose.yml`
```yaml
version: '3.8'

services:
  sqlserver:
    image: mcr.microsoft.com/mssql/server:2022-latest
    environment:
      SA_PASSWORD: "Hospital@123"
      ACCEPT_EULA: "Y"
    ports:
      - "1433:1433"
    volumes:
      - sqldata:/var/opt/mssql

  eureka-server:
    build: ./eureka-server
    ports:
      - "8761:8761"
    depends_on:
      - sqlserver

  auth-service:
    build: ./auth-service
    ports:
      - "8081:8081"
    depends_on:
      - eureka-server
      - sqlserver
    environment:
      - SPRING_DATASOURCE_URL=jdbc:sqlserver://sqlserver:1433;databaseName=auth_db;trustServerCertificate=true
      - SPRING_DATASOURCE_USERNAME=sa
      - SPRING_DATASOURCE_PASSWORD=Hospital@123
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - JWT_SECRET=your_super_secret_jwt_key_minimum_32_chars

  main-service:
    build: ./main-service
    ports:
      - "8082:8082"
    depends_on:
      - eureka-server
      - auth-service
      - sqlserver
    environment:
      - SPRING_DATASOURCE_URL=jdbc:sqlserver://sqlserver:1433;databaseName=main_db;trustServerCertificate=true
      - SPRING_DATASOURCE_USERNAME=sa
      - SPRING_DATASOURCE_PASSWORD=Hospital@123
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - JWT_SECRET=your_super_secret_jwt_key_minimum_32_chars
      - AUTH_SERVICE_URL=http://auth-service:8081

  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
    depends_on:
      - eureka-server
      - auth-service
      - main-service
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - JWT_SECRET=your_super_secret_jwt_key_minimum_32_chars

  frontend:
    build: ./frontend
    ports:
      - "3000:3000"
    depends_on:
      - api-gateway

volumes:
  sqldata:
```

---

## 🗺️ INTER-SERVICE COMMUNICATION

### When Admin creates a Doctor:
```
1. Admin → POST /api/admin/doctors (main-service)
2. main-service → POST /auth/register-internal (auth-service) → gets userId
3. main-service saves Doctor entity with userId
4. Returns success to Admin
```

### When Admin deletes a Doctor:
```
1. Admin → DELETE /api/admin/doctors/{id} (main-service)
2. main-service → DELETE /auth/users/{userId} (auth-service)
3. main-service deletes all appointments for that doctor
4. main-service deletes doctor record
```

Use **Spring WebClient** (or RestTemplate) for inter-service HTTP calls. Auth service URL is injected via environment variable.

---

## 📋 SPRING BOOT DEPENDENCIES (per service)

### Eureka Server
```xml
<dependency>spring-cloud-starter-netflix-eureka-server</dependency>
```

### API Gateway
```xml
<dependency>spring-cloud-starter-gateway</dependency>
<dependency>spring-cloud-starter-netflix-eureka-client</dependency>
<dependency>jjwt-api + jjwt-impl + jjwt-jackson</dependency>
```

### Auth Service
```xml
<dependency>spring-boot-starter-web</dependency>
<dependency>spring-boot-starter-security</dependency>
<dependency>spring-boot-starter-data-jpa</dependency>
<dependency>spring-cloud-starter-netflix-eureka-client</dependency>
<dependency>jjwt-api + jjwt-impl + jjwt-jackson</dependency>
<dependency>mssql-jdbc</dependency>
<dependency>spring-boot-starter-aop</dependency>
```

### Main Service
```xml
<dependency>spring-boot-starter-web</dependency>
<dependency>spring-boot-starter-security</dependency>
<dependency>spring-boot-starter-data-jpa</dependency>
<dependency>spring-cloud-starter-netflix-eureka-client</dependency>
<dependency>spring-boot-starter-webflux</dependency>  <!-- for WebClient -->
<dependency>jjwt-api + jjwt-impl + jjwt-jackson</dependency>
<dependency>mssql-jdbc</dependency>
<dependency>spring-boot-starter-aop</dependency>
```

---

## 🧩 KEY IMPLEMENTATION NOTES

1. **Default Admin**: On startup, if no ADMIN user exists in `users` table, insert one automatically (via `CommandLineRunner` or `DataInitializer`).
   - Default: `admin@hospital.com / admin123`

2. **JWT Secret**: Must be the same across all services (injected via env variable).

3. **API Gateway Routes**: Route `/auth/**` → auth-service, `/api/**` → main-service. Gateway should also validate JWT and pass `X-User-Role` and `X-User-Id` headers to downstream services.

4. **Department Deletion Guard**: Before deleting a department, check if any doctor references it → if yes, throw exception with message "Cannot delete department with existing doctors."

5. **Slot Generation**: Don't store slots in DB. Generate them dynamically when the endpoint is called. Only appointments are stored.

6. **Date Constraint**: All appointments must have `appointment_date = LocalDate.now()`. Reject any booking attempt for another date.

7. **Reschedule**: Cancel old appointment (set status = 'CANCELLED') and create a new appointment for the new slot/doctor.

8. **Patient Reuse**: Before creating a new patient, check if patient with same phone already exists → reuse existing record.

---

## 🖥️ FRONTEND REQUIREMENTS

Build a clean, functional frontend (React preferred) with these pages:

### Login Page (All roles)
- Email + password form → calls `/auth/login` → stores JWT in localStorage → redirects based on role

### Admin Dashboard
- Tabs: Departments | Doctors | Receptionists
- Each tab: list + add button + delete button

### Receptionist Dashboard
- Step 1: Select Department → Step 2: Select Doctor → Step 3: View slots grid
- Slots grid: 4 columns × 4 rows (16 slots), color-coded: Green=Available, Red=Booked, Gray=Past
- Click available slot → modal: enter patient name + phone → confirm booking
- Appointments list: show today's bookings with Cancel and Reschedule buttons

### Doctor Dashboard
- Simple table: today's appointments (time + patient name)
- Read-only, no actions

---

## ✅ IMPLEMENTATION ORDER (Follow this sequence)

```
Step 1: eureka-server (minimal, just @EnableEurekaServer)
Step 2: auth-service (users table, login endpoint, JWT generation, register-internal endpoint)
Step 3: main-service (all entities, repositories, services, controllers)
Step 4: api-gateway (routing + JWT validation filter)
Step 5: AOP aspects (logging + @RequiresRole)
Step 6: Docker (Dockerfile for each + docker-compose.yml)
Step 7: Frontend
Step 8: Test end-to-end flow
```

---

## 🧪 TEST SCENARIOS TO VERIFY

1. Admin logs in → creates Department "Cardiology"
2. Admin creates Doctor "Dr. Ahmed" in Cardiology → verify auth account created
3. Receptionist logs in → navigates to Cardiology → Dr. Ahmed → sees 16 slots
4. Receptionist books 10:00-10:30 slot for patient "John, 0501234567"
5. Another receptionist tries to book same slot → gets error (concurrency test)
6. Doctor logs in → sees John's appointment at 10:00
7. Receptionist reschedules John to 11:00
8. Receptionist cancels the appointment
9. Admin tries to delete Cardiology (with Dr. Ahmed in it) → gets error
10. Admin deletes Dr. Ahmed → verify auth account also deleted

---

*End of Implementation Prompt*
