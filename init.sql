-- Task Manager adatbázis inicializáló script
-- Futtatás: psql -U postgres -f init.sql

-- Adatbázis és felhasználó létrehozása
CREATE DATABASE taskmanager;
CREATE USER taskuser WITH PASSWORD 'taskpassword';
GRANT ALL PRIVILEGES ON DATABASE taskmanager TO taskuser;

\connect taskmanager

-- Séma jogosultság
GRANT ALL ON SCHEMA public TO taskuser;

-- Felhasználók táblája
CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER'
);

-- Feladatok táblája
CREATE TABLE IF NOT EXISTS tasks (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'TODO',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deadline    TIMESTAMP,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

-- Index a felhasználó feladatainak gyors lekérdezéséhez
CREATE INDEX IF NOT EXISTS idx_tasks_user_id ON tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status  ON tasks(status);

-- Teszt adatok (opcionális)
-- INSERT INTO users (username, email, password, role)
-- VALUES ('admin', 'admin@example.com', '$2a$10$...bcrypt_hash...', 'ADMIN');
