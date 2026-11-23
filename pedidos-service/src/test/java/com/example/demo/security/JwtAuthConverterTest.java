package com.example.demo.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica que los claims de Azure AD se traduzcan a permisos de Spring Security. */
class JwtAuthConverterTest {

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("token").header("alg", "RS256").claim("preferred_username", "ana@empresa.cl");
    }

    private static List<String> permisos(AbstractAuthenticationToken auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    void scopesYRolesSeConviertenEnAuthorities() {
        Jwt jwt = token().claim("scp", "Pedidos.Access User.Read").claim("roles", List.of("Admin")).build();

        AbstractAuthenticationToken auth = new JwtAuthConverter().convert(jwt);

        assertEquals("ana@empresa.cl", auth.getName());
        assertTrue(permisos(auth).containsAll(List.of("SCOPE_Pedidos.Access", "SCOPE_User.Read", "ROLE_Admin")));
    }

    @Test
    void tokenSinScopesNiRolesNoTienePermisos() {
        Jwt jwt = token().claim("sub", "123").build();
        assertTrue(new JwtAuthConverter().convert(jwt).getAuthorities().isEmpty());
    }
}
