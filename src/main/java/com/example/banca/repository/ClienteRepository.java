package com.example.banca.repository;

import com.example.banca.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByNumeroCuenta(String numeroCuenta);
    Optional<Cliente> findByNumeroCuentaAndPin(String numeroCuenta, String pin);
}
