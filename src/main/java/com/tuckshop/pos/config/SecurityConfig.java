package com.tuckshop.pos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // enables @PreAuthorize on service/controller methods
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Cashiers who land on an owner-only page (e.g. by clicking an old bookmark)
    // get sent back to the dashboard with a plain-language explanation instead of
    // Spring's default "Whitelabel Error Page", which looks broken and confusing.
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {
            if (request.getRequestURI().startsWith("/api/")) {
                response.setStatus(403);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"You don't have permission to do that. Ask the owner.\"}");
            } else {
                response.sendRedirect("/?denied=1");
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/login").permitAll()
                .requestMatchers("/users", "/api/users/**").hasRole("OWNER")
                .requestMatchers("/reports", "/api/reports/**").hasRole("OWNER")
                .requestMatchers("/backups", "/api/backups/**").hasRole("OWNER")
                .requestMatchers("/license", "/api/license/**").hasRole("OWNER")
                .requestMatchers("/sale-edits", "/api/sale-edits/**").hasRole("OWNER")
                .requestMatchers("/h2-console/**").hasRole("OWNER")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler()))
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?loggedout")
                .permitAll()
            )
            // This app runs only on the shop's local network, never exposed to the internet,
            // and all AJAX calls come from our own JS - so CSRF tokens aren't needed here.
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        return http.build();
    }
}
