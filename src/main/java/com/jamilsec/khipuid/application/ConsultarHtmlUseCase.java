package com.jamilsec.khipuid.application;

import com.jamilsec.khipuid.domain.model.*;
import com.jamilsec.khipuid.domain.ports.HtmlConsultaGateway;

public class ConsultarHtmlUseCase {
    private final HtmlConsultaGateway gateway;

    public ConsultarHtmlUseCase(HtmlConsultaGateway gateway) { this.gateway = gateway; }

    /** Recibe un identificador ya válido (Ruc o Dni) y enruta. */
    public HtmlDocumento execute(IdentificadorContribuyente id) {
        if (id instanceof Ruc ruc) return gateway.consultarPorRuc(ruc);
        if (id instanceof Dni dni) return gateway.consultarPorDni(dni);
        throw new IllegalArgumentException("Tipo de identificador no soportado");
    }
}
