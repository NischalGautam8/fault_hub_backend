package com.leadersfault.controller;

import com.leadersfault.security.JwtUtil;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OidcController {

  @Value("${SERVER_URL:http://localhost:8080}")
  private String serverUrl;

  private final JwtUtil jwtUtil;

  public OidcController(JwtUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
  }

  @GetMapping("/.well-known/openid-configuration")
  public Map<String, Object> configuration() {
    return Map.of(
      "issuer",
      serverUrl,
      "jwks_uri",
      serverUrl + "/.well-known/jwks.json"
    );
  }

  @GetMapping("/.well-known/jwks.json")
  public Map<String, Object> jwks() {
    RSAPublicKey publicKey = (RSAPublicKey) jwtUtil.getPublicKey();

    // Remove leading zero byte if present for proper base64url encoding
    byte[] modulus = publicKey.getModulus().toByteArray();
    if (modulus[0] == 0) {
      byte[] temp = new byte[modulus.length - 1];
      System.arraycopy(modulus, 1, temp, 0, temp.length);
      modulus = temp;
    }

    String n = Base64.getUrlEncoder().withoutPadding().encodeToString(modulus);
    String e = Base64
      .getUrlEncoder()
      .withoutPadding()
      .encodeToString(publicKey.getPublicExponent().toByteArray());

    return Map.of(
      "keys",
      new Object[] {
        Map.of(
          "kty",
          "RSA",
          "use",
          "sig",
          "alg",
          "RS256",
          "kid",
          "1",
          "n",
          n,
          "e",
          e
        ),
      }
    );
  }
}
