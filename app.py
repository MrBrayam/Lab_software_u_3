from flask import Flask, render_template, request, jsonify
import pymysql
import os
import random

app = Flask(__name__)

# Configuración de base de datos
DB_HOST = '127.0.0.1'
DB_PORT = 3307
DB_USER = 'root'
DB_PASSWORD = ''
DB_NAME = 'practica_bd'

# Base de datos simulada de destinatarios para dar una experiencia premium (fallback)
MOCK_RECIPIENTS = {
    '009-123456-78': 'Carlos Alberto Mendoza',
    '009-987654-32': 'Ana María Rodríguez',
    '009-555444-33': 'Luis Fernando Torres',
    '009-999999-99': 'Gabriela Sofía Ortiz',
    '009-111222-33': 'Jorge Luis Bastidas'
}

FIRST_NAMES = ['María', 'José', 'Juan', 'Luis', 'Carlos', 'Ana', 'Luisa', 'Jorge', 'Sofía', 'Pedro']
LAST_NAMES = ['Pérez', 'Rodríguez', 'González', 'Gómez', 'Fernández', 'López', 'Sánchez', 'Martínez', 'Torres', 'Alva']

def get_connection(include_db=True):
    """Establece conexión con el servidor MySQL local."""
    return pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME if include_db else None,
        cursorclass=pymysql.cursors.DictCursor
    )

def init_db():
    """Inicializa la base de datos, tablas de clientes y transferencias, y siembra datos iniciales."""
    try:
        # 1. Crear base de datos si no existe
        conn = get_connection(include_db=False)
        with conn.cursor() as cursor:
            cursor.execute(f"CREATE DATABASE IF NOT EXISTS {DB_NAME}")
        conn.close()

        # 2. Conectar a la base de datos e inicializar tablas
        conn = get_connection(include_db=True)
        with conn.cursor() as cursor:
            # Crear tabla de clientes
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS clientes (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    nombre VARCHAR(50) NOT NULL,
                    apellido VARCHAR(50) NOT NULL,
                    numero_cuenta VARCHAR(50) UNIQUE NOT NULL,
                    saldo DECIMAL(10, 2) NOT NULL DEFAULT 80.00
                )
            """)
            
            # Crear tabla de transferencias vinculada a clientes
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS transferencias (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    cliente_origen_id INT NOT NULL,
                    cuenta_destino VARCHAR(50) NOT NULL,
                    monto DECIMAL(10, 2) NOT NULL,
                    nombre_destinatario VARCHAR(100) NOT NULL,
                    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (cliente_origen_id) REFERENCES clientes(id) ON DELETE CASCADE
                )
            """)
            
            # 3. Sembrar datos de clientes de prueba si la tabla está vacía
            cursor.execute("SELECT COUNT(*) as count FROM clientes")
            result = cursor.fetchone()
            if result['count'] == 0:
                clientes_iniciales = [
                    ('Carlos Alberto', 'Mendoza', '009-123456-78', 120.00),
                    ('Ana María', 'Rodríguez', '009-987654-32', 80.00),
                    ('Luis Fernando', 'Torres', '009-555444-33', 250.00),
                    ('Gabriela Sofía', 'Ortiz', '009-999999-99', 80.00)
                ]
                cursor.executemany(
                    "INSERT INTO clientes (nombre, apellido, numero_cuenta, saldo) VALUES (%s, %s, %s, %s)",
                    clientes_iniciales
                )
                print("Clientes sembrados correctamente.")
                
        conn.commit()
        conn.close()
        print("Base de datos y tablas inicializadas correctamente.")
    except Exception as e:
        print(f"Error al inicializar la base de datos: {e}")

# Inicializar base de datos al arrancar
init_db()

@app.route('/')
def index():
    """Ruta principal que sirve la Single Page Application."""
    return render_template('index.html')

