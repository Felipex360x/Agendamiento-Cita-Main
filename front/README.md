# Frontend - Nicolet Estudio

Interfaz web visual interactiva para la administracion de usuarios, inicio de sesion y control del sistema de agendamiento de citas de Nicolet Estudio.

---

## Estructura del Frontend

```text
front/
├── css/
│   └── index.css       # Estilos compartidos de la interfaz
├── js/
│   ├── auth.js         # Modulo de sesion JWT, proteccion de rutas y fetch autenticado
│   ├── index.js        # Logica e integracion con la API REST de Usuarios
│   └── citas.js        # Logica e integracion con la API REST de Citas
├── login.html          # Pantalla de inicio de sesion con soporte de token JWT
├── index.html          # Pantalla de gestion de usuarios y roles
├── citas.html          # Pantalla de agendamiento y control de citas
└── README.md           # Guia de uso
```

---

## Modulo de Autenticacion y Sesion (`js/auth.js`)

El archivo `js/auth.js` centraliza las politicas de seguridad en el navegador:

1. **Almacenamiento Seguro del Token:** Guarda el JWT y los datos del usuario en `localStorage` al iniciar sesion correctamente.
2. **Proteccion de Vistas (`requireAuth`):** Valida la presencia de un token activo al cargar `citas.html` o `index.html`. Si no existe sesion, redirige inmediatamente a `login.html`.
3. **Peticiones Autenticadas (`authenticatedFetch`):** Encapsula las llamadas a la API agregando de manera automatica el encabezado `Authorization: Bearer <token>`. Si el servidor responde con codigo 401 (token expirado), limpia la sesion y redirige a `login.html`.
4. **Barra de Usuario en Navbar:** Agrega dinamicamente en la esquina superior derecha el nombre del usuario, su rol y el boton de Cerrar Sesion.

---

## Como Visualizar la Interfaz

### Opcion 1: Directo desde el Navegador (Doble Clic)
Abre directamente `login.html`, `citas.html` o `index.html` haciendo doble clic sobre el archivo en el explorador de archivos. El backend soporta peticiones desde origen local de archivos.

### Opcion 2: Con Live Server (VS Code)
1. Instala la extension Live Server en Visual Studio Code.
2. Haz clic derecho sobre `login.html` y selecciona Open with Live Server.

### Opcion 3: Con Servidor HTTP Local
```bash
npx serve front
```

---

## Cuentas de Acceso para Pruebas

En la pantalla `login.html` se encuentran disponibles las siguientes credenciales de prueba preconfiguradas:

- **Administrador Garantizado:** `admin.test@nicolet.cl` / `AdminTest123!` (Cuenta con acceso prioritario e inmune a bloqueos)
- **Administrador Estandar:** `admin@nicolet.cl` / `Admin123!`
- **Profesional / Especialista:** `camila.silva@estudio.cl` / `pro2026`
- **Cliente:** `martina.contreras@gmail.com` / `123412`

---

## Modulos de la Aplicacion

### 1. Inicio de Sesion (`login.html`)
- Formulario de credenciales con validacion de correo y contraseña.
- Deteccion y visualizacion de intentos restantes antes de bloqueo por fuerza bruta.
- Boton de acceso rapido para pruebas de desarrollo local.
- Redireccion automatica al modulo de citas tras autenticacion exitosa.

### 2. Gestion de Usuarios (`index.html`)
- Monitor en vivo del estado del Backend.
- Contador de usuarios y roles registrados.
- Formulario de registro y edicion de usuarios.
- Tabla interactiva con acciones de editar y eliminar.

### 3. Agendamiento de Citas (`citas.html`)
- Formulario con seleccion dinamica de cliente, profesional y servicio.
- Calculo automatico de duracion y tarifa.
- Modales de registro rapido para clientes y profesionales.
- Tabla de agenda con estados, detalle de citas y opciones de finalizacion o cancelacion.
