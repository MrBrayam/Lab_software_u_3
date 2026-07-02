package com.example.banca.config;

import com.example.banca.model.Cliente;
import com.example.banca.repository.ClienteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Arrays;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final ClienteRepository clienteRepository;

    public DatabaseSeeder(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (clienteRepository.count() == 0) {
            Cliente c1 = new Cliente("Carlos Alberto", "Mendoza", "009-123456-78", new BigDecimal("120.00"));
            Cliente c2 = new Cliente("Ana María", "Rodríguez", "009-987654-32", new BigDecimal("80.00"));
            Cliente c3 = new Cliente("Luis Fernando", "Torres", "009-555444-33", new BigDecimal("250.00"));
            Cliente c4 = new Cliente("Gabriela Sofía", "Ortiz", "009-999999-99", new BigDecimal("80.00"));

            clienteRepository.saveAll(Arrays.asList(c1, c2, c3, c4));
            System.out.println("Clientes de prueba sembrados correctamente en la base de datos.");
        }
    }
}
