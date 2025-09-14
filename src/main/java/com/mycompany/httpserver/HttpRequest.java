/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.httpserver;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author luisdanielbenavidesnavarro
 */
public class HttpRequest {
    
    private URI requri;
    private Map<String,String> query = null;

    HttpRequest(URI requri) {
        this.requri = requri;
        this.query = parseQuery(requri.getRawQuery());
    }
    
    public String getValue(String paramName) {
        return query.get(paramName);
    }
        private static Map<String,String> parseQuery(String raw) {
        Map<String,String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            String k = i >= 0 ? pair.substring(0,i) : pair;
            String v = i >= 0 ? pair.substring(i+1) : "";
            map.put(urlDecode(k), urlDecode(v));
        }
        return map;
    }

    private static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

}
