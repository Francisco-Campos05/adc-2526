package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.UUID;

public class AuthToken {

    public static final long EXPIRATION_TIME = 900; // 15 minutos em segundos

    public String tokenId;
    public String username;
    public String role;
    public long issuedAt;
    public long expiresAt;

    public AuthToken() {}

    public AuthToken(String username, String role) {
        this.username = username;
        this.role = role;
        this.tokenId = UUID.randomUUID().toString();
        this.issuedAt = System.currentTimeMillis() / 1000;
        this.expiresAt = this.issuedAt + EXPIRATION_TIME;
    }
}
