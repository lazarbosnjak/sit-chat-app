package ftn.svt.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String SECRET;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUsernameFromToken(String token) {
        try {
            return getClaimsFromToken(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Date getExpirationDateFromToken(String token) {
        try {
            return getClaimsFromToken(token).getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        Date expDate = getExpirationDateFromToken(token);
        Instant now = Instant.now(Clock.systemUTC());
        return expDate != null && expDate.toInstant().isBefore(now);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
       String username = getUsernameFromToken(token);
       return username != null
               && username.equals(userDetails.getUsername())
               && !isTokenExpired(token);
    }

    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now(Clock.systemUTC());
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("role", userDetails.getAuthorities().toArray()[0])
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiration)))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }
}
