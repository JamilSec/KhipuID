package com.jamilsec.khipuid.domain.ports;

import com.jamilsec.khipuid.domain.model.*;

public interface HtmlConsultaGateway {
    HtmlDocumento consultarPorRuc(Ruc ruc);
    HtmlDocumento consultarPorDni(Dni dni);
}