@app.route('/api/clients', methods=['GET'])
def get_clients():
    """Endpoint para obtener la lista de clientes."""
    try:
        conn = get_connection()
        with conn.cursor() as cursor:
            cursor.execute("SELECT id, nombre, apellido, numero_cuenta, ROUND(saldo, 2) as saldo FROM clientes")
            clients = cursor.fetchall()
        conn.close()
        return jsonify({'success': True, 'clients': clients})
    except Exception as e:
        print(f"Error al obtener clientes: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/clients/<int:client_id>', methods=['GET'])
def get_client(client_id):
    """Endpoint para obtener los detalles de un cliente específico."""
    try:
        conn = get_connection()
        with conn.cursor() as cursor:
            cursor.execute("SELECT id, nombre, apellido, numero_cuenta, ROUND(saldo, 2) as saldo FROM clientes WHERE id = %s", (client_id,))
            client = cursor.fetchone()
        conn.close()
        if client:
            return jsonify({'success': True, 'client': client})
            
        return jsonify({'success': False, 'message': 'Cliente no encontrado'}), 404
    except Exception as e:
        print(f"Error al obtener cliente: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/clients/<int:client_id>/transactions', methods=['GET'])
def get_client_transactions(client_id):
    """Endpoint para obtener el historial de transacciones de un cliente."""
    try:
        conn = get_connection()
        with conn.cursor() as cursor:
            cursor.execute("""
                SELECT cuenta_destino, ROUND(monto, 2) as monto, nombre_destinatario, 
                       DATE_FORMAT(fecha, '%%d/%%m/%%Y %%H:%%i') as fecha_formateada 
                FROM transferencias 
                WHERE cliente_origen_id = %s 
                ORDER BY fecha DESC
            """, (client_id,))
            transactions = cursor.fetchall()
        conn.close()
        return jsonify({'success': True, 'transactions': transactions})
    except Exception as e:
        print(f"Error al obtener transacciones: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/recipient', methods=['GET'])
def get_recipient():
    """Endpoint para consultar o generar el nombre asociado a una cuenta destino."""
    cuenta = request.args.get('cuenta', '').strip()
    if not cuenta:
        return jsonify({'success': False, 'message': 'Cuenta no proporcionada'}), 400

    # 1. Intentar buscar en la base de datos si es una cuenta de otro cliente registrado
    try:
        conn = get_connection()
        with conn.cursor() as cursor:
            cursor.execute("SELECT nombre, apellido FROM clientes WHERE numero_cuenta = %s", (cuenta,))
            db_client = cursor.fetchone()
        conn.close()
        
        if db_client:
            return jsonify({
                'success': True,
                'name': f"{db_client['nombre']} {db_client['apellido']}"
            })
    except Exception as e:
        print(f"Error al buscar cliente por cuenta: {e}")

    # 2. Si no es un cliente, buscar en MOCK_RECIPIENTS o autogenerar
    if cuenta in MOCK_RECIPIENTS:
        name = MOCK_RECIPIENTS[cuenta]
    else:
        random.seed(cuenta)
        name = f"{random.choice(FIRST_NAMES)} {random.choice(LAST_NAMES)}"
        MOCK_RECIPIENTS[cuenta] = name
        
    return jsonify({
        'success': True,
        'name': name
    })

@app.route('/api/transfer', methods=['POST'])
def transfer():
    """Endpoint transaccional para realizar la transferencia y descontar del saldo."""
    data = request.json or {}
    
    cliente_origen_id = data.get('cliente_origen_id')
    cuenta_destino = data.get('cuenta_destino', '').strip()
    monto_raw = data.get('monto')
    nombre_destinatario = data.get('nombre_destinatario', '').strip()

    if cliente_origen_id is None or not cuenta_destino or monto_raw is None or not nombre_destinatario:
        return jsonify({'success': False, 'message': 'Datos de transferencia incompletos'}), 400

    try:
        monto = float(monto_raw)
    except ValueError:
        return jsonify({'success': False, 'message': 'Monto inválido'}), 400

    if monto <= 0:
        return jsonify({'success': False, 'message': 'El monto debe ser mayor a 0'}), 400

    conn = get_connection()
    try:
        conn.begin()
        with conn.cursor() as cursor:
            # 1. Obtener y bloquear la fila del cliente de origen para evitar condiciones de carrera (FOR UPDATE)
            cursor.execute("SELECT saldo, numero_cuenta FROM clientes WHERE id = %s FOR UPDATE", (cliente_origen_id,))
            cliente = cursor.fetchone()
            
            if not cliente:
                conn.rollback()
                return jsonify({'success': False, 'message': 'Cliente de origen no existe'}), 404
                
            if cliente['numero_cuenta'] == cuenta_destino:
                conn.rollback()
                return jsonify({'success': False, 'message': 'No puede transferirse a su propia cuenta'}), 400

            saldo_actual = float(cliente['saldo'])
            if monto > saldo_actual:
                conn.rollback()
                return jsonify({'success': False, 'message': 'Saldo insuficiente'}), 400

            # 2. Descontar del saldo del cliente de origen
            nuevo_saldo = saldo_actual - monto
            cursor.execute("UPDATE clientes SET saldo = %s WHERE id = %s", (nuevo_saldo, cliente_origen_id))

            # 3. Si la cuenta destino pertenece a otro cliente registrado, incrementarle su saldo
            cursor.execute("UPDATE clientes SET saldo = saldo + %s WHERE numero_cuenta = %s", (monto, cuenta_destino))

            # 4. Registrar la transferencia en el historial
            sql = """
                INSERT INTO transferencias (cliente_origen_id, cuenta_destino, monto, nombre_destinatario) 
                VALUES (%s, %s, %s, %s)
            """
            cursor.execute(sql, (cliente_origen_id, cuenta_destino, monto, nombre_destinatario))
            
        conn.commit()
        return jsonify({
            'success': True,
            'new_balance': round(nuevo_saldo, 2)
        })
    except Exception as e:
        conn.rollback()
        print(f"Error en la transacción de transferencia: {e}")
        return jsonify({'success': False, 'message': f'Error interno en la transferencia: {str(e)}'}), 500
    finally:
        conn.close()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
