package com.jamilsec.khipuid.domain.model;

public record Dni(String value) implements IdentificadorContribuyente {

    public Dni {
        if (value == null || !value.matches("\\d{8}")) {
            throw new IllegalArgumentException("DNI inválido, debe tener 8 dígitos.");
        }
    }
}
