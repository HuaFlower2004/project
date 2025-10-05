package com.mi.project.config;

import com.mi.project.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    public WebConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/user/**", "/api/file/**")  // 拦截需要认证的路径
                .excludePathPatterns(
                        "/api/user/register",         // 排除注册
                        "/api/user/login",            // 排除登录
                        "/api/user/check-**",
                        "/api/file/api/file/test",
                        "/api/file/test",
                        "/api/user/emailcode",
                        "/api/user/loginbycode"// 排除检查接口

                );
    }
}