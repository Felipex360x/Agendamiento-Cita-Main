# Frontend - Nicolet Estudio

Interfaz web visual interactiva para la administración y visualización de usuarios y el sistema de agendamiento de citas de Nicolet Estudio.

---

## Estructura del Frontend

```text
front/
├── css/
│   └── index.css       # Estilos específicos de la interfaz
├── js/
│   ├── index.js        # Lógica e integración con la API REST de Usuarios
│   └── citas.js        # Lógica e integración con la API REST de Citas
├── index.html          # Pantalla visual de gestión de usuarios
├── citas.html          # Pantalla visual de agendamiento y control de citas
└── README.md           # Guía de uso
```

---

## Cómo visualizar la interfaz

Puedes abrir la interfaz de cualquiera de las siguientes formas:

### Opción 1: Directo desde el navegador (Más rápido)
Abre directamente [`index.html`](index.html) o [`citas.html`](citas.html) haciendo doble clic sobre el archivo en tu explorador de archivos.

### Opción 2: Con Live Server (VS Code)
1. Instala la extensión **Live Server** en Visual Studio Code.
2. Haz clic derecho sobre [`index.html`](index.html) o [`citas.html`](citas.html) y selecciona **Open with Live Server**.

### Opción 3: Con Node.js
```bash
npx serve front
```

---

## Requisitos
Para interactuar con la base de datos y crear/eliminar registros, el servicio **`backend`** debe estar iniciado en el puerto `8080`:
```powershell
.\mvnw.cmd spring-boot:run
```
*(Si el backend no está encendido, el indicador superior mostrará "Sin conexión a API").*

---

## Módulos y Características

### 1. Gestión de Usuarios (`index.html`)
- **Monitor en vivo** del estado del Backend.
- **Contador en tiempo real** de usuarios y tipos de usuario.
- **Formulario de registro y edición** de usuarios.
- **Tabla interactiva** con acciones de editar y eliminar.

### 2. Agendamiento de Citas (`citas.html`)
- **Formulario de Agendamiento:** Selección dinámica de Cliente, Profesional y Servicio con cálculo automático de duración y precio.
- **Modales de Creación Rápida:** Botones `+ Nuevo Cliente` y `+ Nuevo Profesional` para registrar y seleccionar perfiles sin salir de la pantalla de citas.
- **Tabla de Agenda:** Muestra código de reserva, cliente, profesional, servicio, horario (inicio a fin) y estado.
- **Acciones Rápidas:** Finalizar / completar citas o cancelarlas con motivo registrado.
- **Alertas reactivas:** Notificación visual al agendar o en caso de solapamiento o error.
