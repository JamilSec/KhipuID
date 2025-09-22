package com.jamilsec.khipuid.infrastructure.sunat.parsing;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SunatHtmlParser {

    /* =================== Helpers para flujo DNI (lista intermedia) =================== */

    private static final Pattern DATA_RUC = Pattern.compile("data-ruc\\s*=\\s*\"(\\d{11})\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANY_RUC  = Pattern.compile("\\b\\d{11}\\b");
    private static final String HIDDEN_PATTERN_RAW = "name\\s*=\\s*\"%s\"\\s+value\\s*=\\s*\"([^\"]+)\"";

    /** Extrae un RUC de la lista (consPorTipdoc). */
    public String extractRucFromList(String html){
        if (html == null) return null;
        Matcher m1 = DATA_RUC.matcher(html);
        if (m1.find()) return m1.group(1);
        Matcher m2 = ANY_RUC.matcher(html);
        return m2.find() ? m2.group() : null;
    }

    /** Extrae el valor de <input type="hidden" name="X" value="..."> */
    public String extractHidden(String html, String name){
        if (html == null || name == null) return null;
        Pattern p = Pattern.compile(
                String.format(HIDDEN_PATTERN_RAW, Pattern.quote(name)),
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher m = p.matcher(html);
        return m.find() ? m.group(1) : null;
    }

    /* =================== Parser de detalle → DTO =================== */

    public ContribuyenteInfo parseContribuyente(String html) {
        Document doc = Jsoup.parse(html);
        ContribuyenteInfo info = new ContribuyenteInfo();

        info.ruc = textAfterLabel(doc, "Número de RUC:");
        if (info.ruc == null) info.ruc = textAfterLabel(doc, "Numero de RUC:");

        info.razonSocial = extractRazonSocial(info.ruc);
        info.nombreComercial = textAfterLabel(doc, "Nombre Comercial:");
        info.tipoContribuyente = textAfterLabel(doc, "Tipo Contribuyente:");
        info.fechaInscripcion = textAfterLabel(doc, "Fecha de Inscripción:");
        if (info.fechaInscripcion == null) info.fechaInscripcion = textAfterLabel(doc, "Fecha de Inscripcion:");
        info.fechaInicioActividades = textAfterLabel(doc, "Fecha de Inicio de Actividades:");
        info.estado = textAfterLabel(doc, "Estado del Contribuyente:");
        info.condicion = textAfterLabel(doc, "Condición del Contribuyente:");
        if (info.condicion == null) info.condicion = textAfterLabel(doc, "Condicion del Contribuyente:");
        info.domicilioFiscal = textAfterLabel(doc, "Domicilio Fiscal:");
        info.sistemaEmision = textAfterLabel(doc, "Sistema Emisión de Comprobante:");
        if (info.sistemaEmision == null) info.sistemaEmision = textAfterLabel(doc, "Sistema Emision de Comprobante:");
        info.actividadComercioExterior = textAfterLabel(doc, "Actividad Comercio Exterior:");
        info.sistemaContabilidad = textAfterLabel(doc, "Sistema Contabilidad:");

        info.actividadesEconomicas = extractTableTexts(doc, "Actividad(es) Económica(s):");
        info.comprobantesAutorizados = extractTableTexts(doc, "Comprobantes de Pago c/aut. de impresión");
        if (info.comprobantesAutorizados.isEmpty()) {
            info.comprobantesAutorizados = extractTableTexts(doc, "Comprobantes de Pago c/aut. de impresion");
        }
        info.sistemasEmisionElectronica = extractTableTexts(doc, "Sistema de Emisión Electrónica:");
        if (info.sistemasEmisionElectronica.isEmpty()) {
            info.sistemasEmisionElectronica = extractTableTexts(doc, "Sistema de Emision Electronica:");
        }
        info.emisorElectronicoDesde = textAfterLabel(doc, "Emisor electrónico desde:");
        if (info.emisorElectronicoDesde == null) info.emisorElectronicoDesde = textAfterLabel(doc, "Emisor electronico desde:");
        info.comprobantesElectronicos = textAfterLabel(doc, "Comprobantes Electrónicos:");
        if (info.comprobantesElectronicos == null) info.comprobantesElectronicos = textAfterLabel(doc, "Comprobantes Electronicos:");
        info.afiliadoPleDesde = textAfterLabel(doc, "Afiliado al PLE desde:");
        info.padrones = extractTableTexts(doc, "Padrones:");

        // Footer con fecha de consulta
        info.fechaConsulta = doc.select("div.panel-footer small").text();

        return info;
    }

    /* =================== Parser de Representantes Legales =================== */

    /**
     * Parsea la tabla de representantes legales.
     * Estructura esperada de columnas: Documento | Nro. Documento | Nombre | Cargo | Fecha Desde
     */
    public List<Representante> parseRepresentantes(String html) {
        Document doc = Jsoup.parse(html);
        Element table = doc.selectFirst("table.table");
        if (table == null) return List.of();

        List<Representante> reps = new ArrayList<>();
        for (Element tr : table.select("tbody > tr")) {
            var tds = tr.select("td");
            if (tds.size() < 5) continue;

            Representante r = new Representante();
            r.tipoDocumento = safeText(tds.get(0));
            r.numeroDocumento = safeText(tds.get(1));
            r.nombre = safeText(tds.get(2));
            r.cargo = safeText(tds.get(3));
            r.fechaDesde = safeText(tds.get(4));
            reps.add(r);
        }
        return reps;
    }

    private static String safeText(Element td){
        return td == null ? null : td.text().trim();
    }

    /* =================== JSON OUT =================== */

    /** Versión solo datos del contribuyente (snake_case + uppercase + limpieza ruc/fecha). */
    public String toJsonSnakeUpper(ContribuyenteInfo info) throws Exception {
        Map<String,Object> map = baseMapFromInfo(info);
        JsonbConfig config = new JsonbConfig().withFormatting(true);
        try (Jsonb jsonb = JsonbBuilder.create(config)) {
            return jsonb.toJson(map);
        }
    }

    /** Versión extendida con representantes_legales. */
    public String toJsonSnakeUpper(ContribuyenteInfo info, List<Representante> reps) throws Exception {
        Map<String,Object> map = baseMapFromInfo(info);
        map.put("representantes_legales", toRepsSnakeUpper(reps));
        JsonbConfig config = new JsonbConfig().withFormatting(true);
        try (Jsonb jsonb = JsonbBuilder.create(config)) {
            return jsonb.toJson(map);
        }
    }

    /* Construye el mapa base con snake_case + uppercase y normalizaciones. */
    private Map<String,Object> baseMapFromInfo(ContribuyenteInfo info){
        Map<String,Object> map = new LinkedHashMap<>();

        // RUC → solo 11 dígitos
        map.put("ruc", onlyDigits(info.ruc));

        map.put("razon_social", upper(info.razonSocial));
        map.put("nombre_comercial", upper(info.nombreComercial));
        map.put("tipo_contribuyente", upper(info.tipoContribuyente));
        map.put("fecha_inscripcion", upper(info.fechaInscripcion));
        map.put("fecha_inicio_actividades", upper(info.fechaInicioActividades));
        map.put("estado", upper(info.estado));
        map.put("condicion", upper(info.condicion));
        map.put("domicilio_fiscal", upper(info.domicilioFiscal));
        map.put("sistema_emision", upper(info.sistemaEmision));
        map.put("actividad_comercio_exterior", upper(info.actividadComercioExterior));
        map.put("sistema_contabilidad", upper(info.sistemaContabilidad));

        map.put("actividades_economicas", upperList(info.actividadesEconomicas));
        map.put("comprobantes_autorizados", upperList(info.comprobantesAutorizados));
        map.put("sistemas_emision_electronica", upperList(info.sistemasEmisionElectronica));

        map.put("emisor_electronico_desde", upper(info.emisorElectronicoDesde));
        map.put("comprobantes_electronicos", upper(info.comprobantesElectronicos));
        map.put("afiliado_ple_desde", upper(info.afiliadoPleDesde));
        map.put("padrones", upperList(info.padrones));

        // Fecha → limpiar prefijo "Fecha consulta:"
        map.put("fecha_consulta", cleanFechaConsulta(info.fechaConsulta));

        return map;
    }

    private static List<Map<String,Object>> toRepsSnakeUpper(List<Representante> reps) {
        if (reps == null) return List.of();
        List<Map<String,Object>> arr = new ArrayList<>();
        for (Representante r : reps) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("tipo_documento", upper(r.tipoDocumento));
            m.put("nro_documento", upper(r.numeroDocumento));
            m.put("nombre", upper(r.nombre));
            m.put("cargo", upper(r.cargo));
            m.put("fecha_desde", upper(r.fechaDesde));
            arr.add(m);
        }
        return arr;
    }

    /* =================== Helpers internos =================== */

    private static String onlyDigits(String raw) {
        if (raw == null) return null;
        Matcher m = ANY_RUC.matcher(raw);
        return m.find() ? m.group() : raw.replaceAll("\\D+", "");
    }

    private static String cleanFechaConsulta(String raw) {
        if (raw == null) return null;
        return raw.replaceFirst("(?i)fecha consulta:\\s*", "").trim().toUpperCase(Locale.ROOT);
    }

    private static String upper(String val) {
        return val == null ? null : val.toUpperCase(Locale.ROOT).trim();
    }

    private static List<String> upperList(List<String> list) {
        if (list == null) return List.of();
        List<String> out = new ArrayList<>();
        for (String s : list) {
            if (s != null && !s.isBlank()) out.add(s.toUpperCase(Locale.ROOT).trim());
        }
        return out;
    }

    private String textAfterLabel(Document doc, String label) {
        Element e = doc.selectFirst("h4:matchesOwn(" + Pattern.quote(label) + ")");
        if (e != null) {
            Element sibling = e.parent() != null ? e.parent().nextElementSibling() : null;
            if (sibling != null) {
                return sibling.text().trim();
            }
        }
        return null;
    }

    private List<String> extractTableTexts(Document doc, String label) {
        Element e = doc.selectFirst("h4:matchesOwn(" + Pattern.quote(label) + ")");
        if (e != null) {
            Element right = e.parent() != null ? e.parent().nextElementSibling() : null;
            Element table = (right != null) ? right.selectFirst("table") : null;
            if (table != null) {
                List<String> rows = new ArrayList<>();
                for (Element tr : table.select("tr")) {
                    String t = tr.text().trim();
                    if (!t.isBlank()) rows.add(t);
                }
                return rows;
            }
        }
        return List.of();
    }

    private String extractRazonSocial(String rucField) {
        if (rucField == null) return null;
        int idx = rucField.indexOf("-");
        return idx > 0 ? rucField.substring(idx + 1).trim() : null;
    }

    /* =================== DTOs =================== */
    public static class ContribuyenteInfo {
        public String ruc;
        public String razonSocial;
        public String nombreComercial;
        public String tipoContribuyente;
        public String fechaInscripcion;
        public String fechaInicioActividades;
        public String estado;
        public String condicion;
        public String domicilioFiscal;
        public String sistemaEmision;
        public String actividadComercioExterior;
        public String sistemaContabilidad;

        public List<String> actividadesEconomicas;
        public List<String> comprobantesAutorizados;
        public List<String> sistemasEmisionElectronica;
        public String emisorElectronicoDesde;
        public String comprobantesElectronicos;
        public String afiliadoPleDesde;
        public List<String> padrones;

        public String fechaConsulta;
    }

    public static class Representante {
        public String tipoDocumento;
        public String numeroDocumento;
        public String nombre;
        public String cargo;
        public String fechaDesde;
    }
}
