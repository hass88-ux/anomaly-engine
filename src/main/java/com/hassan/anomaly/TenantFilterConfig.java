package com.hassan.anomaly;

import java.io.IOException;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Enables the Hibernate tenant filter for the duration of each authenticated request.
 *
 * Without this, isolation depends on every repository method remembering to include
 * the username in its query. With it, the predicate is appended by Hibernate to every
 * query touching a filtered entity, whether the method asked for it or not.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class TenantFilterConfig extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilterConfig.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String username = currentUsername();

        if (username != null) {
            try {
                Session session = entityManager.unwrap(Session.class);
                session.enableFilter("tenantFilter")
                       .setParameter("tenantUsername", username);
            } catch (Exception e) {
                log.warn("Could not enable tenant filter for {}", username, e);
            }
        }

        chain.doFilter(request, response);
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof String s && !"anonymousUser".equals(s)) {
            return s;
        }

        return null;
    }
}