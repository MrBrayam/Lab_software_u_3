package com.example.banca.repository;

import com.example.banca.model.Transferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {
    List<Transferencia> findByClienteOrigenIdOrderByFechaDesc(Long clienteOrigenId);
}
