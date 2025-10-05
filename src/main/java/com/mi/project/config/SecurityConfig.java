package com.mi.project.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.mi.project.util.JwtUtil;
import org.springframework.beans.factory.annotation.Qualifier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 配置CORS - 必须在最前面
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 完全禁用CSRF，特别是对API
                .csrf(AbstractHttpConfigurer::disable)

                // 配置请求授权
                .authorizeHttpRequests(requests -> requests
                        // OPTIONS请求（CORS预检）完全放行
                        .requestMatchers("OPTIONS", "/**").permitAll()
                        .requestMatchers("/ws").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/test-ws/**").permitAll()
                        // 公开的API端点
                        .requestMatchers(
                                "/api/user/register",
                                "/api/user/login",
                                "/api/user/loginbycode",
                                "/api/user/emailcode"
                        )
                        .permitAll()
                        // 静态资源和公共页面
                        .requestMatchers("/", "/home", "/login", "/logout", "/error").permitAll()
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        // 调试端点
                        .requestMatchers("/debug/**", "/health").permitAll()
                        // 需要特定角色的端点
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**", "/api/user/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/file/**").hasAnyRole("USER", "ADMIN")
                        // 其他请求需要认证
                        .anyRequest().authenticated())

                // 配置表单登录（仅用于页面）
                .formLogin(AbstractHttpConfigurer::disable)

                // 配置登出
                .logout(AbstractHttpConfigurer::disable)

                // 配置会话管理为无状态
                .sessionManagement(session -> session.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS))

                // 关键：配置异常处理，区分API和页面请求
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            String requestUri = request.getRequestURI();
                            String acceptHeader = request.getHeader("Accept");

                            // 对于API请求或AJAX请求，返回JSON错误
                            if (requestUri.startsWith("/api/") ||
                                    (acceptHeader != null && acceptHeader.contains("application/json")) ||
                                    "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {

                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json;charset=UTF-8");
                                response.setHeader("Access-Control-Allow-Origin", getOrigin(request));
                                response.setHeader("Access-Control-Allow-Credentials", "true");
                                response.getWriter().write(
                                        "{\"error\":\"Unauthorized\",\"message\":\"Authentication required\",\"code\":401}");
                            } else {
                                // 对于页面请求，重定向到登录页
                                response.sendRedirect("/login");
                            }
                        }));
        // 集成JWT认证过滤器
        http.addFilterBefore(new com.mi.project.config.JwtAuthenticationFilter(jwtUtil),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 获取请求来源，用于CORS
    private String getOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        return Objects.requireNonNullElse(origin, "*");
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的源（更宽泛的配置）
        configuration.setAllowedOriginPatterns(List.of("*"));

        // 允许所有HTTP方法
        configuration.setAllowedMethods(List.of("*"));
        // 允许所有请求头
        configuration.setAllowedHeaders(List.of("*"));
        // 允许发送认证信息
        configuration.setAllowCredentials(true);
        // 预检请求的缓存时间
        configuration.setMaxAge(3600L);

        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList(
                "Access-Control-Allow-Headers",
                "Access-Control-Allow-Methods",
                "Access-Control-Allow-Origin",
                "Access-Control-Max-Age",
                "X-Frame-Options",
                "Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径生效
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}