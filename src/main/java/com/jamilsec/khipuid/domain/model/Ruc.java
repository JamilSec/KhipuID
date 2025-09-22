package com.jamilsec.khipuid.domain.model;

public record Ruc(String value) implements IdentificadorContribuyente {

    public Ruc {
        if (value == null || !value.matches("\\d{11}")) {
            throw new IllegalArgumentException("RUC inválido, debe tener 11 dígitos.");
        }
    }
}
