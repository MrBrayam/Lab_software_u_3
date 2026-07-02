package com.example.banca.controller;

import com.example.banca.dto.TransferRequest;
import com.example.banca.model.Cliente;
import com.example.banca.model.Transferencia;
import com.example.banca.repository.ClienteRepository;
import com.example.banca.repository.TransferenciaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api")
public class BancaApiController {

    private final ClienteRepository clienteRepository;
    private final TransferenciaRepository transferenciaRepository;

    // Base de datos simulada de destinatarios para dar una experiencia premium (fallback)
    private static final Map<String, String> MOCK_RECIPIENTS = new HashMap<>();
    static {
        MOCK_RECIPIENTS.put("009-123456-78", "Carlos Alberto Mendoza");
        MOCK_RECIPIENTS.put("009-987654-32", "Ana María Rodríguez");
        MOCK_RECIPIENTS.put("009-555444-33", "Luis Fernando Torres");
        MOCK_RECIPIENTS.put("009-999999-99", "Gabriela Sofía Ortiz");
        MOCK_RECIPIENTS.put("009-111222-33", "Jorge Luis Bastidas");
    }

    private static final String[] FIRST_NAMES = {"María", "José", "Juan", "Luis", "Carlos", "Ana", "Luisa", "Jorge", "Sofía", "Pedro"};
    private static final String[] LAST_NAMES = {"Pérez", "Rodríguez", "González", "Gómez", "Fernández", "López", "Sánchez", "Martínez", "Torres", "Alva"};

    public BancaApiController(ClienteRepository clienteRepository, TransferenciaRepository transferenciaRepository) {
        this.clienteRepository = clienteRepository;
        this.transferenciaRepository = transferenciaRepository;
    }

    @GetMapping("/clients")
    public ResponseEntity<Map<String, Object>> getClients() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Cliente> clients = clienteRepository.findAll();
            
            // Formatear la lista de clientes para enviar la estructura exacta esperada por JS
            List<Map<String, Object>> formattedClients = new ArrayList<>();
            for (Cliente c : clients) {
                Map<String, Object> clientMap = new HashMap<>();
                clientMap.put("id", c.getId());
                clientMap.put("nombre", c.getNombre());
                clientMap.put("apellido", c.getApellido());
                clientMap.put("numero_cuenta", c.getNumeroCuenta());
                clientMap.put("saldo", c.getSaldo().toString());
                formattedClients.add(clientMap);
            }
            
