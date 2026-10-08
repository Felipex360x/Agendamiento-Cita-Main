# Nicolet Estudio - Sistema de Agendamiento de Citas

Sistema integral de gestión y agendamiento de citas para **Nicolet Estudio**, desarrollado con arquitectura cliente-servidor basada en **Spring Boot**, **MySQL** y **Frontend Web**.

---

##  Estructura del Proyecto

```text
Agendamiento-Cita-Main/
├── src/                          # Backend: Código fuente Java Spring Boot
│   ├── main/java/cl/nicolet/backend/
│   │   ├── config/              # Configuraciones (Swagger / OpenAPI)
│   │   ├── controller/          # Controladores REST API
│   │   ├── dto/                 # Objetos de Transferencia de Datos
│   │   ├── exception/           # Manejador global de excepciones
│   │   ├── model/               # Entidades JPA (Usuario, etc.)
│   │   ├── repository/          # Repositorios Spring Data JPA
│   │   ├── service/             # Lógica de negocio
│   │   └── BackendApplication.java
│   └── main/resources/
│       ├── application.properties      # Configuración de base de datos MySQL
│       └── application-dev.properties  # Perfil de desarrollo alternativo
├── db/                           # Base de Datos
│   ├── init.sql                 # Script de creación de tablas y datos semilla
│   └── README.md                # Documentación de la base de datos
├── front/                        # Frontend Web (HTML / CSS / JS)
│   ├── index.html               # Pantalla web principal
│   ├── css/
│   │   └── index.css            # Estilos visuales
│   ├── js/
│   │   └── index.js             # Lógica e integración REST
│   └── README.md                # Guía de uso del frontend
├── docker-compose.yml            # Orquestación de MySQL 8.0 y phpMyAdmin
├── Dockerfile                    # Empaquetado del microservicio Backend
├── start.ps1                     # Script de inicio seguro en PowerShell (libera puerto 8080)
├── start.cmd                     # Script de inicio seguro en CMD (libera puerto 8080)
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

Existen dos alternativas para iniciar el servidor:

#### Opcion A (Recomendada): Inicio Seguro con Auto-liberacion de Puerto
Los scripts de inicio seguro revisan si el puerto 8080 esta ocupado por un proceso previo de Java, lo liberan y arrancan la aplicacion:

- En Windows PowerShell:
  ```powershell
  .\start.ps1
  ```

- En Windows CMD:
  ```cmd
  start.cmd
  ```

#### Opcion B: Inicio Estandar con Maven Wrapper
No necesitas tener Maven instalado; el proyecto incluye su propio envoltorio ejecutable (mvnw):

- En Windows (PowerShell o CMD):
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```

- En Linux o macOS:
  ```bash
  ./mvnw spring-boot:run
  ```

El servidor iniciara en el puerto 8080.

#### Liberacion Manual del Puerto 8080
Si alguna vez un proceso anterior queda colgado reteniendo el puerto 8080, puedes liberarlo directamente en PowerShell:
```powershell
Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess -Force
```

---

### Paso 4: Abrir la Interfaz Visual (Frontend)

Ve a la carpeta [`front/`](front/) y haz doble clic en `index.html` para abrirlo en tu navegador (Chrome, Edge, Brave, Firefox).

- La interfaz detectara automaticamente el Backend encendido.
- Podras ver el listado de usuarios cargados desde MySQL con sus respectivos roles.
- Podras buscar usuarios por ID, registrarlos, editarlos y eliminarlos en tiempo real (soporte completo GET, POST, PUT, DELETE).

---

## Documentacion Interactiva de la API (Swagger UI)

Con el backend iniciado, puedes probar y consultar todos los endpoints REST directamente desde el explorador:

