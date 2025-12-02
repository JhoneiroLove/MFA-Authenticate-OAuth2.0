# 🏗️ Arquitectura del Sistema

## Diagrama de Capas

```
┌─────────────────────────────────────────┐
│          FRONTEND (HTML/JS)             │
│  login.html | dashboard.html | rbac     │
└──────────────────┬──────────────────────┘
                   │ HTTP/REST
┌──────────────────▼──────────────────────┐
│         CONTROLLERS (Spring)            │
│  AuthController | RbacController        │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│      SECURITY LAYER (Spring Security)   │
│  JwtFilter | OAuth2 | MFA               │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│         SERVICES (Lógica de Negocio)    │
│  MfaService | CustomOAuth2UserService   │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│      REPOSITORIES (Spring Data JPA)     │
│  UserRepo | RoleRepo | PermissionRepo   │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│         DATABASE (PostgreSQL)           │
│  users | roles | permissions | etc.     │
└─────────────────────────────────────────┘
```

---

## Flujo de Autenticación OAuth2

```
1. Usuario hace clic en "Login con Google"
   ↓
2. Redirige a Google OAuth2 consent screen
   ↓
3. Usuario autoriza → Google retorna código
   ↓
4. Spring Security intercambia código por token
   ↓
5. CustomOAuth2UserService crea/actualiza usuario
   ↓
6. OAuth2AuthenticationSuccessHandler genera JWT
   ↓
7. Redirige a /oauth2/redirect?token=<jwt>
   ↓
8. Frontend guarda JWT en localStorage
   ↓
9. Dashboard carga con JWT en headers
```

---

## Flujo de MFA

```
1. Usuario en dashboard → clic "Configurar MFA"
   ↓
2. POST /api/auth/mfa/setup
   ↓
3. MfaService genera secreto + QR
   ↓
4. Usuario escanea QR con Google Authenticator
   ↓
5. Ingresa código de 6 dígitos
   ↓
6. POST /api/auth/mfa/verify
   ↓
7. MfaService valida código TOTP
   ↓
8. Si válido → mfaEnabled=true en BD
```

---

## Modelo de Datos RBAC

```sql
-- Jerarquía
USER ──> USER_ROLES ──> ROLE ──> ROLE_PERMISSIONS ──> PERMISSION ──> RESOURCE

-- Ejemplo
User: juan@example.com
  ↓ tiene
Role: VENDEDOR
  ↓ tiene
Permission: Productos:CREATE, Productos:READ
  ↓ sobre
Resource: Productos
```

---

## Tecnologías Clave

| Capa | Tecnología | Propósito |
|------|------------|-----------|
| **Backend** | Spring Boot 3.2 | Framework principal |
| **Seguridad** | Spring Security | Autenticación y autorización |
| **OAuth2** | Spring OAuth2 Client | Login social |
| **JWT** | jjwt 0.12.3 | Tokens de sesión |
| **MFA** | Google Authenticator | 2FA/TOTP |
| **ORM** | Spring Data JPA | Mapeo objeto-relacional |
| **BD** | PostgreSQL | Persistencia |
| **Frontend** | HTML5 + Vanilla JS | UI sin frameworks |

---

## Endpoints REST

### Públicos (sin auth)
- `GET /` → login.html
- `GET /oauth2/authorization/{provider}` → OAuth2 redirect

### Autenticados (JWT required)
- `GET /api/auth/profile` → Perfil usuario
- `POST /api/auth/mfa/setup` → Configurar MFA
- `POST /api/auth/mfa/verify` → Verificar MFA
- `POST /api/auth/mfa/disable` → Desactivar MFA

### Solo ADMIN (JWT + role ADMIN)
- `GET/POST /api/rbac/roles`
- `GET/POST /api/rbac/resources`
- `GET/POST /api/rbac/permissions`
- `GET/POST /api/rbac/users`

---

## Seguridad Implementada

✅ **CSRF Protection** (deshabilitado para REST API)  
✅ **CORS** configurado para localhost:8080  
✅ **JWT** con firma HMAC-SHA256  
✅ **OAuth2** state parameter anti-CSRF  
✅ **MFA** basado en TOTP (RFC 6238)  
✅ **Password** NO almacenada (solo OAuth2)  
✅ **HTTPS** recomendado para producción  

---

## Base de Datos - Esquema Principal

```sql
-- Usuarios
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  name VARCHAR(255),
  provider VARCHAR(50),
  provider_id VARCHAR(255),
  mfa_secret VARCHAR(255),
  mfa_enabled BOOLEAN DEFAULT FALSE
);

-- Roles
CREATE TABLE roles (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) UNIQUE NOT NULL,
  description TEXT
);

-- Relación User-Role (Many-to-Many)
CREATE TABLE user_roles (
  user_id BIGINT REFERENCES users(id),
  role_id BIGINT REFERENCES roles(id),
  PRIMARY KEY (user_id, role_id)
);

-- Recursos (entidades de negocio)
CREATE TABLE resources (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) UNIQUE NOT NULL,
  description TEXT,
  path VARCHAR(255) NOT NULL
);

-- Permisos (Resource + Operation)
CREATE TABLE permissions (
  id BIGSERIAL PRIMARY KEY,
  resource_id BIGINT REFERENCES resources(id),
  operation VARCHAR(50) NOT NULL -- CREATE, READ, UPDATE, DELETE
);

-- Relación Role-Permission (Many-to-Many)
CREATE TABLE role_permissions (
  role_id BIGINT REFERENCES roles(id),
  permission_id BIGINT REFERENCES permissions(id),
  PRIMARY KEY (role_id, permission_id)
);
```

---

## Configuración Spring Security

```java
// SecurityConfig.java

@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    return http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfig()))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/login.html", "/oauth2/**").permitAll()
            .requestMatchers("/api/rbac/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth -> oauth
            .successHandler(oAuth2SuccessHandler)
        )
        .addFilterBefore(jwtFilter, OAuth2LoginAuthenticationFilter.class)
        .build();
}
```

---

## Variables de Entorno

Ver `.env.example` para lista completa. Principales:

```env
# Base de Datos
DB_URL=jdbc:postgresql://localhost:5432/mfa_auth_db
DB_USERNAME=postgres
DB_PASSWORD=admin

# JWT
JWT_SECRET=your-secret-min-32-chars
JWT_EXPIRATION=86400000

# OAuth2 (Google, GitHub, Facebook)
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
```

---

## Compilación y Empaquetado

```bash
# Desarrollo
mvnw spring-boot:run

# Producción
mvnw clean package
java -jar target/mfa-autenticate-0.0.1-SNAPSHOT.jar

# Docker (si tienes Dockerfile)
docker build -t mfa-auth .
docker run -p 8080:8080 mfa-auth
```

---

## Testing

```bash
# Tests unitarios
mvnw test

# Tests de integración
mvnw verify
```

---

**Para más detalles, ver:**
- [README.md](README.md) - Guía de usuario
- [GUIA_RBAC.md](GUIA_RBAC.md) - Sistema de permisos
- [QUICKSTART.md](QUICKSTART.md) - Inicio rápido

