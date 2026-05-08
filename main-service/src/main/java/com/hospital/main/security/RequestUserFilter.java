package com.hospital.main.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestUserFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        try {
            String role = request.getHeader("X-User-Role");
            String userId = request.getHeader("X-User-Id");
            if (role != null && userId != null) {
                RequestUserHolder.set(new RequestUser(Long.parseLong(userId), role));
            }
            filterChain.doFilter(request, response);
        } finally {
            RequestUserHolder.clear();
        }
    }
}
