# BikeRent Backend

REST API backend for the Bike Rental Service.

## 🚀 Quick Start (Docker)

### Prerequisites

- Docker & Docker Compose

### Запуск

1. **Скопируй и настрой `.env`:**
   ```bash
   cp .env.example .env
   
   # Windows PowerShell (alternative):
   # Copy-Item .env.example .env
   #
   # Windows helper to generate .env + JWT_SECRET:
   # powershell -ExecutionPolicy Bypass -File .\\init-scripts\\bootstrap-env.ps1
   ```

2. **Сгенерируй JWT секрет:**
   ```bash
   # Linux/Mac
    openssl rand -base64 64
    
    # Windows PowerShell (alternative):
    # [Convert]::ToBase64String((1..64 | ForEach-Object { [byte](Get-Random -Maximum 256) }))
   
   # Или любой base64 строкой минимум 256 бит
   ```
   
   Вставь в `.env`:
   ```
   JWT_SECRET=<твой_сгенерированный_секрет>
   ```

3. **Запусти всё:**
   ```bash
   docker-compose up -d --build
   ```

4. **Готово!**
   - API: http://localhost:8080/api/v1/
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - pgAdmin: http://localhost:5050 (admin@bikerent.local / admin)

### Полезные команды

```bash
# Посмотреть логи
docker-compose logs -f app

# Перезапустить после изменений
docker-compose up -d --build app

# Остановить всё
docker-compose down

# Остановить и удалить данные БД
docker-compose down -v
```

---

## 🛠 Локальная разработка (для дебага)

Если нужно дебажить в IDE:

1. **Запусти только БД:**
   ```bash
   docker-compose up -d postgres pgadmin
   ```

2. **Создай `application-local.properties`:**
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/bikerent
   spring.datasource.username=postgres
   spring.datasource.password=root
   jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci1sb2NhbC1kZXZlbG9wbWVudC1vbmx5
   jwt.access-expiration=900000
   jwt.refresh-expiration=604800000
   ```

3. **Запусти из IDE** с профилем `local`:
   ```
   -Dspring.profiles.active=local
   ```

---

## 🔐 Security Configuration

### JWT Configuration

The application uses JWT tokens for authentication with access/refresh token flow:

| Token Type | Expiration | Purpose |
|------------|------------|---------|
| Access Token | 15 minutes | API authentication |
| Refresh Token | 7 days | Obtain new access tokens |

**⚠️ IMPORTANT:** Never commit secrets to version control!

Set these environment variables:
```bash
JWT_SECRET=<your-base64-encoded-256-bit-secret>
JWT_ACCESS_EXPIRATION=900000      # 15 minutes
JWT_REFRESH_EXPIRATION=604800000  # 7 days
```

### Roles

| Role | Access |
|------|--------|
| USER | Rent bicycles, manage payments |
| TECH | + Manage repairs |
| ADMIN | + Full system access |

### Rate Limiting

| Endpoint | Limit |
|----------|-------|
| `/auth/login`, `/auth/register` | 10/min |
| `/payments/**` | 30/min |
| General API | 100/min |

## 📦 Project Structure

```
backend/
├── src/main/java/com/company/bikerent/
│   ├── auth/           # Authentication & JWT
│   ├── bicycle/        # Bicycle management
│   ├── billing/        # Payments
│   ├── common/         # Shared configs & exceptions
│   ├── geo/            # Coordinates
│   ├── maintenance/    # Repairs & Technicians
│   ├── rental/         # Rental operations
│   ├── station/        # Stations
│   └── user/           # User management
├── src/main/resources/
│   ├── db/migration/   # Flyway migrations
│   └── application.properties
├── .github/workflows/  # CI/CD
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage report
mvn test jacoco:report

# Run integration tests only
mvn verify -DskipUnitTests
```

## 📐 Code Quality

```bash
# Format code (Google Java Format)
mvn spotless:apply

# Check formatting
mvn spotless:check

# Run SpotBugs static analysis
mvn spotbugs:check

# Check dependencies for vulnerabilities
mvn org.owasp:dependency-check-maven:check
```

## 🐳 Docker

Приложение собирается автоматически при `docker-compose up --build`.

Dockerfile использует multi-stage build:
1. **Stage 1:** Maven сборка WAR
2. **Stage 2:** Tomcat с WAR файлом

```bash
# Пересобрать после изменений в коде
docker-compose up -d --build app

# Посмотреть что происходит при сборке
docker-compose build --progress=plain app
```

## 📊 Database Migrations

Migrations are managed by Flyway and run automatically on startup.

```bash
# Run migrations manually
mvn flyway:migrate

# Validate migrations
mvn flyway:validate

# View migration info
mvn flyway:info
```

## 📖 API Documentation

Interactive API documentation is available at:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api/v1/api-docs

### Authentication Flow

1. **Register/Login** → Get `access_token` + `refresh_token`
2. **Use Access Token** → Add `Authorization: Bearer <access_token>` header
3. **Refresh Token** → POST to `/auth/refresh` when access token expires
4. **Logout** → POST to `/auth/logout` to revoke refresh token

## 🔧 Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | Database URL | `jdbc:postgresql://localhost:5432/bikerent` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | - |
| `JWT_SECRET` | JWT signing key (base64) | **Required** |
| `JWT_ACCESS_EXPIRATION` | Access token TTL (ms) | `900000` |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL (ms) | `604800000` |
| `CORS_ALLOWED_ORIGINS` | CORS whitelist | `http://localhost:3000` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `default` |

## 🚢 CI/CD

GitHub Actions workflow (`.github/workflows/ci.yml`) includes:

1. **Build & Test** - Compile, unit tests, integration tests
2. **Code Quality** - Spotless, SpotBugs, Checkstyle
3. **Security** - OWASP dependency check, Gitleaks
4. **Docker** - Build Docker image

## 📝 License

MIT License - see [LICENSE](LICENSE) for details.
