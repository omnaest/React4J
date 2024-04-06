package org.omnaest.react4j.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfiguration
{
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        return http.authorizeHttpRequests(authorize -> authorize.anyRequest()
                                                                .permitAll())
                   .cors(CorsConfigurer::disable)
                   .csrf(CsrfConfigurer::disable)
                   //                   .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                   .build();
    }

    @Bean
    WebSecurityCustomizer webSecurityCustomizer()
    {
        return web -> web.debug(false);
    }
}
