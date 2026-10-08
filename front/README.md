# Frontend - Nicolet Estudio

Interfaz web visual interactiva para la administración y visualización de usuarios del sistema de agendamiento de citas Nicolet Estudio.

---

##  Estructura del Frontend

```text
front/
├── css/
│   └── index.css       # Estilos específicos de la interfaz
├── js/
│   └── index.js        # Lógica e integración con la API REST
├── index.html          # Pantalla visual de gestión de usuarios
└── README.md           # Guía de uso
```

---

##  Cómo visualizar la interfaz

Puedes abrir la interfaz de cualquiera de las siguientes formas:

### Opción 1: Directo desde el navegador (Más rápido)
Abre directamente [`index.html`](index.html) haciendo doble clic sobre él en tu explorador de archivos.

### Opción 2: Con Live Server (VS Code)
1. Instala la extensión **Live Server** en Visual Studio Code.
2. Haz clic derecho sobre [`index.html`](index.html) y selecciona **Open with Live Server**.

### Opción 3: Con Node.js
```bash
npx serve front
```

---

##  Requisitos
Para interactuar con la base de datos y crear/eliminar registros, el servicio **`backend`** debe estar iniciado en el puerto `8080`:
```powershell
.\mvnw.cmd spring-boot:run
```
*(Si el backend no está encendido, el indicador superior mostrará "Backend Desconectado").*

---

## Características
- **Monitor en vivo del estado del Backend** (indicador verde/rojo).
- **Contador en tiempo real** de usuarios registrados en MySQL.
- **Formulario de registro** con validaciones para agregar nuevos usuarios a la base de datos física.
- **Tabla interactiva** para visualizar y eliminar usuarios.
- **Notificaciones Toast** visuales de éxito y error.
