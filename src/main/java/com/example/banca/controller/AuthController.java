package com.example.banca.controller;

import com.example.banca.dto.LoginRequest;
import com.example.banca.dto.RegisterRequest;
import com.example.banca.dto.UpdateProfileRequest;
import com.example.banca.model.Cliente;
import com.example.banca.repository.ClienteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String SESSION_CLIENT_ID = "AUTH_CLIENT_ID";

    private final ClienteRepository clienteRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AuthController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        String numeroCuenta = request.getNumero_cuenta() != null ? request.getNumero_cuenta().trim() : "";
        String pin = request.getPin() != null ? request.getPin().trim() : "";

        if (numeroCuenta.isEmpty() || pin.isEmpty()) {
            response.put("success", false);
            response.put("message", "Cuenta y PIN son obligatorios");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<Cliente> clienteOpt = clienteRepository.findByNumeroCuentaAndPin(numeroCuenta, pin);
        if (clienteOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Credenciales inválidas");
            return ResponseEntity.status(401).body(response);
        }

        Cliente cliente = clienteOpt.get();
        session.setAttribute(SESSION_CLIENT_ID, cliente.getId());
        response.put("success", true);
        response.put("client", buildClientMap(cliente));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        String nombre = request.getNombre() != null ? request.getNombre().trim() : "";
        String apellido = request.getApellido() != null ? request.getApellido().trim() : "";
        String numeroCuenta = request.getNumero_cuenta() != null ? request.getNumero_cuenta().trim() : "";
        String pin = request.getPin() != null ? request.getPin().trim() : "";
        if (nombre.isEmpty() || apellido.isEmpty() || numeroCuenta.isEmpty() || pin.isEmpty()) {
            response.put("success", false);
            response.put("message", "Nombre, apellido, cuenta y PIN son obligatorios");
            return ResponseEntity.badRequest().body(response);
        }

        if (numeroCuenta.length() < 13) {
            response.put("success", false);
            response.put("message", "La cuenta debe tener el formato 009-XXXXXX-XX");
            return ResponseEntity.badRequest().body(response);
        }

        if (pin.length() < 4) {
            response.put("success", false);
            response.put("message", "El PIN debe tener al menos 4 dígitos");
            return ResponseEntity.badRequest().body(response);
        }

        if (clienteRepository.findByNumeroCuenta(numeroCuenta).isPresent()) {
            response.put("success", false);
            response.put("message", "Ya existe una cuenta registrada con ese número");
            return ResponseEntity.status(409).body(response);
        }

        Cliente cliente = new Cliente(nombre, apellido, numeroCuenta, pin, BigDecimal.ZERO);
        allowClientUpdates();
        try {
            clienteRepository.save(cliente);
            session.setAttribute(SESSION_CLIENT_ID, cliente.getId());
        } finally {
            disableClientUpdates();
        }

        response.put("success", true);
        response.put("client", buildClientMap(cliente));
        response.put("message", "Cuenta creada correctamente");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody UpdateProfileRequest request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        Cliente cliente = getAuthenticatedClient(session);
        if (cliente == null) {
            response.put("success", false);
            response.put("message", "Sesión no autenticada");
            return ResponseEntity.status(401).body(response);
        }

        String nombre = request.getNombre() != null ? request.getNombre().trim() : "";
        String apellido = request.getApellido() != null ? request.getApellido().trim() : "";
        String pin = request.getPin() != null ? request.getPin().trim() : "";

        if (nombre.isEmpty() || apellido.isEmpty() || pin.isEmpty()) {
            response.put("success", false);
            response.put("message", "Nombre, apellido y PIN son obligatorios");
            return ResponseEntity.badRequest().body(response);
        }

        if (pin.length() < 4) {
            response.put("success", false);
            response.put("message", "El PIN debe tener al menos 4 dígitos");
            return ResponseEntity.badRequest().body(response);
        }

        allowClientUpdates();
        try {
            cliente.setNombre(nombre);
            cliente.setApellido(apellido);
            cliente.setPin(pin);
            clienteRepository.save(cliente);
        } finally {
            disableClientUpdates();
        }

        response.put("success", true);
        response.put("client", buildClientMap(cliente));
        response.put("message", "Datos actualizados correctamente");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Cliente cliente = getAuthenticatedClient(session);
        if (cliente == null) {
            response.put("authenticated", false);
            return ResponseEntity.ok(response);
        }

        response.put("authenticated", true);
        response.put("client", buildClientMap(cliente));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        session.invalidate();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    public Cliente getAuthenticatedClient(HttpSession session) {
        Object clientId = session.getAttribute(SESSION_CLIENT_ID);
        if (!(clientId instanceof Long authenticatedId)) {
            return null;
        }

        return clienteRepository.findById(authenticatedId).orElse(null);
    }

    private Map<String, Object> buildClientMap(Cliente cliente) {
        Map<String, Object> clientMap = new HashMap<>();
        clientMap.put("id", cliente.getId());
        clientMap.put("nombre", cliente.getNombre());
        clientMap.put("apellido", cliente.getApellido());
        clientMap.put("numero_cuenta", cliente.getNumeroCuenta());
        clientMap.put("saldo", cliente.getSaldo().toString());
        return clientMap;
    }

    private void allowClientUpdates() {
        entityManager.createNativeQuery("SET @allow_cliente_update = 1").executeUpdate();
    }

    private void disableClientUpdates() {
        entityManager.createNativeQuery("SET @allow_cliente_update = 0").executeUpdate();
    }
}