# Guía de Implementación RBAC

## Resumen de Implementación

Se ha implementado un sistema completo de **Role-Based Access Control (RBAC)** en tu aplicación MFA-Authenticate-OAuth2.0 sin afectar la funcionalidad existente.

---

## Nuevas Entidades Creadas

### 1. **Role** (Rol)
- Tabla: `roles`
- Campos:
  - `id`: Identificador único
  - `name`: Nombre del rol (único)
  - `description`: Descripción del rol
  - Relaciones: Many-to-Many con `Permission` y `User`

### 2. **Resource** (Recurso)
- Tabla: `resources`
- Campos:
  - `id`: Identificador único
  - `name`: Nombre del recurso (único)
  - `description`: Descripción del recurso
  - `path`: Ruta del recurso (ej: `/api/users`)
  - Relaciones: One-to-Many con `Permission`

### 3. **Permission** (Permiso)
- Tabla: `permissions`
- Campos:
  - `id`: Identificador único
  - `resource_id`: FK a Resource
  - `operation`: Enum (CREATE, READ, UPDATE, DELETE)
  - Relaciones: Many-to-One con `Resource`, Many-to-Many con `Role`

### 4. **Operation** (Enum)
- Valores: `CREATE`, `READ`, `UPDATE`, `DELETE`

---

## Modificaciones a Entidades Existentes

### User
Se agregó la relación Many-to-Many con `Role`:
```java
@ManyToMany(fetch = FetchType.EAGER)
@JoinTable(
    name = "user_roles",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id")
)
private Set<Role> roles = new HashSet<>();
```

---

## Nuevos Endpoints REST

### Gestión de Roles
- `POST /api/rbac/roles` - Crear rol
- `GET /api/rbac/roles` - Listar todos los roles
- `DELETE /api/rbac/roles/{roleId}` - Eliminar rol

### Gestión de Recursos
- `POST /api/rbac/resources` - Crear recurso
- `GET /api/rbac/resources` - Listar todos los recursos
- `DELETE /api/rbac/resources/{resourceId}` - Eliminar recurso

### Gestión de Permisos
- `POST /api/rbac/permissions` - Crear permiso
- `GET /api/rbac/permissions` - Listar todos los permisos
- `DELETE /api/rbac/permissions/{permissionId}` - Eliminar permiso

### Asignaciones
- `POST /api/rbac/roles/{roleId}/permissions/{permissionId}` - Asignar permiso a rol
- `DELETE /api/rbac/roles/{roleId}/permissions/{permissionId}` - Quitar permiso de rol
- `POST /api/rbac/users/{userId}/roles/{roleId}` - Asignar rol a usuario
- `DELETE /api/rbac/users/{userId}/roles/{roleId}` - Quitar rol de usuario

### Usuarios
- `GET /api/rbac/users` - Listar todos los usuarios
- `GET /api/rbac/users/{userId}` - Obtener usuario con sus roles

### Utilidades
- `GET /api/rbac/operations` - Listar operaciones disponibles (CREATE, READ, UPDATE, DELETE)

---

## Nuevas Interfaces de Usuario

### rbac-admin.html
Interfaz completa de administración RBAC con 4 pestañas:

1. **Roles**: Crear, listar y eliminar roles. Asignar permisos a roles.
2. **Recursos**: Crear, listar y eliminar recursos (rutas/módulos).
3. **Permisos**: Crear, listar y eliminar permisos (combinación de recurso + operación).
4. **Usuarios**: Listar usuarios y asignarles roles.

**Acceso**: Desde el dashboard hay un botón "Administración RBAC" que redirige a `/rbac-admin.html`

---

## Funcionalidades Especiales

### 1. Primer Usuario = ADMIN Automático
Cuando el **primer usuario** se registra en la aplicación, automáticamente se le asigna el rol **ADMIN**.

**Implementación**: En `CustomOAuth2UserService.java:80-91`

```java
// Asignar rol ADMIN al primer usuario
long totalUsers = userRepository.count();
if (totalUsers == 1) {
    Optional<Role> adminRole = roleRepository.findByName("ADMIN");
    if (adminRole.isPresent()) {
        user.getRoles().add(adminRole.get());
        user = userRepository.save(user);
        log.info("Primer usuario registrado - Rol ADMIN asignado a: {}", user.getEmail());
    }
}
```

### 2. Roles por Defecto
Al iniciar la aplicación, se crean automáticamente 2 roles:
- **ADMIN**: Administrador con acceso total
- **USER**: Usuario regular con acceso limitado

**Implementación**: `DataInitializer.java` (CommandLineRunner)

### 3. Interceptor de Permisos
Se creó un interceptor que valida permisos antes de ejecutar endpoints protegidos.

**Uso con anotaciones**:
```java
@RequirePermission(resource = "/api/users", operation = Operation.CREATE)
public ResponseEntity<?> createUser() {
    // Solo usuarios con permiso CREATE en /api/users pueden acceder
}
```

**Regla especial**: Los usuarios con rol **ADMIN** tienen acceso a todo automáticamente.

---

## Servicios Creados