[http://localhost:8080/doc/swagger-ui.html](http://localhost:8080/doc/swagger-ui.html)

### Endpoints Principales:

#### Usuarios:
| Metodo | Endpoint | Descripcion |
|---|---|---|
| GET | /api/v2/nicolet/usuarios | Lista todos los usuarios registrados |
| GET | /api/v2/nicolet/usuarios/{id} | Busca un usuario por su ID unico |
| POST | /api/v2/nicolet/usuarios | Registra un nuevo usuario |
| PUT | /api/v2/nicolet/usuarios/{id} | Actualiza datos de un usuario existente |
| DELETE | /api/v2/nicolet/usuarios/{id} | Elimina un usuario por su ID |
| POST | /api/v2/nicolet/usuarios/manual | Validacion manual de existencia de correo |

#### Tipos de Usuario (Roles):
| Metodo | Endpoint | Descripcion |
|---|---|---|
| GET | /api/v2/nicolet/tipos-usuario | Lista todos los tipos de usuario |
| GET | /api/v2/nicolet/tipos-usuario/{id} | Busca un tipo de usuario por su ID unico |
| POST | /api/v2/nicolet/tipos-usuario | Registra un nuevo tipo de usuario |
| PUT | /api/v2/nicolet/tipos-usuario/{id} | Actualiza un tipo de usuario existente |
| DELETE | /api/v2/nicolet/tipos-usuario/{id} | Elimina un tipo de usuario por su ID |

---

## Ejecucion de Pruebas Automatizadas

El proyecto cuenta con una suite completa de pruebas unitarias y de integracion (JPA, Servicios, Modelos y Contexto Spring Boot):

```powershell
.\mvnw.cmd test
```

Resultado esperado:
```text
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## Flujo de Trabajo con Git y GitHub (Ramas, Push y Pull)

Para mantener el orden y la estabilidad del proyecto en las ramas `main` y `Test`, utiliza la siguiente guía de comandos:

### 1. Sincronizar tu repositorio local (Git Pull)
Antes de crear una nueva rama o comenzar a trabajar, asegúrate de tener siempre la versión más reciente del repositorio remoto:

```bash
# 1. Cambiar a la rama base (main o Test)
git checkout main

# 2. Descargar y combinar los últimos cambios de GitHub
git pull origin main
```

---

### 2. Crear y cambiarte a una nueva rama
Cada nueva funcionalidad o arreglo debe desarrollarse en su propia rama aislada (ej: `feature/nombre-tarea` o `fix/descripcion`):

```bash
# Crear la rama y posicionarte en ella automáticamente:
git checkout -b feature/nueva-funcionalidad
```

Para ver la lista de todas las ramas locales y saber en cuál te encuentras:
```bash
git branch
```

---

### 3. Guardar cambios y subirlos a GitHub (Git Push)
Cuando termines de realizar y probar tus cambios en el código:

```bash
# 1. Verificar los archivos modificados
git status

# 2. Preparar todos los cambios para el commit
git add .

# 3. Guardar los cambios con un mensaje descriptivo
git commit -m "feat: descripción de los cambios realizados"

# 4. Subir la rama a GitHub por primera vez (configura el rastreo remoto)
git push -u origin feature/nueva-funcionalidad
```

> **Tip**: En los commits posteriores dentro de esa misma rama, solo necesitarás escribir:
> ```bash
> git push
> ```

---

### 4. Integrar los cambios a `main` (Pull Request)
1. Ingresa al repositorio en GitHub: [https://github.com/Felipex360x/Agendamiento-Cita-Main](https://github.com/Felipex360x/Agendamiento-Cita-Main).
2. Verás un aviso con el botón verde **Compare & pull request**. Haz clic en él.
3. Revisa los cambios y presiona **Create pull request**.
4. Haz clic en **Merge pull request** y confirma la fusión.

---

### 5. Limpieza y eliminación de ramas terminadas
Una vez que tu rama fue aprobada y fusionada en `main`, elimínala para mantener el repositorio limpio:

```bash
# 1. Vuelve a la rama principal y actualízala
git checkout main
git pull origin main

# 2. Eliminar la rama en tu equipo local
git branch -d feature/nueva-funcionalidad

# 3. Eliminar la rama en GitHub remoto
git push origin --delete feature/nueva-funcionalidad
```
