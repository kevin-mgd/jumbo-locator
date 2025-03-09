package nl.jumbo.assignement.jumbolocator.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.web.cors.CorsConfiguration

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Disable CSRF for our REST API
            .csrf { it.disable() }

            .cors { cors ->
                cors.configurationSource { _ ->
                    CorsConfiguration().apply {
                        // For production, use specific allowed origins instead of "*"
                        allowedOrigins = listOf("*")
                        allowedMethods = listOf("GET", "POST", "OPTIONS")
                        allowedHeaders = listOf("Authorization", "Content-Type")
                        exposedHeaders = listOf("X-Rate-Limit-Remaining")
                        maxAge = 3600L
                    }
                }
            }

            // Permit access to Swagger UI and API for everyone
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        AntPathRequestMatcher("/v3/api-docs/**"),
                        AntPathRequestMatcher("/swagger-ui/**"),
                        AntPathRequestMatcher("/swagger-ui.html"),
                        AntPathRequestMatcher("/api/v1/stores/**"),
                        AntPathRequestMatcher("/actuator/health"),
                        AntPathRequestMatcher("/actuator/info")
                    ).permitAll()
                    .anyRequest().authenticated()
            }

            .headers { headers ->
                headers.frameOptions { it.deny() }

                headers.contentSecurityPolicy { csp ->
                    csp.policyDirectives("default-src 'self'; script-src 'self'; img-src 'self'; style-src 'self'; connect-src 'self'")
                }

                headers.httpStrictTransportSecurity { hsts ->
                    hsts.includeSubDomains(true)
                    hsts.maxAgeInSeconds(31536000)  // 1 year
                }
            }

            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

        return http.build()
    }
}