### RbacService
Servicio centralizado para gestión de RBAC:
- Crear/eliminar roles, recursos y permisos
- Asignar/quitar permisos a roles
- Asignar/quitar roles a usuarios
- Verificar permisos: `hasPermission(email, resourcePath, operation)`
- Verificar si es admin: `isAdmin(email)`
- Inicializar roles por defecto

---

## Configuración de Seguridad

### SecurityConfig.java
Se actualizó para permitir acceso público a:
- `/api/rbac/**` - Endpoints de RBAC
- `/rbac-admin.html` - Interfaz de administración

**Nota**: En producción, deberías proteger `/api/rbac/**` para que solo admins puedan acceder.

### WebMvcConfig.java
Configura el `PermissionInterceptor` para validar permisos en:
- Todas las rutas `/api/**`
- Excepto `/api/auth/**` (autenticación)

---

## Flujo de Uso Completo

### 1. Primer Registro
```
1. Usuario se registra con OAuth2
2. Sistema detecta que es el primer usuario (count = 1)
3. Se le asigna automáticamente el rol ADMIN
4. Usuario accede al dashboard
```

### 2. Configurar RBAC (Como Admin)
```
1. Click en "Administración RBAC" en el dashboard
2. En pestaña "Recursos":
   - Crear recurso: "User Management" | "/api/users"
3. En pestaña "Permisos":
   - Crear permiso: Resource = "User Management", Operation = CREATE
   - Crear permiso: Resource = "User Management", Operation = READ
   - Crear permiso: Resource = "User Management", Operation = UPDATE
   - Crear permiso: Resource = "User Management", Operation = DELETE
4. En pestaña "Roles":
   - Crear rol: "MANAGER" | "Team Manager"
   - Click en "Permisos" para ese rol
   - Asignar permisos deseados (ej: READ, UPDATE)
5. En pestaña "Usuarios":
   - Seleccionar un usuario
   - Click en "Asignar Roles"
   - Asignar rol "MANAGER"
```

### 3. Proteger Endpoints
```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    @RequirePermission(resource = "/api/users", operation = Operation.CREATE)
    public ResponseEntity<?> createUser() {
        // Solo usuarios con permiso CREATE pueden acceder
    }

    @GetMapping
    @RequirePermission(resource = "/api/users", operation = Operation.READ)
    public ResponseEntity<?> getAllUsers() {
        // Solo usuarios con permiso READ pueden acceder
    }
}
```

---

## Tablas de Base de Datos Creadas

Por Hibernate (JPA) automáticamente:
1. `roles`
2. `resources`
3. `permissions`
4. `user_roles` (tabla de unión User-Role)
5. `role_permissions` (tabla de unión Role-Permission)

---

## Características Técnicas

### Sin Afectar Funcionalidad Actual
- ✅ OAuth2 sigue funcionando igual
- ✅ MFA sigue funcionando igual
- ✅ Login/Dashboard no cambiaron su comportamiento
- ✅ Solo se agregó la relación `roles` a User
- ✅ Se agregó un botón en el dashboard para acceder a RBAC

### Arquitectura Limpia
- Separación de responsabilidades
- Servicios independientes
- DTOs para transferencia de datos
- Interceptor para validación centralizada

### Tecnologías Usadas
- Spring Data JPA para persistencia
- Hibernate para generación de esquema
- Lombok para reducir boilerplate
- Anotaciones personalizadas (@RequirePermission)

---

## Próximos Pasos Recomendados

1. **Proteger la interfaz RBAC**: Solo admins deberían acceder a `/rbac-admin.html`
2. **Proteger endpoints RBAC**: Agregar `@RequirePermission` a `/api/rbac/**`
3. **Crear recursos para tus módulos**: Define los recursos reales de tu aplicación
4. **Asignar permisos a roles**: Configura permisos según tus necesidades de negocio
5. **Testing**: Probar que los permisos funcionan correctamente
6. **Auditoría**: Agregar logs de auditoría para cambios en RBAC

---

## Ejemplo de Flujo Completo

```
ESCENARIO: Sistema de gestión de empleados

1. Admin crea recursos:
   - "Employees Module" | "/api/employees"
   - "Payroll Module" | "/api/payroll"

2. Admin crea permisos:
   - Employees - CREATE, READ, UPDATE, DELETE
   - Payroll - READ

3. Admin crea roles:
   - HR_MANAGER: Employees (CREATE, READ, UPDATE), Payroll (READ)
   - HR_VIEWER: Employees (READ)

4. Admin asigna roles:
   - User A → HR_MANAGER
   - User B → HR_VIEWER

5. Proteger controladores:
   @RequirePermission(resource = "/api/employees", operation = Operation.CREATE)

6. Resultado:
   - User A puede crear/leer/actualizar empleados y ver payroll
   - User B solo puede leer empleados
   - Admin puede hacer todo
```

---

## Comandos para Ejecutar

```bash
# Compilar
./mvnw clean compile

# Ejecutar
./mvnw spring-boot:run
```

**Acceso**:
- Login: http://localhost:8080/login.html
- Dashboard: http://localhost:8080/dashboard.html
- RBAC Admin: http://localhost:8080/rbac-admin.html

---

## Soporte

Para preguntas o problemas, revisa:
1. Logs de la aplicación
2. Consola del navegador (F12)
3. Tablas de base de datos generadas
4. Código fuente en `src/main/java/com/security/mfaautenticate/`
