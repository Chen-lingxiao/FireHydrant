package com.example.backend.utils;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {
    // 从配置文件读取密钥和过期时间
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private long expire;

    // 创建加密密钥
    private SecretKey getSigningKey() {
        // 使用HMAC-SHA256算法，需要至少256位(32字节)的密钥
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 生成Token
    public String generateToken(String username) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expire);

        return Jwts.builder()
                .setSubject(username) // 设置用户名作为主题
                .setIssuedAt(now) // 签发时间
                .setExpiration(expirationDate) // 过期时间
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 使用最新的签名方式
                .compact();
    }

    // 从Token中获取用户名
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder() // 使用builder模式构建解析器
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // 验证Token有效性
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 包括过期、签名错误等各种JWT异常
            return false;
        }
    }

    // 检查Token是否过期
    public boolean isTokenExpired(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }
}


