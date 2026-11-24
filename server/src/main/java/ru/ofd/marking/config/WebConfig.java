package ru.ofd.marking.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class WebConfig {

    @Bean
    FilterRegistrationBean<OncePerRequestFilter> clientApiKeyFilter(AppProperties props) {
        OncePerRequestFilter filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                String expected = props.getClientApiKey();
                if (expected == null || expected.isBlank()) {
                    filterChain.doFilter(request, response);
                    return;
                }
                if (request.getRequestURI().startsWith("/api/health")
                        || request.getRequestURI().startsWith("/actuator")) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String provided = request.getHeader("X-Api-Key");
                if (expected.equals(provided)) {
                    filterChain.doFilter(request, response);
                } else {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
                }
            }
        };
        FilterRegistrationBean<OncePerRequestFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.addUrlPatterns("/api/*");
        return reg;
    }
}
