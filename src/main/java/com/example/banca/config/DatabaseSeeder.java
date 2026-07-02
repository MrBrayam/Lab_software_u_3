package com.example.banca.config;

import com.example.banca.model.Cliente;
import com.example.banca.repository.ClienteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Map<String, String> DEFAULT_PINS = new HashMap<>();

    static {
        DEFAULT_PINS.put("009-123456-78", "1234");
        DEFAULT_PINS.put("009-987654-32", "2345");
        DEFAULT_PINS.put("009-555444-33", "3456");
        DEFAULT_PINS.put("009-999999-99", "4567");
    }

    private final ClienteRepository clienteRepository;

    public DatabaseSeeder(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (clienteRepository.count() == 0) {
            Cliente c1 = new Cliente("Carlos Alberto", "Mendoza", "009-123456-78", "1234", new BigDecimal("120.00"));
            Cliente c2 = new Cliente("Ana María", "Rodríguez", "009-987654-32", "2345", new BigDecimal("80.00"));
            Cliente c3 = new Cliente("Luis Fernando", "Torres", "009-555444-33", "3456", new BigDecimal("250.00"));
            Cliente c4 = new Cliente("Gabriela Sofía", "Ortiz", "009-999999-99", "4567", new BigDecimal("80.00"));

            clienteRepository.saveAll(Arrays.asList(c1, c2, c3, c4));
            System.out.println("Clientes de prueba sembrados correctamente en la base de datos.");
        } else {
            clienteRepository.findAll().forEach(cliente -> {
                if (cliente.getPin() == null || cliente.getPin().isBlank()) {
                    String pin = DEFAULT_PINS.get(cliente.getNumeroCuenta());
                    if (pin != null) {
                        cliente.setPin(pin);
                        clienteRepository.save(cliente);
                    }
                }
            });
        }
    }
}
