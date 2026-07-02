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

# Saldo inicial en memoria para la sesión de demostración
current_balance = 80.00

# Base de datos simulada de destinatarios para dar una experiencia premium
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
    """Inicializa la base de datos y la tabla de transferencias si no existen."""
    try:
        # Primero conectar sin base de datos para crearla si no existe
        conn = get_connection(include_db=False)
        with conn.cursor() as cursor:
            cursor.execute(f"CREATE DATABASE IF NOT EXISTS {DB_NAME}")
        conn.close()

        # Conectar a la base de datos recién creada/existente para crear la tabla
        conn = get_connection(include_db=True)
        with conn.cursor() as cursor:
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS transferencias (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    cuenta_destino VARCHAR(50) NOT NULL,
                    monto DECIMAL(10, 2) NOT NULL,
                    nombre_destinatario VARCHAR(100) NOT NULL,
                    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """)
        conn.close()
        print("Base de datos y tabla inicializadas correctamente.")
    except Exception as e:
        print(f"Error al inicializar la base de datos: {e}")

# Inicializar base de datos al arrancar
init_db()

@app.route('/')
def index():
    """Ruta principal que sirve la Single Page Application."""
    return render_template('index.html')

@app.route('/api/balance', methods=['GET'])
def get_balance():
    """Endpoint para obtener el saldo actual."""
    global current_balance
    return jsonify({
        'success': True,
        'balance': round(current_balance, 2)
    })

@app.route('/api/recipient', methods=['GET'])
def get_recipient():
    """Endpoint para consultar o generar el nombre asociado a una cuenta destino."""
    cuenta = request.args.get('cuenta', '').strip()
    if not cuenta:
        return jsonify({'success': False, 'message': 'Cuenta no proporcionada'}), 400

    # Si la cuenta está en el diccionario simulado, la retornamos
    if cuenta in MOCK_RECIPIENTS:
        name = MOCK_RECIPIENTS[cuenta]
    else:
        # Generar un nombre aleatorio pero determinista basado en el número de cuenta
        # para que siempre retorne el mismo nombre para la misma cuenta en esta sesión
        random.seed(cuenta)
        name = f"{random.choice(FIRST_NAMES)} {random.choice(LAST_NAMES)}"
        # Guardarlo para que sea consistente
        MOCK_RECIPIENTS[cuenta] = name
        
    return jsonify({
        'success': True,
        'name': name
    })

@app.route('/api/transfer', methods=['POST'])
def transfer():
    """Endpoint para registrar y confirmar la transferencia en la base de datos."""
    global current_balance
    data = request.json or {}
    
    cuenta_destino = data.get('cuenta_destino', '').strip()
    monto_raw = data.get('monto')
    nombre_destinatario = data.get('nombre_destinatario', '').strip()

    if not cuenta_destino or monto_raw is None or not nombre_destinatario:
        return jsonify({'success': False, 'message': 'Datos incompletos'}), 400

    try:
        monto = float(monto_raw)
    except ValueError:
        return jsonify({'success': False, 'message': 'Monto inválido'}), 400

    if monto <= 0:
        return jsonify({'success': False, 'message': 'El monto debe ser mayor a 0'}), 400

    if monto > current_balance:
        return jsonify({'success': False, 'message': 'Saldo insuficiente'}), 400

    # Guardar en base de datos
    try:
        conn = get_connection(include_db=True)
        with conn.cursor() as cursor:
            sql = "INSERT INTO transferencias (cuenta_destino, monto, nombre_destinatario) VALUES (%s, %s, %s)"
            cursor.execute(sql, (cuenta_destino, monto, nombre_destinatario))
        conn.commit()
        conn.close()
        
        # Descontar del saldo local
        current_balance -= monto
        
        return jsonify({
            'success': True,
            'new_balance': round(current_balance, 2)
        })
    except Exception as e:
        print(f"Error al registrar transferencia: {e}")
        return jsonify({'success': False, 'message': 'Error interno al registrar la transferencia'}), 500

if __name__ == '__main__':
    # Ejecutamos en el puerto 5000 por defecto de Flask
    app.run(host='0.0.0.0', port=5000, debug=True)
