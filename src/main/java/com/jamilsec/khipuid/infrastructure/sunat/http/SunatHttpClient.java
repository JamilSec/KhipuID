package com.jamilsec.khipuid.infrastructure.sunat.http;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;

import static com.jamilsec.khipuid.infrastructure.sunat.http.SunatEndpoints.UA;

public class SunatHttpClient {
    private final SunatSessionFactory sessionFactory = new SunatSessionFactory();

    /* ---------- Versión de instancia (crea su propio HttpClient efímero) ---------- */

    public HttpResponse<String> get(String url) throws IOException, InterruptedException {
        var client = sessionFactory.newClient();
        var req = requestGet(url);
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> postForm(String url, Map<String, String> form)
            throws IOException, InterruptedException {
        var client = sessionFactory.newClient();
        var req = requestPostForm(url, form);
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /* ---------- Builders ESTÁTICOS (devuelven HttpRequest; tú pones el HttpClient) ---------- */

    /** Crea un HttpRequest GET con UA y timeout estándar. */
    public static HttpRequest requestGet(String url) {
        return HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .header("User-Agent", UA)
                .timeout(Duration.ofSeconds(15))
                .build();
    }

    /** Crea un HttpRequest POST x-www-form-urlencoded con UA y timeout estándar. */
    public static HttpRequest requestPostForm(String url, Map<String, String> form) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", UA)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(25))
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(form)))
                .build();
    }

    /* ---------- Util ---------- */

    public static String formEncode(Map<String, String> params) {
        var sj = new StringJoiner("&");
        for (var e : params.entrySet()) {
            sj.add(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                    URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sj.toString();
    }
}
