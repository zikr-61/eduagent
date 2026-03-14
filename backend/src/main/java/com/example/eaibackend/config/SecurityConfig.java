package com.example.eaibackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()  // 禁用CSRF
                .authorizeRequests()
                .requestMatchers("/api/user/register", "/api/user/login").permitAll() // 放行注册和登录接口
                .anyRequest().authenticated(); // 其他请求需要认证
        return http.build();
    }
}