            response.put("success", true);
            response.put("clients", formattedClients);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<Map<String, Object>> getClient(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Cliente> clientOpt = clienteRepository.findById(id);
            if (clientOpt.isPresent()) {
                Cliente c = clientOpt.get();
                Map<String, Object> clientMap = new HashMap<>();
                clientMap.put("id", c.getId());
                clientMap.put("nombre", c.getNombre());
                clientMap.put("apellido", c.getApellido());
                clientMap.put("numero_cuenta", c.getNumeroCuenta());
                clientMap.put("saldo", c.getSaldo().toString());

                response.put("success", true);
                response.put("client", clientMap);
                return ResponseEntity.ok(response);
            }
            response.put("success", false);
            response.put("message", "Cliente no encontrado");
            return ResponseEntity.status(404).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/clients/{id}/transactions")
    public ResponseEntity<Map<String, Object>> getClientTransactions(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Transferencia> transactions = transferenciaRepository.findByClienteOrigenIdOrderByFechaDesc(id);
            
            List<Map<String, Object>> formattedTxList = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            
            for (Transferencia t : transactions) {
                Map<String, Object> txMap = new HashMap<>();
                txMap.put("cuenta_destino", t.getCuentaDestino());
                txMap.put("monto", t.getMonto());
                txMap.put("nombre_destinatario", t.getNombreDestinatario());
                txMap.put("fecha_formateada", t.getFecha().format(formatter));
                formattedTxList.add(txMap);
            }
            
            response.put("success", true);
            response.put("transactions", formattedTxList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/recipient")
    public ResponseEntity<Map<String, Object>> getRecipient(@RequestParam String cuenta) {
        Map<String, Object> response = new HashMap<>();
        String cleanCuenta = cuenta.trim();
        if (cleanCuenta.isEmpty()) {
            response.put("success", false);
            response.put("message", "Cuenta no proporcionada");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 1. Intentar buscar en la base de datos si la cuenta pertenece a otro cliente registrado
            Optional<Cliente> dbClientOpt = clienteRepository.findByNumeroCuenta(cleanCuenta);
            if (dbClientOpt.isPresent()) {
                Cliente dbClient = dbClientOpt.get();
                response.put("success", true);
                response.put("name", dbClient.getNombre() + " " + dbClient.getApellido());
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            System.err.println("Error al buscar cliente por cuenta: " + e.getMessage());
        }

        // 2. Si no es un cliente, buscar en MOCK_RECIPIENTS o generar de forma determinista
        String name;
        if (MOCK_RECIPIENTS.containsKey(cleanCuenta)) {
            name = MOCK_RECIPIENTS.get(cleanCuenta);
        } else {
            // Generador determinista basado en el hash del número de cuenta
            int hash = Math.abs(cleanCuenta.hashCode());
            String firstName = FIRST_NAMES[hash % FIRST_NAMES.length];
            String lastName = LAST_NAMES[(hash / FIRST_NAMES.length) % LAST_NAMES.length];
            name = firstName + " " + lastName;
            MOCK_RECIPIENTS.put(cleanCuenta, name); // Cachearlo en memoria
        }

        response.put("success", true);
        response.put("name", name);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    @Transactional
    public ResponseEntity<Map<String, Object>> transfer(@RequestBody TransferRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        Long originId = request.getCliente_origen_id();
        String destAccount = request.getCuenta_destino() != null ? request.getCuenta_destino().trim() : "";
        BigDecimal amount = request.getMonto();
        String destName = request.getNombre_destinatario() != null ? request.getNombre_destinatario().trim() : "";

        if (originId == null || destAccount.isEmpty() || amount == null || destName.isEmpty()) {
            response.put("success", false);
            response.put("message", "Datos de transferencia incompletos");
            return ResponseEntity.badRequest().body(response);
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            response.put("success", false);
            response.put("message", "El monto debe ser mayor a 0");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 1. Obtener cliente de origen
            Optional<Cliente> originOpt = clienteRepository.findById(originId);
            if (originOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Cliente de origen no existe");
                return ResponseEntity.status(404).body(response);
            }
            Cliente origin = originOpt.get();

            // 2. Evitar transferirse a sí mismo
            if (origin.getNumeroCuenta().equals(destAccount)) {
                response.put("success", false);
                response.put("message", "No puede transferirse a su propia cuenta");
                return ResponseEntity.badRequest().body(response);
            }

            // 3. Validar saldo
            if (origin.getSaldo().compareTo(amount) < 0) {
                response.put("success", false);
                response.put("message", "Saldo insuficiente");
                return ResponseEntity.badRequest().body(response);
            }

            // 4. Descontar saldo al remitente
            origin.setSaldo(origin.getSaldo().subtract(amount));
            clienteRepository.save(origin);

            // 5. Si la cuenta destino pertenece a otro cliente registrado, incrementarle su saldo
            Optional<Cliente> destOpt = clienteRepository.findByNumeroCuenta(destAccount);
            if (destOpt.isPresent()) {
                Cliente dest = destOpt.get();
                dest.setSaldo(dest.getSaldo().add(amount));
                clienteRepository.save(dest);
            }

            // 6. Registrar la transferencia en el historial
            Transferencia tx = new Transferencia(origin, destAccount, amount, destName);
            transferenciaRepository.save(tx);

            response.put("success", true);
            response.put("new_balance", origin.getSaldo());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Spring @Transactional ejecutará el rollback automáticamente en caso de error
            response.put("success", false);
            response.put("message", "Error interno en la transferencia: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
