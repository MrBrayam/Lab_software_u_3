# Sistema de Transferencias Bancarias (Flask + MySQL)

Este es un proyecto premium de banca móvil con un flujo de transferencia en 3 pantallas, selector de clientes activos e historial de transacciones dinámico.

---

## 🚀 Creación Automatizada de Base de Datos

**La base de datos se crea e inicializa de forma 100% automática al iniciar la aplicación.** 

Cuando cualquier persona clona el repositorio y ejecuta la aplicación por primera vez, el archivo backend `app.py` realiza los siguientes pasos de forma automática:
1. **Conexión al Servidor**: Se conecta a tu servidor MySQL local (`localhost:3307` con el usuario `root` sin contraseña).
2. **Creación de la BD**: Si la base de datos `practica_bd` no existe en el servidor, el script ejecuta la instrucción `CREATE DATABASE IF NOT EXISTS practica_bd` para crearla al instante.
3. **Estructura de Tablas**: Crea las tablas `clientes` y `transferencias` (vinculadas mediante claves foráneas) en caso de que falten.
4. **Siembra de Datos (Seeding)**: Inserta automáticamente 4 clientes de prueba con saldos iniciales reales para que la aplicación sea totalmente interactiva desde el primer segundo.

---

## 🛠️ Requisitos e Instalación

### 1. Requisitos Previos
- Tener instalado **Python 3.8 o superior**.
- Tener activo **MySQL** en el puerto `3307` (por ejemplo, mediante XAMPP configurando el puerto en `3307`).

### 2. Instalación de Dependencias
Abre una terminal en la carpeta raíz del proyecto y ejecuta el siguiente comando para instalar Flask y los conectores de base de datos necesarios:

```bash
pip install -r requirements.txt
```

---

## 🏃 Cómo Ejecutar la Aplicación

Para arrancar el servidor web de desarrollo:

```bash
python app.py
```

Una vez ejecutado, abre tu navegador web y entra a:
👉 **[http://localhost:5000](http://localhost:5000)**

---

## 👥 Clientes de Prueba Creados Automáticamente

Al iniciar, se sembrarán las siguientes cuentas en la base de datos:
- **Carlos Alberto Mendoza** (Cuenta: `009-123456-78`, Saldo Inicial: S/ 120.00)
- **Ana María Rodríguez** (Cuenta: `009-987654-32`, Saldo Inicial: S/ 80.00)
- **Luis Fernando Torres** (Cuenta: `009-555444-33`, Saldo Inicial: S/ 250.00)
- **Gabriela Sofía Ortiz** (Cuenta: `009-999999-99`, Saldo Inicial: S/ 80.00)
