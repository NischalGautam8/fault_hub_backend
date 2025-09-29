package com.leadersfault.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  private static final String AUDIENCE = "custom-jwt";
  private static final long EXPIRATION_MS = 1000 * 60 * 60 * 24 * 5; // 5 days

  private final PrivateKey privateKey;
  private final PublicKey publicKey;
  private final String issuer;

  public JwtUtil(
    @Value("${JWT_PRIVATE_KEY_BASE64}") String privateKeyBase64,
    @Value("${JWT_PUBLIC_KEY_BASE64}") String publicKeyBase64,
    @Value("${SERVER_URL}") String serverUrl
  ) throws Exception {
    this.issuer = serverUrl;
    // Load private key from base64 encoded environment variable
    byte[] pkcs8 = Base64.getDecoder().decode(privateKeyBase64);
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8);
    this.privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);

    // Load public key from base64 encoded environment variable
    byte[] x509 = Base64.getDecoder().decode(publicKeyBase64);
    X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(x509);
    this.publicKey = KeyFactory.getInstance("RSA").generatePublic(pubSpec);
  }

  public String generateToken(String username) {
    return Jwts
      .builder()
      .setSubject(username)
      .setIssuer(issuer)
      .setAudience(AUDIENCE)
      .setIssuedAt(new Date())
      .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
      .signWith(privateKey, SignatureAlgorithm.RS256)
      .compact();
  }

  public Claims validateAndParse(String token) {
    try {
      // Trim any leading/trailing whitespace from token
      token = token.trim();
      return Jwts
        .parserBuilder()
        .setSigningKey(publicKey)
        .build()
        .parseClaimsJws(token)
        .getBody();
    } catch (JwtException e) {
      throw new JwtException("Invalid JWT: " + e.getMessage());
    }
  }

  public <T> T extractClaim(String token, Function<Claims, T> resolver) {
    return resolver.apply(validateAndParse(token));
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  public void validateJwt(String token) {
    try {
      // Trim any leading/trailing whitespace from token
      token = token.trim();
      validateAndParse(token);
    } catch (JwtException e) {
      throw new JwtException("Invalid JWT token: " + e.getMessage(), e);
    }
  }

  public Boolean validateToken(String token, String username) {
    final String extractedUsername = extractUsername(token);
    return (extractedUsername.equals(username) && !isTokenExpired(token));
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }
}
