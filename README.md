# 🔐 MFA Authentication System

Sistema de autenticación con **MFA (Multi-Factor Authentication)**, **OAuth2** (Google, GitHub, Facebook) y **RBAC** (Role-Based Access Control).

## 🚀 Stack Tecnológico

- **Java**: 21
- **Spring Boot**: 3.2.0
- **Base de Datos**: PostgreSQL
- **Seguridad**: Spring Security + JWT + OAuth2
- **Frontend**: HTML5 + JavaScript (Vanilla)

## 📋 Características

✅ Autenticación con **MFA/2FA** (Google Authenticator)  
✅ Login con **OAuth2**: Google, GitHub, Facebook  
✅ Sistema **RBAC** completo (Roles, Permisos, Recursos)  
✅ **JWT** para sesiones  
✅ Panel de administración RBAC  
✅ Dashboard de usuario

---

## 📦 Requisitos Previos

1. **Java 21** - [Descargar](https://www.oracle.com/java/technologies/downloads/#java21)
2. **PostgreSQL** - [Descargar](https://www.postgresql.org/download/)
3. **Maven** - [Descargar](https://maven.apache.org/download.cgi) (o usar el wrapper incluido)

---

## ⚙️ Instalación y Configuración

### 1. Clonar el repositorio

```bash
git clone <tu-repo>
cd mfa-autenticate
```

### 2. Crear la base de datos

```sql
-- En PostgreSQL
CREATE DATABASE mfa_auth_db;
```

### 3. Configurar application.yml

Edita `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mfa_auth_db
    username: postgres        # ← Tu usuario de PostgreSQL
    password: admin           # ← Tu contraseña de PostgreSQL

app:
  jwt:
    secret: tu-clave-secreta-minimo-32-caracteres  # ← Cámbiala
```

**⚠️ IMPORTANTE**: Si quieres usar OAuth2, configura tus propias credenciales de:
- [Google Cloud Console](https://console.cloud.google.com)
- [GitHub OAuth Apps](https://github.com/settings/developers)
- [Facebook Developers](https://developers.facebook.com)

### 4. Compilar y ejecutar

**Windows:**
```bash
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

**Linux/Mac:**
```bash
./mvnw clean install
./mvnw spring-boot:run
```

### 5. Acceder a la aplicación

🌐 **Frontend**: [http://localhost:8080](http://localhost:8080)

---

## 📁 Estructura del Proyecto

```
mfa-autenticate/
├── src/main/java/com/security/mfaautenticate/
│   ├── config/          # Configuración de Spring Security
│   ├── controller/      # Endpoints REST
│   ├── entity/          # Entidades JPA (User, Role, Permission, Resource)
│   ├── repository/      # Repositorios JPA
│   ├── security/        # JWT, OAuth2, filtros
│   └── service/         # Lógica de negocio
├── src/main/resources/
│   ├── static/          # Frontend (HTML/JS)
│   │   ├── login.html
│   │   ├── dashboard.html
│   │   └── rbac-admin.html
│   └── application.yml  # Configuración
└── pom.xml              # Dependencias Maven
```

---

## 🔑 Endpoints Principales

### Autenticación

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/oauth2/authorization/google` | Login con Google |
| `GET` | `/oauth2/authorization/github` | Login con GitHub |
| `GET` | `/oauth2/authorization/facebook` | Login con Facebook |
| `GET` | `/api/auth/profile` | Perfil del usuario (requiere JWT) |

### MFA

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/auth/mfa/setup` | Generar QR para MFA |
| `POST` | `/api/auth/mfa/verify` | Verificar código MFA |
| `POST` | `/api/auth/mfa/disable` | Desactivar MFA |

### RBAC (requiere rol ADMIN)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET/POST` | `/api/rbac/roles` | Gestión de roles |
| `GET/POST` | `/api/rbac/resources` | Gestión de recursos |
| `GET/POST` | `/api/rbac/permissions` | Gestión de permisos |
| `GET/POST` | `/api/rbac/users` | Gestión de usuarios |

---

## 👥 Usuarios por Defecto

Al iniciar la aplicación, se crean automáticamente:

- **Roles**: `ADMIN`, `USER`
- Los usuarios OAuth2 reciben el rol `USER` por defecto

---

## 🎯 Flujo de Uso

### 1. **Primer Login**
1. Ve a [http://localhost:8080](http://localhost:8080)
2. Haz clic en "Login con Google/GitHub/Facebook"
3. Autoriza la aplicación
4. Serás redirigido al dashboard

### 2. **Activar MFA (Opcional)**
1. En el dashboard, haz clic en "Configurar MFA"
2. Escanea el QR con Google Authenticator
3. Ingresa el código de 6 dígitos
4. ¡MFA activado!

### 3. **Panel RBAC (Solo Administradores)**
1. Asigna rol `ADMIN` a tu usuario (directamente en la BD la primera vez)
2. Accede a "Administración" en el dashboard
3. Crea recursos (ej: "Productos")
4. Crea permisos (ej: "Productos:CREATE")
5. Asigna permisos a roles
6. Asigna roles a usuarios

---

## 🔧 Dependencias Principales

```xml
<!-- Spring Boot -->
<spring-boot.version>3.2.0</spring-boot.version>

<!-- Seguridad -->
<dependency>spring-boot-starter-security</dependency>
<dependency>spring-boot-starter-oauth2-client</dependency>
<dependency>io.jsonwebtoken:jjwt-api:0.12.3</dependency>

<!-- MFA -->
<dependency>com.warrenstrange:googleauth:1.5.0</dependency>
<dependency>com.google.zxing:core:3.5.2</dependency>

<!-- Base de Datos -->
<dependency>spring-boot-starter-data-jpa</dependency>
<dependency>org.postgresql:postgresql</dependency>
```

---

## 🐛 Solución de Problemas

### Error: "Cannot connect to PostgreSQL"
✅ Verifica que PostgreSQL esté corriendo  
✅ Confirma usuario/contraseña en `application.yml`  
✅ Asegúrate de que la BD `mfa_auth_db` existe

### Error: "StackOverflowError"
✅ Ya está solucionado - Las entidades usan `@JsonManagedReference` y `@JsonBackReference`

### No puedo acceder al panel RBAC
✅ Asigna rol `ADMIN` manualmente en la BD:
```sql
UPDATE user_roles SET role_id = (SELECT id FROM roles WHERE name = 'ADMIN') 
WHERE user_id = <tu-user-id>;
```

---

## 📚 Documentación Adicional

- [Guía de RBAC](GUIA_RBAC.md) - Explicación completa del modelo de permisos

---

## 🛡️ Seguridad

⚠️ **Para producción:**

1. Cambia el secreto JWT en `application.yml`
2. Usa variables de entorno para credenciales OAuth2
3. Habilita HTTPS
4. Configura CORS apropiadamente
5. Cambia las credenciales de la base de datos

---

## 📝 Licencia

Este proyecto es de código abierto para fines educativos.

---

## 🤝 Contribuir

1. Fork el proyecto
2. Crea una rama (`git checkout -b feature/nueva-funcionalidad`)
3. Commit tus cambios (`git commit -m 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Abre un Pull Request

---

## 📧 Soporte

¿Problemas? Abre un [issue](../../issues) en GitHub.

---

**Desarrollado con ❤️ usando Spring Boot y Java 21**

