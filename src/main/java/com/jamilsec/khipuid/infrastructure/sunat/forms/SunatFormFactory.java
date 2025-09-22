package com.jamilsec.khipuid.infrastructure.sunat.forms;

import java.util.LinkedHashMap;
import java.util.Map;

public class SunatFormFactory {

    private final String token;
    public SunatFormFactory(String token){ this.token = token; }

    private Map<String,String> base(String accion){
        Map<String,String> p = new LinkedHashMap<>();
        p.put("accion", accion);
        p.put("razSoc", "");
        p.put("token", token);
        p.put("contexto", "ti-it");
        p.put("modo", "1");
        p.put("codigo", "");
        return p;
    }

    public Map<String,String> rucConsulta(String ruc){
        var p = base("consPorRuc");
        p.put("nroRuc", ruc);
        p.put("nrodoc", "");
        p.put("rbtnTipo","1");
        p.put("search1", ruc);
        p.put("tipdoc","1");
        p.put("search2","");
        p.put("search3","");
        return p;
    }

    public Map<String,String> dniPaso1(String dni){
        var p = base("consPorTipdoc");
        p.put("nroRuc","");
        p.put("nrodoc", dni);
        p.put("rbtnTipo","2");
        p.put("tipdoc","1");
        p.put("search1","");
        p.put("search2", dni);
        p.put("search3","");
        return p;
    }

    public Map<String,String> dniPaso2(String ruc, String numRnd){
        var p = base("consPorRuc");
        p.put("actReturn","1");
        p.put("nroRuc", ruc);
        p.put("numRnd", numRnd);
        return p;
    }

    public Map<String,String> repLeg(String ruc, String desRuc) {
        Map<String,String> p = base("getRepLeg");
        p.put("nroRuc", ruc);
        p.put("desRuc", desRuc);
        return p;
    }
}
