# Nicolet Estudio - Sistema de Agendamiento de Citas

Sistema integral de gestión y agendamiento de citas para **Nicolet Estudio**, desarrollado con arquitectura cliente-servidor basada en **Spring Boot**, **MySQL** y **Frontend Web**.

---

##  Estructura del Proyecto

```text
Agendamiento-Cita-Main/
├── src/                          # Backend: Código fuente Java Spring Boot
│   ├── main/java/cl/nicolet/backend/
│   │   ├── Config/              # Configuraciones (Swagger / OpenAPI)
│   │   ├── Controller/          # Controladores REST API
│   │   ├── DTO/                 # Objetos de Transferencia de Datos
│   │   ├── Exception/           # Manejador global de excepciones
│   │   ├── Model/               # Entidades JPA (Usuario, etc.)
│   │   ├── Repository/          # Repositorios Spring Data JPA
│   │   ├── Service/             # Lógica de negocio
│   │   └── BackendApplication.java
│   └── main/resources/
│       ├── application.properties      # Configuración de base de datos MySQL
│       └── application-dev.properties  # Perfil de desarrollo alternativo
├── db/                           # Base de Datos
│   ├── init.sql                 # Script de creación de tablas y datos semilla
│   └── README.md                # Documentación de la base de datos
├── front/                        # Frontend (Interfaz Visual Organizada)
│   ├── pantallas/
│   │   └── index.html           # Pantalla web principal
│   ├── css/
│   │   └── index.css            # Estilos visuales de index.html
│   ├── js/
│   │   └── index.js             # Lógica e integración REST de index.html
│   └── README.md                # Guía de uso del frontend
├── docker-compose.yml            # Orquestación de MySQL 8.0 y phpMyAdmin
├── Dockerfile                    # Empaquetado del microservicio Backend
├── mvnw / mvnw.cmd               # Wrapper de Maven (no requiere instalar Maven)
├── pom.xml                       # Dependencias y build del proyecto
└── README.md                     # Guía principal de instalación y ejecución
```

---

## Requisitos Previos y Dependencias a Instalar

Para ejecutar este proyecto en cualquier computadora, se necesitan los siguientes componentes:

### 1. Java Development Kit (JDK 17)
- **Requerido**: Java 17 o superior (Eclipse Temurin / OpenJDK).
- **Instalación rápida en Windows (PowerShell)**:
  ```powershell
  winget install --id EclipseAdoptium.Temurin.17.JDK -e --accept-source-agreements --accept-package-agreements
  ```
- **Descarga manual**: [https://adoptium.net/temurin/releases/?version=17](https://adoptium.net/temurin/releases/?version=17)
- **Verificación**:
  ```bash
  java -version
  ```

---

### 2. Motor de Base de Datos (MySQL o Docker)
Elige **entre la a o b ** de las siguientes dos alternativas:

#### Opción A: Usando Docker Desktop (Recomendada)
- Instala Docker Desktop si no lo tienes: [https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/)

#### Opción B: MySQL Server o MariaDB Local (Nativo)
- **Instalación rápida en Windows (PowerShell)**:
  ```powershell
  winget install --id MariaDB.Server -e --accept-source-agreements --accept-package-agreements
  ```
- **Descarga MySQL**: [https://dev.mysql.com/downloads/installer/](https://dev.mysql.com/downloads/installer/)

---

##  Guía Paso a Paso para Ejecutar el Proyecto

### Paso 1: Clonar o Descargar el Repositorio
```bash
git clone <URL de nuestro repositorio >
cd Agendamiento-Cita-Main
```

---

### Paso 2: Iniciar la Base de Datos

#### Si usas Docker:
Desde la raíz del proyecto, ejecuta:
```bash
docker compose up -d
```
Esto creará automáticamente:
- El contenedor **`mysql_usuario`** en el puerto `3306`.
- La base de datos `universidad_backend` con las tablas y datos iniciales de [`db/init.sql`](db/init.sql).
- Un panel visual **phpMyAdmin** en [http://localhost:8081](http://localhost:8081).

#### Si usas MySQL / MariaDB local instalado en Windows:
Asegúrate de que el servicio esté iniciado e importa el script inicial:
```powershell
# En Windows PowerShell con MariaDB/MySQL:
Get-Content "db\init.sql" | mysql -u root
```

#### Credenciales Configuradas:
| Parámetro | Valor |
|---|---|
| **Host** | `localhost` |
| **Puerto** | `3306` |
| **Base de Datos** | `universidad_backend` |
| **Usuario** | `desarrollador` |
| **Contraseña** | `password_seguro_123` |

---

### Paso 3: Iniciar el Backend

No necesitas tener Maven instalado; el proyecto incluye su propio envoltorio ejecutable (`mvnw`).

- **En Windows (PowerShell o CMD)**:
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```

- **En Linux o macOS**:
  ```bash
  ./mvnw spring-boot:run
  ```

El servidor iniciará en el puerto **`8080`**.

---

### Paso 4: Abrir la Interfaz Visual (Frontend)

Ve a la carpeta [`front/pantallas/`](front/pantallas/) y haz **doble clic en `index.html`** para abrirlo en tu navegador favorito (Chrome, Edge, Brave, Firefox).

- La interfaz detectará automáticamente el Backend encendido (indicador verde).
- Podrás ver el listado de usuarios cargados desde MySQL.
- Podrás registrar nuevos usuarios y eliminarlos en tiempo real.

---

## Documentación Interactiva de la API (Swagger UI)

Con el backend iniciado, puedes probar y consultar todos los endpoints REST directamente desde el explorador:

 **[http://localhost:8080/doc/swagger-ui.html](http://localhost:8080/doc/swagger-ui.html)**

### Endpoints Principales:

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/v2/nicolet/usuarios` | Lista todos los usuarios registrados |
| `GET` | `/api/v2/nicolet/usuarios/{id}` | Busca un usuario por su ID único |
| `POST` | `/api/v2/nicolet/usuarios` | Registra un nuevo usuario |
| `PUT` | `/api/v2/nicolet/usuarios/{id}` | Actualiza datos de un usuario existente |
| `DELETE` | `/api/v2/nicolet/usuarios/{id}` | Elimina un usuario por su ID |
| `POST` | `/api/v2/nicolet/usuarios/manual` | Validación manual de existencia de correo |

---

## Ejecución de Pruebas Automatizadas

El proyecto cuenta con una suite completa de pruebas unitarias y de integración (JPA, Servicios, Modelos y Contexto Spring Boot):

```powershell
.\mvnw.cmd test
```

Resultado esperado:
```text
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
