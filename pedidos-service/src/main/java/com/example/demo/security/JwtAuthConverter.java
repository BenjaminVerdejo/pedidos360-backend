package com.example.demo.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Lee los claims de Azure AD:
 *  - "scp" (scopes, string separado por espacios) -> SCOPE_xxx
 *  - "roles" (app roles, lista)                   -> ROLE_xxx
 */
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        String scp = jwt.getClaimAsString("scp");
        if (scp != null && !scp.isBlank()) {
            for (String s : scp.split(" ")) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
            }
        }

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            for (String r : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
            }
        }

        return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("preferred_username"));
    }
}
