package com.example.banca.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SaldoProtectionInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SaldoProtectionInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_clientes_protect_saldo");
        jdbcTemplate.execute("""
                CREATE TRIGGER trg_clientes_protect_saldo
                BEFORE UPDATE ON clientes
                FOR EACH ROW
                BEGIN
                    IF COALESCE(@allow_cliente_update, 0) <> 1 THEN
                        SIGNAL SQLSTATE '45000'
                            SET MESSAGE_TEXT = 'Los datos del cliente solo pueden modificarse desde una sesión autenticada';
                    END IF;
                END
                """);
    }
}