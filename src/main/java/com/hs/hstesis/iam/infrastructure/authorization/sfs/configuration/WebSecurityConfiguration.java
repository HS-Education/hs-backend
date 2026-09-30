package com.hs.hstesis.iam.infrastructure.authorization.sfs.configuration;

import com.hs.hstesis.iam.infrastructure.authorization.sfs.pipeline.BearerAuthorizationRequestFilter;
import com.hs.hstesis.iam.infrastructure.hashing.bcrypt.BCryptHashingService;
import com.hs.hstesis.iam.infrastructure.tokens.jwt.BearerTokenService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfiguration {
    @org.springframework.beans.factory.annotation.Value("${app.auth.allowed-origins:http://localhost:8080,http://localhost:4200}")
    private List<String> allowedOrigins;
    @org.springframework.beans.factory.annotation.Value("${app.auth.origin-check-enabled:false}")
    private boolean originCheckEnabled;
    @org.springframework.beans.factory.annotation.Value("${app.frontend.enabled:false}")
    private boolean frontendEnabled;

    private final UserDetailsService userDetailsService;
    private final BearerTokenService tokenService;
    private final BCryptHashingService hashingService;
    private final AuthenticationEntryPoint unauthorizedRequestHandler;
    private final com.hs.hstesis.iam.infrastructure.authorization.sfs.pipeline.CustomAccessDeniedHandler customAccessDeniedHandler;

    public WebSecurityConfiguration(@Qualifier("defaultUserDetailsService") UserDetailsService userDetailsService, BearerTokenService tokenService, BCryptHashingService hashingService, AuthenticationEntryPoint authenticationEntryPoint, com.hs.hstesis.iam.infrastructure.authorization.sfs.pipeline.CustomAccessDeniedHandler customAccessDeniedHandler) {
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.hashingService = hashingService;
        this.unauthorizedRequestHandler = authenticationEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Bean
    public BearerAuthorizationRequestFilter authorizationRequestFilter() {
        return new BearerAuthorizationRequestFilter(tokenService, userDetailsService);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(hashingService);
        return authenticationProvider;
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return hashingService;
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository(
            @org.springframework.beans.factory.annotation.Value("${app.auth.cookie-secure:false}") boolean secure) {
        var repository = new CookieCsrfTokenRepository();
        repository.setCookieCustomizer(cookie -> cookie.path("/").httpOnly(true).secure(secure).sameSite("Strict"));
        return repository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CookieCsrfTokenRepository csrfTokenRepository) {
        http.cors(configurer -> configurer.configurationSource(request -> {
            var cors = new CorsConfiguration();
            if (allowedOrigins.isEmpty() || allowedOrigins.contains("*")) {
                throw new IllegalArgumentException("Explicit frontend origins are required");
            }
            cors.setAllowedOrigins(allowedOrigins);
            cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
            cors.setAllowedHeaders(List.of("*"));
            cors.setAllowCredentials(true);
            return cors;
        }));

        http
                // Keep the default XOR handler: bootstrap returns a masked token, not the raw cookie.
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedRequestHandler)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(request -> frontendEnabled && "GET".equals(request.getMethod())
                                && !request.getRequestURI().startsWith("/api/")
                                && !request.getRequestURI().startsWith("/actuator")).permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(authorizationRequestFilter(), UsernamePasswordAuthenticationFilter.class);
        if (originCheckEnabled) {
            http.addFilterBefore(new com.hs.hstesis.iam.infrastructure.authorization.sfs.pipeline.TrustedOriginFilter(
                    new java.util.HashSet<>(allowedOrigins)), UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }
}
