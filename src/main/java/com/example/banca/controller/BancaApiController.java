package com.example.banca.controller;

import com.example.banca.dto.TransferRequest;
import com.example.banca.model.Cliente;
import com.example.banca.model.Transferencia;
import com.example.banca.repository.ClienteRepository;
import com.example.banca.repository.TransferenciaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class BancaApiController {

    private final ClienteRepository clienteRepository;
    private final TransferenciaRepository transferenciaRepository;

    public BancaApiController(ClienteRepository clienteRepository, TransferenciaRepository transferenciaRepository) {
        this.clienteRepository = clienteRepository;
        this.transferenciaRepository = transferenciaRepository;
    }

    @GetMapping("/clients")
    public ResponseEntity<Map<String, Object>> getClients(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Cliente authenticatedClient = requireAuthenticatedClient(session);
            if (authenticatedClient == null) {
                return unauthorized(response);
            }

            // Mantener la estructura de respuesta, pero limitarla a la sesión activa.
            List<Map<String, Object>> formattedClients = new ArrayList<>();
            formattedClients.add(buildClientMap(authenticatedClient));
            
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
    public ResponseEntity<Map<String, Object>> getClient(@PathVariable Long id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Cliente authenticatedClient = requireAuthenticatedClient(session);
            if (authenticatedClient == null) {
                return unauthorized(response);
            }

            if (!authenticatedClient.getId().equals(id)) {
                response.put("success", false);
                response.put("message", "No autorizado");
                return ResponseEntity.status(403).body(response);
            }

            Optional<Cliente> clientOpt = clienteRepository.findById(id);
            if (clientOpt.isPresent()) {
                Cliente c = clientOpt.get();
                response.put("success", true);
                response.put("client", buildClientMap(c));
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
    public ResponseEntity<Map<String, Object>> getClientTransactions(@PathVariable Long id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Cliente authenticatedClient = requireAuthenticatedClient(session);
            if (authenticatedClient == null) {
                return unauthorized(response);
            }

            if (!authenticatedClient.getId().equals(id)) {
                response.put("success", false);
                response.put("message", "No autorizado");
                return ResponseEntity.status(403).body(response);
            }

            List<Transferencia> transactions = transferenciaRepository.findByClienteOrigenIdOrderByFechaDesc(id);
            transactions.addAll(transferenciaRepository.findByCuentaDestinoOrderByFechaDesc(authenticatedClient.getNumeroCuenta()));
            transactions = transactions.stream()
                    .sorted(Comparator.comparing(Transferencia::getFecha).reversed())
                    .collect(Collectors.toList());
            
            List<Map<String, Object>> formattedTxList = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            
            for (Transferencia t : transactions) {
                Map<String, Object> txMap = new HashMap<>();
                txMap.put("cuenta_destino", t.getCuentaDestino());
                txMap.put("monto", t.getMonto());
                txMap.put("nombre_destinatario", t.getNombreDestinatario());
                txMap.put("fecha_formateada", t.getFecha().format(formatter));
                boolean incoming = authenticatedClient.getNumeroCuenta().equals(t.getCuentaDestino());
                txMap.put("tipo", incoming ? "ingreso" : "egreso");
                txMap.put("monto_visible", incoming ? t.getMonto() : t.getMonto().negate());
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
    public ResponseEntity<Map<String, Object>> getRecipient(@RequestParam String cuenta, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Cliente authenticatedClient = requireAuthenticatedClient(session);
        if (authenticatedClient == null) {
            return unauthorized(response);
        }

        String cleanCuenta = cuenta.trim();
        if (cleanCuenta.isEmpty()) {
            response.put("success", false);
            response.put("message", "Cuenta no proporcionada");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Optional<Cliente> dbClientOpt = clienteRepository.findByNumeroCuenta(cleanCuenta);
            if (dbClientOpt.isPresent()) {
                Cliente dbClient = dbClientOpt.get();
                if (authenticatedClient.getNumeroCuenta().equals(dbClient.getNumeroCuenta())) {
                    response.put("success", false);
                    response.put("message", "No puede transferirse a su propia cuenta");
                    return ResponseEntity.badRequest().body(response);
                }

                response.put("success", true);
                response.put("name", dbClient.getNombre() + " " + dbClient.getApellido());
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            System.err.println("Error al buscar cliente por cuenta: " + e.getMessage());
        }

        response.put("success", false);
        response.put("message", "La cuenta no existe en la base de datos");
        return ResponseEntity.status(404).body(response);
    }

    @PostMapping("/transfer")
    @Transactional
    public ResponseEntity<Map<String, Object>> transfer(@RequestBody TransferRequest request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        Cliente authenticatedClient = requireAuthenticatedClient(session);
        if (authenticatedClient == null) {
            return unauthorized(response);
        }

        Long originId = authenticatedClient.getId();
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
            Cliente origin = authenticatedClient;

            Optional<Cliente> destOpt = clienteRepository.findByNumeroCuenta(destAccount);
            if (destOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "La cuenta destino no existe en la base de datos");
                return ResponseEntity.status(404).body(response);
            }

            Cliente dest = destOpt.get();

            // 2. Evitar transferirse a sí mismo
            if (origin.getNumeroCuenta().equals(dest.getNumeroCuenta())) {
                response.put("success", false);
                response.put("message", "No puede transferirse a su propia cuenta");
                return ResponseEntity.badRequest().body(response);
            }

            if (destName.isEmpty()) {
                destName = dest.getNombre() + " " + dest.getApellido();
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

            // 5. Incrementar saldo al cliente destino registrado
            dest.setSaldo(dest.getSaldo().add(amount));
            clienteRepository.save(dest);

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

    private Cliente requireAuthenticatedClient(HttpSession session) {
        Object clientId = session.getAttribute(AuthController.SESSION_CLIENT_ID);
        if (!(clientId instanceof Long authenticatedId)) {
            return null;
        }

        return clienteRepository.findById(authenticatedId).orElse(null);
    }

    private ResponseEntity<Map<String, Object>> unauthorized(Map<String, Object> response) {
        response.put("success", false);
        response.put("message", "Sesión no autenticada");
        return ResponseEntity.status(401).body(response);
    }

    private Map<String, Object> buildClientMap(Cliente c) {
        Map<String, Object> clientMap = new HashMap<>();
        clientMap.put("id", c.getId());
        clientMap.put("nombre", c.getNombre());
        clientMap.put("apellido", c.getApellido());
        clientMap.put("numero_cuenta", c.getNumeroCuenta());
        clientMap.put("saldo", c.getSaldo().toString());
        return clientMap;
    }
}
