package com.example.banca.dto;

import java.math.BigDecimal;

public class TransferRequest {
    private Long cliente_origen_id;
    private String cuenta_destino;
    private BigDecimal monto;
    private String nombre_destinatario;

    public TransferRequest() {}

    // Getters and Setters (with snake_case style to match the existing JS fetch request variables)
    public Long getCliente_origen_id() {
        return cliente_origen_id;
    }

    public void setCliente_origen_id(Long cliente_origen_id) {
        this.cliente_origen_id = cliente_origen_id;
    }

    public String getCuenta_destino() {
        return cuenta_destino;
    }

    public void setCuenta_destino(String cuenta_destino) {
        this.cuenta_destino = cuenta_destino;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getNombre_destinatario() {
        return nombre_destinatario;
    }

    public void setNombre_destinatario(String nombre_destinatario) {
        this.nombre_destinatario = nombre_destinatario;
    }
}
