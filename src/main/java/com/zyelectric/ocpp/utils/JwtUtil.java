package com.zyelectric.ocpp.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    private final String SECRET_KEY = "840fc9e6a2f28c8be9710a53fdb94c8af551d7ae60560515dc4d6b768e7ab26c2356ec99d089b2404479fe54bf28aeb11fac706f7e6a7669ead83bb71a3029efd6979bf3732ac99389c2d86d32543945d3b158bf5bccbd0df7d561b870c2cb94d14a15f4490c4e387936f4e30fb21f15454fd6480e313ece4da4486ed9fedaf3b842ef59c483827c94bda77e2bb12e526b43b98163d76cf274d589246d44503b1e16b7651a0fa68a0e2e8839113a28e5b08e3399dee0bd6ca4e1aa7354eaa36d22c3012fb057c656d77187edbd2837846d1442a6b0214f3c0bb440852c8b1ce1094e5b0492a5299aa5f743acb20950abc0b2ba5118a7fdd391fdde792234d04a"; // 32+ chars for HS256
    private final long EXPIRATION_TIME = 1000 * 60 * 60; // 1 hour

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
