# Feladatkezelő alkalmazás

Webalapú feladatkezelő (task manager) Spring Boot backenddel, JWT autentikációval és Docker támogatással.

---

## Alkalmazás felépítése

```
taskmanager/
├── src/
│   ├── main/java/com/taskmanager/
│   │   ├── controller/       # REST végpontok (AuthController, TaskController)
│   │   ├── model/            # JPA entitások (User, Task)
│   │   ├── repository/       # Spring Data JPA repository-k
│   │   ├── service/          # (bővítésre fenntartva)
│   │   ├── security/         # JWT logika, Spring Security konfiguráció
│   │   └── dto/              # Kérés/válasz DTO osztályok
│   └── main/resources/
│       └── application.properties
├── src/test/                 # Integrációs tesztek
├── frontend/                 # Statikus HTML/CSS/JS kliens
│   ├── index.html
│   ├── css/style.css
│   └── js/app.js
├── init.sql                  # Adatbázis inicializáló script
├── Dockerfile
├── docker-compose.yml
└── .github/workflows/ci-cd.yml
```

### Technológiai stack

| Réteg      | Technológia                    |
|------------|-------------------------------|
| Backend    | Java 17, Spring Boot 3.2      |
| Adatbázis  | PostgreSQL 16, Spring Data JPA |
| Auth       | JWT (jjwt 0.11.5)             |
| Frontend   | Vanilla HTML / CSS / JavaScript|
| Konténer   | Docker, Docker Compose         |
| CI/CD      | GitHub Actions                 |

---

## Telepítési útmutató

### 1. Előfeltételek

- Docker és Docker Compose telepítve
- (Fejlesztéshez: Java 17, Maven)

### 2. Gyors indítás Dockerrel

```bash
# Klónozd a repót
git clone <repo-url>
cd taskmanager

# Indítás
docker compose up --build

# Az alkalmazás elérhető: http://localhost:8080
```

### 3. Fejlesztői indítás (Docker nélkül)

```bash
# 1. Adatbázis inicializálása
psql -U postgres -f init.sql

# 2. Alkalmazás indítása
mvn spring-boot:run

# 3. Frontend megnyitása
# Nyisd meg: frontend/index.html
# (vagy konfiguráld a Spring-et statikus fájlok kiszolgálására)
```

---

## Konfiguráció

Az `src/main/resources/application.properties` fájlban:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/taskmanager
spring.datasource.username=taskuser
spring.datasource.password=taskpassword

app.jwt.secret=<titkos-kulcs-min-32-karakter>
app.jwt.expiration=86400000   # 24 óra milliszekundumban
```

> **Fontos:** Éles környezetben a `app.jwt.secret` értékét környezeti változóként add meg, ne commitold be!

Docker Compose esetén a konfiguráció a `docker-compose.yml` `environment` szekciójában adható meg.

---

## API végpontok

### Autentikáció

#### `POST /api/auth/register` – Regisztráció

**Kérés törzse:**
```json
{
  "username": "kovacs_janos",
  "email": "janos@example.com",
  "password": "jelszo123"
}
```

**Válasz (201 Created):**
```json
{ "message": "Sikeres regisztráció!" }
```

**Hibák:**
- `400 Bad Request` – foglalt felhasználónév/email, vagy hiányzó mező

---

#### `POST /api/auth/login` – Bejelentkezés

**Kérés törzse:**
```json
{
  "username": "kovacs_janos",
  "password": "jelszo123"
}
```

**Válasz (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "kovacs_janos"
}
```

**Hibák:**
- `401 Unauthorized` – hibás belépési adatok

---

### Feladatok

> Minden feladat-végpont az `Authorization: Bearer <token>` fejlécet igényli.

#### `GET /api/tasks` – Feladatok listázása

**Query paraméter (opcionális):** `?status=TODO|IN_PROGRESS|DONE`

**Válasz (200 OK):**
```json
[
  {
    "id": 1,
    "title": "Dokumentáció írása",
    "description": "README elkészítése",
    "status": "IN_PROGRESS",
    "createdAt": "2025-01-15T10:30:00",
    "deadline": "2025-01-20T23:59:00",
    "username": "kovacs_janos"
  }
]
```

---

#### `GET /api/tasks/{id}` – Feladat részletei

**Válasz (200 OK):** Egyetlen TaskResponse objektum  
**Hibák:** `404 Not Found`, `403 Forbidden`

---

#### `POST /api/tasks` – Új feladat létrehozása

**Kérés törzse:**
```json
{
  "title": "Tesztek írása",
  "description": "Unit és integrációs tesztek",
  "status": "TODO",
  "deadline": "2025-02-01T18:00:00"
}
```

**Válasz (201 Created):** TaskResponse objektum

---

#### `PUT /api/tasks/{id}` – Feladat módosítása

Ugyanolyan törzs mint a POST. **Válasz (200 OK):** frissített TaskResponse

---

#### `PATCH /api/tasks/{id}/status` – Státusz módosítása

**Kérés törzse:**
```json
{ "status": "DONE" }
```

**Válasz (200 OK):** frissített TaskResponse

---

#### `DELETE /api/tasks/{id}` – Feladat törlése

**Válasz (200 OK):**
```json
{ "message": "Feladat törölve" }
```

---

## Tesztek futtatása

```bash
# Összes teszt futtatása
mvn test

# A tesztek az application-test.properties alapján H2 in-memory adatbázist használnak.
```

---

## CI/CD

A `.github/workflows/ci-cd.yml` pipeline:

1. **Minden push-ra / PR-re:** Maven build + tesztek futtatása
2. **`main` ágra push esetén:** Docker image build és push Docker Hub-ra

**Szükséges GitHub Secrets:**
- `DOCKER_USERNAME` – Docker Hub felhasználónév
- `DOCKER_PASSWORD` – Docker Hub jelszó vagy access token

---

## Adatbázis séma

```sql
-- users tábla
id       BIGSERIAL PRIMARY KEY
username VARCHAR(50)  UNIQUE NOT NULL
email    VARCHAR(100) UNIQUE NOT NULL
password VARCHAR(255) NOT NULL          -- BCrypt hash
role     VARCHAR(20)  DEFAULT 'USER'

-- tasks tábla
id          BIGSERIAL PRIMARY KEY
title       VARCHAR(200) NOT NULL
description TEXT
status      VARCHAR(20)  DEFAULT 'TODO'  -- TODO | IN_PROGRESS | DONE
created_at  TIMESTAMP    DEFAULT NOW()
deadline    TIMESTAMP
user_id     BIGINT REFERENCES users(id) ON DELETE CASCADE
```
