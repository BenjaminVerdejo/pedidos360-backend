package com.example.demo;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/test")
    public Map<String, Object> testEndpoint(Authentication auth) {
        Jwt jwt = ((JwtAuthenticationToken) auth).getToken();
        List<String> authorities = auth.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        return Map.of(
                "mensaje", "Conexión exitosa, el backend validó tu token de Azure AD",
                "usuario", String.valueOf(jwt.getClaimAsString("preferred_username")),
                "authorities", authorities
        );
    }
}
