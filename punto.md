# Explicación del sistema de transferencias

Este documento resume cómo funciona el proyecto y por qué cada archivo usa ciertos imports y funciones. La idea es entender el código desde arriba hacia abajo: primero la aplicación, luego la autenticación, después las transferencias y por último el frontend.

---

## 1. Vista general del sistema

La aplicación está construida con **Spring Boot + MySQL + HTML/CSS/JavaScript**.

El flujo general es este:

1. El usuario abre la página principal.
2. El sistema permite iniciar sesión con número de cuenta y PIN, o crear una cuenta nueva.
3. Una vez autenticado, se cargan sus datos, saldo e historial.
4. El usuario puede realizar transferencias.
5. Cada transferencia se guarda en la base de datos y actualiza el saldo del cliente origen y destino.

---

## 2. Backend: por qué usa imports y funciones

### `AuthController.java`

Este controlador maneja el acceso al sistema.

#### Imports principales y su razón de uso

- `LoginRequest` y `RegisterRequest`: representan los datos que llegan desde el frontend al iniciar sesión o crear cuenta.
- `Cliente`: se usa para leer y crear registros de clientes.
- `ClienteRepository`: permite consultar y guardar clientes en la base de datos.
- `HttpSession`: guarda el cliente autenticado durante la sesión del navegador.
- `ResponseEntity`: devuelve respuestas HTTP con estado y mensaje.
- `@GetMapping`, `@PostMapping`, `@RequestBody`, `@RequestMapping`, `@RestController`: anotaciones de Spring para exponer endpoints REST.
- `HashMap`, `Map`, `Optional`: sirven para construir respuestas JSON y manejar búsquedas seguras.

#### Funciones importantes

- `login(...)`: valida número de cuenta y PIN. Si existen, guarda el cliente en sesión.
- `register(...)`: crea una nueva cuenta de cliente. El saldo inicial se fuerza a `0.00`.
- `me(...)`: verifica si existe una sesión activa y devuelve el cliente autenticado.
- `logout(...)`: elimina la sesión.
- `getAuthenticatedClient(...)`: recupera el cliente guardado en sesión.
- `buildClientMap(...)`: convierte el objeto `Cliente` en un mapa listo para JSON.

### `BancaApiController.java`

Este controlador contiene la lógica de consulta y transferencia.

#### Imports principales y su razón de uso

- `TransferRequest`: contiene los datos que llegan al transferir.
- `Cliente` y `Transferencia`: son las entidades JPA del sistema.
- `ClienteRepository` y `TransferenciaRepository`: permiten leer y guardar datos.
- `HttpSession`: sirve para restringir las acciones al cliente autenticado.
- `ResponseEntity`: responde con estados como 200, 401, 403 o 404.
- `@Transactional`: garantiza que una transferencia complete o se deshaga por completo.
- `DateTimeFormatter`: formatea la fecha del historial para mostrarla bonita en el frontend.
- `Comparator` y `Collectors`: se usan para ordenar el historial por fecha.

#### Funciones importantes

- `getClients(...)`: devuelve solo el cliente autenticado.
- `getClient(...)`: devuelve los datos del cliente autenticado si coincide con el id solicitado.
- `getClientTransactions(...)`: junta ingresos y egresos del historial.
- `getRecipient(...)`: verifica si la cuenta destino existe en la base de datos y devuelve el nombre del destinatario real.
- `transfer(...)`: ejecuta la transferencia, descuenta al origen, suma al destino y guarda el movimiento.
- `requireAuthenticatedClient(...)`: valida que la sesión exista antes de permitir operaciones.
- `unauthorized(...)`: construye la respuesta estándar cuando no hay sesión.
- `buildClientMap(...)`: reutiliza el formato JSON del cliente.

### `Cliente.java`

Esta es la entidad que representa la tabla `clientes`.

#### Imports y uso

- `jakarta.persistence.*`: permite que la clase funcione como entidad JPA.
- `BigDecimal`: se usa para manejar el saldo con precisión decimal.

#### Funciones y campos

- Los atributos (`id`, `nombre`, `apellido`, `numeroCuenta`, `pin`, `saldo`) representan las columnas de la tabla.
- Los getters y setters permiten leer y modificar esos valores.
- El constructor con parámetros se usa para crear clientes desde el seeder o desde el registro.

### `Transferencia.java`

Representa la tabla `transferencias`.

#### Imports y uso

- `jakarta.persistence.*`: convierte la clase en entidad JPA.
- `BigDecimal`: guarda el monto transferido.
- `LocalDateTime`: guarda la fecha exacta de la transferencia.

#### Funciones importantes

