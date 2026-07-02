# Sistema de Transferencias Bancarias (Spring Boot + MySQL)

Este es un proyecto premium de banca móvil con un flujo de transferencia en 3 pantallas, selector de clientes activos e historial de transacciones dinámico, migrado a **Java + Spring Boot**.

---

## 🚀 Creación Automatizada de Base de Datos y Tablas

**La base de datos y la estructura de tablas se inicializan automáticamente al arrancar la aplicación.**

Cuando ejecutas el servidor por primera vez, Spring Boot realiza las siguientes tareas de forma autónoma:
1. **Creación de la BD**: La URL de la base de datos incluye `createDatabaseIfNotExist=true`, lo que le indica a MySQL que cree la base de datos `practica_bd` en el puerto `3307` si no existe.
2. **Generación de Tablas**: Gracias a Hibernate y Spring Data JPA (`spring.jpa.hibernate.ddl-auto=update`), las tablas `clientes` y `transferencias` (con sus relaciones de clave foránea) se crean automáticamente según las clases de entidad en Java.
3. **Siembra de Clientes (Seeding)**: En el primer arranque, el componente `DatabaseSeeder` detecta si la tabla de clientes está vacía e inserta automáticamente a los 4 clientes de prueba con sus respectivos saldos y números de cuenta.

---

## 🛠️ Requisitos de Ejecución

- **Java**: JDK 21 instalado.
- **MySQL**: Servidor activo en el puerto `3307` (por ejemplo, con XAMPP) con usuario `root` y sin contraseña.

---

## 🏃 Cómo Compilar y Ejecutar la Aplicación

El proyecto incluye el Maven Wrapper (`mvnw`), por lo que no es necesario instalar Maven de forma global.

### 1. Compilar y empaquetar el proyecto:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\mvnw clean package -DskipTests
```

### 2. Iniciar la aplicación Spring Boot:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\mvnw spring-boot:run
```

Una vez que se inicie correctamente (verás la salida en consola indicando que corre en el puerto `5000`), abre tu navegador web e ingresa a:
👉 **[http://localhost:5000](http://localhost:5000)**

---

## 👥 Clientes de Prueba Creados Automáticamente

Al iniciar, se sembrarán las siguientes cuentas en la base de datos:
- **Carlos Alberto Mendoza** (Cuenta: `009-123456-78`, Saldo Inicial: S/ 120.00)
- **Ana María Rodríguez** (Cuenta: `009-987654-32`, Saldo Inicial: S/ 80.00)
- **Luis Fernando Torres** (Cuenta: `009-555444-33`, Saldo Inicial: S/ 250.00)
- **Gabriela Sofía Ortiz** (Cuenta: `009-999999-99`, Saldo Inicial: S/ 80.00)
