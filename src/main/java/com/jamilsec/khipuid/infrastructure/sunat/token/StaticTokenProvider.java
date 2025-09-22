package com.jamilsec.khipuid.infrastructure.sunat.token;

public class StaticTokenProvider implements TokenProvider {
    private final String token;
    public StaticTokenProvider(String token) { this.token = token; }
    @Override public String get() { return token; }
}