- `onCreate()`: se ejecuta antes de guardar y asigna la fecha actual.
- Getters y setters: permiten consultar los datos de la transferencia.

### Repositorios

#### `ClienteRepository.java`

Usa `JpaRepository` para evitar escribir SQL manual.

- `findByNumeroCuenta(...)`: busca un cliente por cuenta.
- `findByNumeroCuentaAndPin(...)`: valida credenciales en el login.

#### `TransferenciaRepository.java`

Permite consultar transferencias por cliente origen o por cuenta destino.

- `findByClienteOrigenIdOrderByFechaDesc(...)`: trae egresos del usuario.
- `findByCuentaDestinoOrderByFechaDesc(...)`: trae ingresos recibidos.

### `DatabaseSeeder.java`

Este componente se ejecuta al arrancar la aplicación.

#### Imports y uso

- `CommandLineRunner`: permite correr código automáticamente al iniciar Spring Boot.
- `ClienteRepository`: se usa para verificar si ya hay clientes y sembrarlos si la tabla está vacía.
- `BigDecimal`, `Arrays`, `HashMap`, `Map`: ayudan a crear y revisar los datos iniciales.

#### Funciones importantes

- `run(...)`: inserta clientes de prueba si la base está vacía.

---

## 3. Frontend: por qué usa funciones

### `index.html`

Contiene la estructura visual:

- login
- registro
- panel de transferencias
- historial
- modal de mensajes

No tiene lógica pesada: solo organiza los bloques que luego controla JavaScript.

### `app.js`

Este archivo controla la interacción del usuario.

#### Funciones principales

- `initializeSession()`: pregunta al backend si ya existe una sesión activa.
- `loginForm.addEventListener(...)`: envía el login al backend.
- `registerForm.addEventListener(...)`: crea una nueva cuenta.
- `loadClientData(...)`: carga saldo, nombre y cuenta del cliente.
- `loadTransactions(...)`: trae el historial.
- `renderTransactions(...)`: dibuja los movimientos en pantalla.
- `validateAmount()`: evita transferencias inválidas.
- `transferForm.addEventListener(...)`: consulta el destinatario.
- `btnConfirmar.addEventListener(...)`: ejecuta la transferencia real.
- `showMessageModal(...)`: muestra errores o mensajes exitosos en una ventana flotante.
- `setAuthMode(...)`: alterna entre login y registro.
- `formatAccountInput(...)`: aplica la máscara de cuenta `009-XXXXXX-XX`.

#### Por qué usa estos eventos

- Los `addEventListener` permiten reaccionar cuando el usuario escribe, envía formularios o pulsa botones.
- `fetch(...)` conecta el frontend con los endpoints Spring Boot.
- `classList.add/remove/toggle(...)` cambia pantallas y muestra u oculta secciones sin recargar la página.

### `style.css`

Define la apariencia del sistema:

- colores
- tarjetas con efecto glassmorphism
- animaciones
- modal flotante
- responsive para móvil

---

## 4. Flujo real del código

### Registro de cliente

1. El usuario abre la pestaña Crear cuenta.
2. Escribe nombre, apellido, cuenta y PIN.
3. `app.js` envía los datos a `/api/auth/register`.
4. `AuthController` valida que la cuenta no exista.
5. Se crea el cliente con saldo `0.00`.
6. Se guarda el cliente en sesión y se muestra el panel principal.

### Inicio de sesión

1. El usuario escribe cuenta y PIN.
2. `app.js` envía los datos a `/api/auth/login`.
3. `AuthController` busca el cliente en la base.
4. Si coincide, el cliente queda autenticado.
5. El frontend carga saldo, cuenta e historial.

### Transferencia

1. El usuario ingresa cuenta destino y monto.
2. `app.js` consulta al backend si la cuenta existe.
3. Si existe, se muestra la pantalla de confirmación.
4. Al confirmar, el backend descuenta el saldo del origen y suma al destino.
5. Se registra el movimiento en `transferencias`.
6. El historial se actualiza.

---

## 5. Por qué este diseño es correcto

- `Repository` evita SQL manual y simplifica el acceso a datos.
- `HttpSession` protege las acciones para que solo un cliente autenticado pueda operar.
- `ResponseEntity` permite responder con mensajes claros al frontend.
- `@Transactional` evita inconsistencias si algo falla durante la transferencia.
- `fetch + eventos + modal` dan una experiencia fluida sin recargar la página.

---

## 6. Resumen corto

El sistema usa imports para traer clases, anotaciones y utilidades necesarias para cada capa. Las funciones existen para separar responsabilidades: autenticación, consulta de datos, transferencia, historial y presentación. Esa separación hace que el código sea más fácil de mantener, probar y extender.