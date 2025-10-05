package com.mi.project.interceptor;

import com.mi.project.entity.User;
import com.mi.project.repository.UserRepository;
import com.mi.project.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    // 公开访问的端点，不需要JWT验证
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/api/user/register",
            "/api/user/login",
            "/api/user/check-",
            "/error"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String origin = request.getHeader("Origin");

        log.info("JwtInterceptor 拦截到请求: {} {}", method, requestURI);
        log.info("请求来源Origin: [{}]", origin);

        // ========== 1. 优先处理CORS相关 ==========

        // 设置CORS响应头（无论什么请求都设置，确保前端能收到响应）
        setCorsHeaders(request, response);

        // 对于OPTIONS预检请求，直接返回成功，不进行JWT验证
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.info("OPTIONS预检请求，直接放行");
            response.setStatus(HttpServletResponse.SC_OK);
            return true;  // 直接返回，不继续后续处理
        }

        // ========== 2. 检查是否是公开端点 ==========

        if (isPublicEndpoint(requestURI)) {
            log.info("访问公开端点，无需JWT验证: {}", requestURI);
            return true;
        }

        // ========== 3. JWT Token验证逻辑 ==========

        String authHeader = request.getHeader("Authorization");
        log.info("Authorization头: [{}]", authHeader);

        // 检查Authorization头格式
        if (!hasValidAuthHeader(authHeader)) {
            log.error("缺少Authorization头或格式错误，URI: {}", requestURI);
            return handleUnauthorized(request, response, "缺少有效的Authorization头");
        }

        // 提取token
        String token = authHeader.substring(7);  // 移除"Bearer "前缀
        log.debug("提取的token: {}...", token.length() > 10 ? token.substring(0, 10) : token);

        try {
            // 验证token
            if (!jwtUtil.validateToken(token)) {
                log.error("Token验证失败，URI: {}", requestURI);
                return handleUnauthorized(request, response, "Token无效或已过期");
            }

            // 从token获取用户名
            String username = jwtUtil.getUserNameFromToken(token);
            log.info("从token中获取的用户名: {}", username);

            if (!StringUtils.hasText(username)) {
                log.error("无法从token中获取用户名");
                return handleUnauthorized(request, response, "Token中缺少用户信息");
            }

            // 查找用户
            User user = userRepository.findByUserName(username);
            if (user == null) {
                log.error("用户不存在: {}", username);
                return handleUnauthorized(request, response, "用户不存在");
            }

            // 设置用户信息到request中，供Controller使用
            request.setAttribute("currentUser", user);
            request.setAttribute("currentUserId", user.getId());
            request.setAttribute("currentUserName", username);

            log.info("用户验证成功，设置currentUser: {} (ID: {})", username, user.getId());
            return true;

        } catch (Exception e) {
            log.error("Token处理异常，URI: {}, 错误: {}", requestURI, e.getMessage(), e);
            return handleUnauthorized(request, response, "Token处理异常: " + e.getMessage());
        }
    }

    /**
     * 设置CORS响应头
     */
    private void setCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");

        // 如果有Origin头，回显允许该源（在生产环境中应该检查白名单）
        if (StringUtils.hasText(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }

        // 设置其他CORS头
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        response.setHeader("Access-Control-Allow-Headers",
                "Origin, X-Requested-With, Content-Type, Accept, Authorization, Cache-Control, Pragma");
        response.setHeader("Access-Control-Expose-Headers",
                "Authorization, Content-Length, X-Kuma-Revision");
        response.setHeader("Access-Control-Max-Age", "3600");
    }

    /**
     * 检查是否是公开端点
     */
    private boolean isPublicEndpoint(String requestURI) {
        for (String endpoint : PUBLIC_ENDPOINTS) {
            if (requestURI.startsWith(endpoint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查Authorization头是否有效
     */
    private boolean hasValidAuthHeader(String authHeader) {
        return StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ") && authHeader.length() > 7;
    }

    /**
     * 处理未授权请求的统一方法
     */
    private boolean handleUnauthorized(HttpServletRequest request, HttpServletResponse response, String message) throws Exception {

        // 确保设置了CORS头（重要：让前端能收到错误响应）
        setCorsHeaders(request, response);

        String requestURI = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        // 判断是否是API请求或AJAX请求
        boolean isApiRequest = requestURI.startsWith("/api/") ||
                (acceptHeader != null && acceptHeader.contains("application/json")) ||
                "XMLHttpRequest".equals(requestedWith);

        if (isApiRequest) {
            // API请求返回JSON格式错误
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            String jsonError = String.format(
                    "{\"code\":401,\"message\":\"%s\",\"success\":false,\"timestamp\":\"%s\",\"path\":\"%s\"}",
                    message,
                    java.time.Instant.now().toString(),
                    requestURI
            );
            response.getWriter().write(jsonError);
            log.info("返回API错误响应: {}", message);
        } else {
            // 页面请求重定向到登录页
            log.info("页面请求重定向到登录页");
            response.sendRedirect("/login?error=unauthorized");
        }

        return false;
    }
    /**
     * 从请求中提取JWT Token（备用方法，支持多种方式）
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 1. 从Authorization头获取
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // 2. 从请求参数获取（备用方式）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }
        // 3. 从Cookie获取（如果你的应用使用Cookie存储JWT）
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("JWT_TOKEN".equals(cookie.getName()) || "access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理ThreadLocal变量（如果有的话）
        String requestURI = request.getRequestURI();
        log.debug("请求完成: {}", requestURI);
    }
}