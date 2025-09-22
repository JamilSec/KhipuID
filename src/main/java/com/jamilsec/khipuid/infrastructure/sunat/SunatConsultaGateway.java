package com.jamilsec.khipuid.infrastructure.sunat;

import com.jamilsec.khipuid.domain.model.*;
import com.jamilsec.khipuid.domain.ports.HtmlConsultaGateway;
import com.jamilsec.khipuid.infrastructure.sunat.errors.SunatConnectionException;
import com.jamilsec.khipuid.infrastructure.sunat.errors.SunatHttpException;
import com.jamilsec.khipuid.infrastructure.sunat.errors.SunatParsingException;
import com.jamilsec.khipuid.infrastructure.sunat.forms.SunatFormFactory;
import com.jamilsec.khipuid.infrastructure.sunat.http.SunatEndpoints;
import com.jamilsec.khipuid.infrastructure.sunat.http.SunatHttpClient;
import com.jamilsec.khipuid.infrastructure.sunat.http.SunatSessionFactory;
import com.jamilsec.khipuid.infrastructure.sunat.parsing.SunatHtmlParser;
import com.jamilsec.khipuid.infrastructure.sunat.token.TokenProvider;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

public class SunatConsultaGateway implements HtmlConsultaGateway {

    private final SunatSessionFactory sessions = new SunatSessionFactory();
    private final SunatHtmlParser parser = new SunatHtmlParser();
    private final SunatFormFactory forms;

    public SunatConsultaGateway(TokenProvider tokenProvider) {
        this.forms = new SunatFormFactory(tokenProvider.get());
    }

    @Override
    public HtmlDocumento consultarPorRuc(Ruc ruc) {
        try {
            HttpClient c = sessions.newClient();

            var r1 = c.send(SunatHttpClient.requestGet(SunatEndpoints.URL_CRITERIOS),
                    HttpResponse.BodyHandlers.ofString());
            ensure200(r1, "GET bootstrap");

            var r2 = c.send(SunatHttpClient.requestPostForm(
                            SunatEndpoints.URL_JCR,
                            forms.rucConsulta(ruc.value())),
                    HttpResponse.BodyHandlers.ofString());
            ensure200(r2, "POST consPorRuc");

            return new HtmlDocumento(r2.body());

        } catch (IOException e) {
            throw new SunatConnectionException("Error de conexión con SUNAT (RUC)", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SunatConnectionException("Consulta interrumpida (RUC)", e);
        }
    }

    @Override
    public HtmlDocumento consultarPorDni(Dni dni) {
        try {
            HttpClient c = sessions.newClient();

            var r1 = c.send(SunatHttpClient.requestGet(SunatEndpoints.URL_CRITERIOS),
                    HttpResponse.BodyHandlers.ofString());
            ensure200(r1, "GET bootstrap");

            var r2 = c.send(SunatHttpClient.requestPostForm(
                            SunatEndpoints.URL_JCR,
                            forms.dniPaso1(dni.value())),
                    HttpResponse.BodyHandlers.ofString());
            ensure200(r2, "POST consPorTipdoc");

            String ruc = parser.extractRucFromList(r2.body());
            if (ruc == null) {
                throw new SunatParsingException("No se encontró RUC en respuesta consPorTipdoc");
            }

            String numRnd = parser.extractHidden(r2.body(), "numRnd");
            if (numRnd == null) {
                throw new SunatParsingException("No se encontró numRnd en respuesta consPorTipdoc");
            }

            var r3 = c.send(SunatHttpClient.requestPostForm(
                            SunatEndpoints.URL_JCR,
                            forms.dniPaso2(ruc, numRnd)),
                    HttpResponse.BodyHandlers.ofString());
            ensure200(r3, "POST consPorRuc (desde DNI)");

            return new HtmlDocumento(r3.body());

        } catch (IOException e) {
            throw new SunatConnectionException("Error de conexión con SUNAT (DNI)", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SunatConnectionException("Consulta interrumpida (DNI)", e);
        }
    }

    public HtmlDocumento consultarRepresentantes(Ruc ruc, String desRuc) {
        try {
            HttpClient c = sessions.newClient();
            // Bootstrap para cookie
            var r1 = c.send(SunatHttpClient.requestGet(SunatEndpoints.URL_CRITERIOS),
                    HttpResponse.BodyHandlers.ofString());
            ensure200(r1, "GET bootstrap");

            var r2 = c.send(SunatHttpClient.requestPostForm(
                            SunatEndpoints.URL_JCR,
                            forms.repLeg(ruc.value(), desRuc)),
                    HttpResponse.BodyHandlers.ofString());
            ensure200(r2, "POST getRepLeg");
            return new HtmlDocumento(r2.body());

        } catch (IOException e) {
            throw new SunatConnectionException("Error de conexión con SUNAT (Rep. legales)", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SunatConnectionException("Consulta interrumpida (Rep. legales)", e);
        }
    }

    /** Si la respuesta no es 200, lanza SunatHttpException */
    private static void ensure200(HttpResponse<?> r, String label) {
        if (r.statusCode() != 200) {
            throw new SunatHttpException(label + " respondió HTTP " + r.statusCode(), r.statusCode());
        }
    }
}