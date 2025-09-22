package com.jamilsec.khipuid.runner;

import com.jamilsec.khipuid.application.ConsultarHtmlUseCase;
import com.jamilsec.khipuid.domain.model.*;
import com.jamilsec.khipuid.infrastructure.sunat.SunatConsultaGateway;
import com.jamilsec.khipuid.infrastructure.sunat.forms.SunatFormFactory;
import com.jamilsec.khipuid.infrastructure.sunat.http.SunatEndpoints;
import com.jamilsec.khipuid.infrastructure.sunat.http.SunatHttpClient;
import com.jamilsec.khipuid.infrastructure.sunat.http.SunatSessionFactory;
import com.jamilsec.khipuid.infrastructure.sunat.parsing.SunatHtmlParser;
import com.jamilsec.khipuid.infrastructure.sunat.token.StaticTokenProvider;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;

public class ConsoleRunner {
    public static void main(String[] args) {
        // 1) Token estático (ideal: cargar de configuración/ENV)
        var tokenProvider = new StaticTokenProvider(
                "93cae35nzpbbfs3by6z6jjcsugchtngry9jhc2j9uh70916jbpp1"
        );

        // 2) Gateway + UseCase
        var gateway = new SunatConsultaGateway(tokenProvider);
        var useCase = new ConsultarHtmlUseCase(gateway);

        // 3) Input (desde args o valor por defecto)
        String input = (args.length > 0) ? args[0] : "20132272418"; // RUC de ejemplo
        IdentificadorContribuyente id = parseId(input);

        // 4) Ejecutar caso de uso → obtener HTML detalle
        HtmlDocumento html = useCase.execute(id);

        // 5) Parsear HTML → DTO
        var parser = new SunatHtmlParser();
        SunatHtmlParser.ContribuyenteInfo info = parser.parseContribuyente(html.contenido());

        try {
            if (id instanceof Ruc ruc) {
                // 6) Extra: traer representantes legales
                var sessions = new SunatSessionFactory();
                HttpClient c = sessions.newClient();

                var forms = new SunatFormFactory(tokenProvider.get());
                var req = SunatHttpClient.requestPostForm(
                        SunatEndpoints.URL_JCR,
                        forms.repLeg(ruc.value(), info.razonSocial)
                );

                HttpResponse<String> rReps = c.send(req, HttpResponse.BodyHandlers.ofString());
                List<SunatHtmlParser.Representante> reps = parser.parseRepresentantes(rReps.body());

                // 7) Serializar todo a JSON extendido
                String json = parser.toJsonSnakeUpper(info, reps);
                System.out.println(json);
            } else {
                // Solo DNI → no hay reps
                String json = parser.toJsonSnakeUpper(info);
                System.out.println(json);
            }
        } catch (Exception e) {
            System.err.println("Error procesando: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Convierte string a Ruc o Dni según longitud. */
    private static IdentificadorContribuyente parseId(String raw) {
        if (raw == null) throw new IllegalArgumentException("Identificador nulo");
        String digits = raw.replaceAll("\\D+", "");
        if (digits.length() == 11) return new Ruc(digits);  // validación en record
        if (digits.length() == 8)  return new Dni(digits);  // validación en record
        throw new IllegalArgumentException("Debe ser DNI (8) o RUC (11) dígitos");
    }
}
