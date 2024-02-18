package org.omnaest.react4j.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfiguration
{

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        return http.authorizeHttpRequests(authorize -> authorize.anyRequest()
                                                                .permitAll())
                   .cors(cors -> cors.disable())
                   .build();
    }
}
