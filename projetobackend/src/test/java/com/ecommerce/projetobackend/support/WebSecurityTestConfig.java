package com.ecommerce.projetobackend.support;

import com.ecommerce.projetobackend.ratelimit.RateLimitProperties;
import com.ecommerce.projetobackend.ratelimit.RateLimitResult;
import com.ecommerce.projetobackend.ratelimit.RateLimiter;
import com.ecommerce.projetobackend.security.JwtAuthenticationEntryPoint;
import com.ecommerce.projetobackend.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Duration;

/**
 * Fornece os beans de segurança que os slices @WebMvcTest não montam sozinhos.
 *
 * O @Import(SecurityConfig.class) carrega a cadeia de filtros real (JwtAuthFilter,
 * RateLimitFilter, entry point). Como o slice web não inclui @Service/@Component
 * comuns, estes beans precisam ser fornecidos manualmente para o contexto subir.
 * Rate limiting fica desligado (enabled=false) para não interferir nos testes.
 */
@TestConfiguration
public class WebSecurityTestConfig {

    @Bean
    JwtService jwtService() {
        return Mockito.mock(JwtService.class);
    }

    @Bean
    UserDetailsService userDetailsService() {
        return Mockito.mock(UserDetailsService.class);
    }

    @Bean
    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return Mockito.mock(JwtAuthenticationEntryPoint.class);
    }

    @Bean
    RateLimiter rateLimiter() {
        RateLimiter rateLimiter = Mockito.mock(RateLimiter.class);
        // Sempre permite: garante pass-through do filtro mesmo se alguma regra casar.
        Mockito.when(rateLimiter.consume(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyInt(),
                        ArgumentMatchers.any(Duration.class)))
                .thenReturn(new RateLimitResult(true, 100, 99, Long.MAX_VALUE));
        return rateLimiter;
    }

    @Bean
    RateLimitProperties rateLimitProperties() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(false);
        return properties;
    }

    /**
     * O RateLimitFilter (e os testes via @Autowired) usam o ObjectMapper do Jackson 2,
     * que não é auto-configurado no slice porque o app padroniza no Jackson 3.
     */
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
