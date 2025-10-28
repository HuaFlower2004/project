package com.mi.project.aspect;

import com.mi.project.annotation.Cacheable;
import com.mi.project.service.ICacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 缓存切面
 * 实现自定义缓存注解的功能
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CacheAspect {

    private final ICacheService cacheService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(cacheable)")
    public Object around(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // 生成缓存键
        String cacheKey = generateCacheKey(cacheable, method, args);
        
        // 检查缓存条件
        if (!evaluateCondition(cacheable.condition(), method, args)) {
            return joinPoint.proceed();
        }

        try {
            // 尝试从缓存获取
            Object result = cacheService.getCache(cacheKey, Object.class);
            if (result != null) {
                log.debug("缓存命中: key={}", cacheKey);
                
                // 记录热点数据访问
                if (cacheable.enableHotDataTracking()) {
                    cacheService.recordDataAccess(cacheKey, cacheable.dataType());
                }
                
                return result;
            }

            // 缓存未命中，执行方法
            log.debug("缓存未命中: key={}", cacheKey);
            result = joinPoint.proceed();

            // 检查是否应该缓存结果
            if (result != null && evaluateCondition(cacheable.unless(), method, args, result)) {
                // 设置缓存
                long ttlSeconds = TimeUnit.SECONDS.convert(cacheable.ttl(), cacheable.timeUnit());
                cacheService.setCache(cacheKey, result, ttlSeconds);
                
                log.debug("设置缓存: key={}, ttl={}s", cacheKey, ttlSeconds);
            }

            return result;

        } catch (Exception e) {
            log.error("缓存操作异常: key={}", cacheKey, e);
            // 缓存异常不影响业务逻辑
            return joinPoint.proceed();
        }
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(Cacheable cacheable, Method method, Object[] args) {
        String key = cacheable.key();
        
        if (!StringUtils.hasText(key)) {
            // 如果没有指定key，使用类名+方法名+参数
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(method.getDeclaringClass().getSimpleName())
                     .append(":")
                     .append(method.getName());
            
            if (args != null && args.length > 0) {
                keyBuilder.append(":");
                for (Object arg : args) {
                    keyBuilder.append(arg != null ? arg.toString() : "null").append(":");
                }
            }
            key = keyBuilder.toString();
        } else {
            // 解析SpEL表达式
            key = parseSpelExpression(key, method, args);
        }
        
        return key;
    }

    /**
     * 解析SpEL表达式
     */
    private String parseSpelExpression(String expression, Method method, Object[] args) {
        try {
            Expression exp = parser.parseExpression(expression);
            EvaluationContext context = new StandardEvaluationContext();
            
            // 设置方法参数
            String[] paramNames = nameDiscoverer.getParameterNames(method);
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            
            Object value = exp.getValue(context);
            return value != null ? value.toString() : expression;
        } catch (Exception e) {
            log.warn("SpEL表达式解析失败: {}", expression, e);
            return expression;
        }
    }

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String condition, Method method, Object[] args) {
        return evaluateCondition(condition, method, args, null);
    }

    /**
     * 评估条件表达式（支持返回值）
     */
    private boolean evaluateCondition(String condition, Method method, Object[] args, Object result) {
        if (!StringUtils.hasText(condition)) {
            return true;
        }

        try {
            Expression exp = parser.parseExpression(condition);
            EvaluationContext context = new StandardEvaluationContext();
            
            // 设置方法参数
            String[] paramNames = nameDiscoverer.getParameterNames(method);
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            
            // 设置返回值
            if (result != null) {
                context.setVariable("result", result);
            }
            
            Boolean value = exp.getValue(context, Boolean.class);
            return value != null && value;
        } catch (Exception e) {
            log.warn("条件表达式评估失败: {}", condition, e);
            return true;
        }
    }
}

