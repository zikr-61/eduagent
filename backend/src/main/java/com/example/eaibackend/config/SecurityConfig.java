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
                .cors()  // 启用CORS
                .and()
                .authorizeRequests()
                .requestMatchers("/api/user/register", "/api/user/login").permitAll() // 放行注册和登录接口
                .anyRequest().authenticated(); // 其他请求需要认证
        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.addAllowedOrigin("*"); // 允许所有来源
        configuration.addAllowedMethod("*"); // 允许所有方法
        configuration.addAllowedHeader("*"); // 允许所有头
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
