# Plan de Implementación: Refactorización a Java + Spring Boot

Este plan detalla la migración completa de la aplicación de transferencias bancarias desde Python/Flask a **Java + Spring Boot** utilizando **Maven**. Se conservará la misma funcionalidad, diseño de base de datos y la interfaz estética interactiva (HTML/CSS/JS) en el frontend.

---

## User Review Required

> [!IMPORTANT]
> **Estructura del Proyecto**:
> Se transformará la estructura del directorio actual `c:\xampp\htdocs\Lab_software_u_3` en un proyecto Java estándar de Maven. Eliminaremos los archivos de Python (`app.py`, `requirements.txt`) para mantener limpia la raíz.
>
> **Automatización de Base de Datos**:
> Mantendremos la automatización. La URL de conexión incluirá el parámetro `createDatabaseIfNotExist=true`, de modo que si no existe la base de datos `practica_bd`, MySQL la creará automáticamente al arrancar la aplicación. Las tablas se generarán automáticamente mediante Hibernate JPA, y sembraremos los clientes iniciales a través de un `CommandLineRunner` en Java.
>
> **Lanzamiento**:
> Dado que la máquina cuenta con Java 21 pero no tiene Maven global, incluiremos el cargador de Maven (`mvnw` y `mvnw.cmd`) en la raíz del proyecto para que pueda compilarse y ejecutarse fácilmente con `./mvnw spring-boot:run` sin necesidad de instalaciones manuales adicionales.

---

## Proposed Changes

### [Backend Java Spring Boot]

#### [NEW] [pom.xml](file:///c:/xampp/htdocs/Lab_software_u_3/pom.xml)
- Configuración de dependencias de Maven:
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-thymeleaf` (para servir la plantilla `index.html`)
  - `mysql-connector-j` (driver para conectar a MySQL)

#### [NEW] [src/main/resources/application.properties](file:///c:/xampp/htdocs/Lab_software_u_3/src/main/resources/application.properties)
- Configuración de Spring Data, Hibernate y base de datos:
  - `spring.datasource.url=jdbc:mysql://localhost:3307/practica_bd?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true`
  - `spring.datasource.username=root`
  - `spring.datasource.password=` (vacío)
  - `spring.jpa.hibernate.ddl-auto=update` (generación automática de tablas)
  - `spring.jpa.show-sql=true` (mostrar queries en consola para depuración)

#### [NEW] [src/main/java/com/example/banca/BancaApplication.java](file:///c:/xampp/htdocs/Lab_software_u_3/src/main/java/com/example/banca/BancaApplication.java)
- Clase principal que arranca la aplicación Spring Boot.

#### [NEW] [src/main/java/com/example/banca/model/Cliente.java](file:///c:/xampp/htdocs/Lab_software_u_3/src/main/java/com/example/banca/model/Cliente.java)
- Entidad JPA para la tabla `clientes` (campos `id`, `nombre`, `apellido`, `numeroCuenta`, `saldo`).

#### [NEW] [src/main/java/com/example/banca/model/Transferencia.java](file:///c:/xampp/htdocs/Lab_software_u_3/src/main/java/com/example/banca/model/Transferencia.java)
- Entidad JPA para la tabla `transferencias` (campos `id`, `clienteOrigen`, `cuentaDestino`, `monto`, `nombreDestinatario`, `fecha`).

#### [NEW] [src/main/java/com/example/banca/repository/ClienteRepository.java](file:///c:/xampp/htdocs/Lab_software_u_3/src/main/java/com/example/banca/repository/ClienteRepository.java)
- Interfaz JpaRepository para interactuar con la tabla `clientes`.

#### [NEW] [src/main/java/com/example/banca/repository/TransferenciaRepository.java](file:///c:/xampp/htdocs/Lab_software_u_3/src/main/java/com/example/banca/repository/TransferenciaRepository.java)
- Interfaz JpaRepository para interactuar con la tabla `transferencias`.

#### [NEW] [src/main/java/com/example/banca/config/DatabaseSeeder.java](file:///c:/xampp/htdocs/Lab_software_u_3/src/main/java/com/example/banca/config/DatabaseSeeder.java)
- Ejecuta el seeding de los 4 clientes de prueba al arrancar si la base de datos está vacía.

#### [NEW] [src/main/java/com/example/banca/controller/BancaApiController.java](file:///c:/xampp/htdocs/Lab_software_u_3/src/main/java/com/example/banca/controller/BancaApiController.java)
- Controlador REST para los endpoints:
  - `GET /api/clients`: Lista de clientes.
  - `GET /api/clients/{id}`: Detalle de cliente.
  - `GET /api/clients/{id}/transactions`: Historial de transacciones de un cliente.
  - `GET /api/recipient?cuenta={cuenta}`: Nombre de destinatario.
  - `POST /api/transfer`: Ejecuta la transferencia transaccional restando saldo al remitente e incrementándolo al destinatario.

#### [NEW] [src/main/java/com/example/banca/controller/HomeController.java](file:///c:/xampp/htdocs/Lab_software_u_3/src/main/java/com/example/banca/controller/HomeController.java)
- Controlador web simple para servir `index.html` en la raíz `/`.

---

### [Frontend Web & Estáticos]

Migraremos los archivos HTML, CSS y JS a la estructura estándar de recursos de Spring Boot.

#### [NEW] [src/main/resources/templates/index.html](file:///c:/xampp/htdocs/Lab_software_u_3/src/main/resources/templates/index.html)
- Copia de la plantilla HTML, adaptada para usar rutas Thymeleaf o relativas de Spring Boot:
  - `<link rel="stylesheet" href="/css/style.css">`
  - `<script src="/js/app.js"></script>`

#### [NEW] [src/main/resources/static/css/style.css](file:///c:/xampp/htdocs/Lab_software_u_3/src/main/resources/static/css/style.css)
- Hoja de estilos original con el diseño esmerilado y animaciones.

#### [NEW] [src/main/resources/static/js/app.js](file:///c:/xampp/htdocs/Lab_software_u_3/src/main/resources/static/js/app.js)
- Código javascript original para la lógica de transferencias e historial.

---

### [Limpieza]

#### [DELETE] [app.py](file:///c:/xampp/htdocs/Lab_software_u_3/app.py)
#### [DELETE] [requirements.txt](file:///c:/xampp/htdocs/Lab_software_u_3/requirements.txt)
#### [DELETE] [templates/index.html](file:///c:/xampp/htdocs/Lab_software_u_3/templates/index.html)
#### [DELETE] [static/css/style.css](file:///c:/xampp/htdocs/Lab_software_u_3/static/css/style.css)
#### [DELETE] [static/js/app.js](file:///c:/xampp/htdocs/Lab_software_u_3/static/js/app.js)

---

## Verification Plan

### Automated Tests
- Compilar el proyecto usando Maven Wrapper: `./mvnw clean package`
- Ejecutar la aplicación: `./mvnw spring-boot:run`
- Probar endpoints mediante peticiones `curl` o comandos HTTP.

### Manual Verification
- Iniciar la aplicación y abrir `http://localhost:8080` (puerto por defecto de Spring Boot).
- Realizar pruebas de flujo completo de transferencias y verificar que los datos se actualicen e inserten en MySQL de forma transaccional.
