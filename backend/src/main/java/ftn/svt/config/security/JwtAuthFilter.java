package ftn.svt.config.security;

import ftn.svt.model.UserActivityType;
import ftn.svt.service.UserActivityService;
import ftn.svt.service.UserDetailsServiceImpl;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final UserActivityService userActivityService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        String userIdString = jwtUtils.getSubjectFromToken(token);

        if (userIdString != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UUID userId = UUID.fromString(userIdString);
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                if (jwtUtils.isTokenValid(token, userId)) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    userActivityService.recordActivity(userId, UserActivityType.AUTHENTICATED_REQUEST);
                }
            } catch (JwtException | UsernameNotFoundException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();

                AuthenticationException authException =
                        new BadCredentialsException("Invalid or expired token", ex);
                authenticationEntryPoint.commence(request, response, authException);

            }
        }

        filterChain.doFilter(request, response);
    }
}
