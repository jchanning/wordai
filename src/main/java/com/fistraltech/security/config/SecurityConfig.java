package com.fistraltech.security.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fistraltech.security.service.CustomOAuth2UserService;
import com.fistraltech.security.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * Comma-separated list of origins permitted to make cross-origin requests.
     * Use {@code *} in dev; set to your public domain in production via
     * the {@code WORDAI_CORS_ALLOWED_ORIGINS} environment variable.
     * See {@code docs/features/cors-policy.spec.md}.
     */
    @Value("${wordai.cors.allowed-origins}")
    private String allowedOrigins;

    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService oAuth2UserService;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                         CustomOAuth2UserService oAuth2UserService) {
        this.userDetailsService = userDetailsService;
        this.oAuth2UserService = oAuth2UserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/h2-console/**")
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/", "/index.html", "/login.html", "/help.html",
                               "/privacy", "/terms", "/cookies", "/terms-sale",
                               "/privacy.html", "/terms.html", "/cookies.html", "/terms-sale.html", "/register",
                               "/api/auth/register", "/api/auth/login", "/api/auth/check",
                               "/css/**", "/js/**", "/h2-console/**", "/error", "/static/**").permitAll()
                // Guest access - basic game functionality
                .requestMatchers("/api/wordai/games/**", "/api/wordai/dictionaries/**",
                               "/api/wordai/algorithms").permitAll()
                // Authenticated user endpoints
                .requestMatchers("/api/auth/user", "/api/wordai/stats/**").authenticated()
                // Premium features
                .requestMatchers("/api/wordai/analytics/**", "/api/wordai/export/**").hasAnyRole("PREMIUM", "ADMIN")
                // Admin only endpoints
                .requestMatchers("/api/wordai/admin/**", "/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/api/auth/login")
                .defaultSuccessUrl("/index.html", true)
                .failureUrl("/login.html?error=true")
                .permitAll()
            )
            .oauth2Login(oauth -> oauth
                .loginPage("/login.html")
                .defaultSuccessUrl("/index.html", true)
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuth2UserService)
                )
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/api/auth/logout"))
                .logoutSuccessUrl("/login.html?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable()) // For H2 console
            );

        return http.build();
    }

    /**
     * Centralised CORS policy for all {@code /api/**} endpoints.
     *
     * <p>{@code setAllowedOriginPatterns} is used instead of {@code setAllowedOrigins}
     * because the latter throws an {@code IllegalArgumentException} when combined with
     * {@code allowCredentials=true} and a wildcard value {@code *}.
     * {@code setAllowedOriginPatterns} supports both wildcards and specific origins.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
