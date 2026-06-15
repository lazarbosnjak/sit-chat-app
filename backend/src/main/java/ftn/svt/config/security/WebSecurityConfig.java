package ftn.svt.config.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtUtils jwtUtils;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authenticationProvider(authenticationProvider(userDetailsService))
                .sessionManagement(sessionCfg -> sessionCfg.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");

                            response.getWriter().write("""
                                            {
                                              "timestamp": "%s",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "%s",
                                              "path": "%s"
                                            }
                                            """.formatted(
                                            Instant.now(),
                                            e.getMessage(),
                                            request.getRequestURI()
                                    )
                            );
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");

                            response.getWriter().write("""
                                            {
                                              "timestamp": "%s",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "%s",
                                              "path": "%s"
                                            }
                                            """.formatted(
                                            Instant.now(),
                                            e.getMessage(),
                                            request.getRequestURI()
                                    )
                            );
                        })
                )
                .authorizeHttpRequests(matcherRegistry -> matcherRegistry
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/ws").permitAll()
                        .requestMatchers("/api/v0/auth/**").permitAll()
                        .requestMatchers("/api/v0/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthFilter(jwtUtils, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class
                );


        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider daoAuthProvider = new DaoAuthenticationProvider(userDetailsService);
        daoAuthProvider.setPasswordEncoder(passwordEncoder());
        return daoAuthProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:4200", "https://kvt.lazarb.dev"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
