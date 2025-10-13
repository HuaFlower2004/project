// 文件路径: src/main/java/com/mi/project/config/datasource/DataSourceAspect.java
package com.mi.project.config.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 数据源切换AOP
 * 根据注解自动切换数据源
 */
@Aspect
@Component
@Order(1) // 确保在事务之前执行
public class DataSourceAspect {

    private static final Logger log = LoggerFactory.getLogger(DataSourceAspect.class);

    /**
     * 定义切点: Service层的所有方法
     */
    @Pointcut("execution(* com.mi.project.service..*.*(..))")
    public void servicePointcut() {}

    /**
     * 环绕通知,根据注解切换数据源
     */
    @Around("servicePointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        String dataSource = determineDataSource(method, targetClass);

        try {
            // 设置数据源
            DataSourceContextHolder.setDataSource(dataSource);
            log.debug("方法 [{}] 使用数据源: {}", method.getName(), dataSource);

            // 执行方法
            return joinPoint.proceed();
        } finally {
            // 清除数据源设置
            DataSourceContextHolder.clearDataSource();
        }
    }

    /**
     * 确定使用哪个数据源
     * 优先级: 方法注解 > 类注解 > 方法名判断 > 默认master
     */
    private String determineDataSource(Method method, Class<?> targetClass) {
        // 1. 检查方法上的@DS注解
        if (method.isAnnotationPresent(DS.class)) {
            return method.getAnnotation(DS.class).value();
        }

        // 2. 检查方法上的@ReadOnly注解
        if (method.isAnnotationPresent(ReadOnly.class)) {
            return getSlaveDataSource(); // 使用负载均衡选择从库
        }

        // 3. 检查方法上的@Master注解
        if (method.isAnnotationPresent(Master.class)) {
            return "master";
        }

        // 4. 检查类上的@DS注解
        if (targetClass.isAnnotationPresent(DS.class)) {
            return targetClass.getAnnotation(DS.class).value();
        }

        // 5. 检查类上的@ReadOnly注解
        if (targetClass.isAnnotationPresent(ReadOnly.class)) {
            return getSlaveDataSource(); // 使用负载均衡选择从库
        }

        // 6. 根据方法名判断
        String methodName = method.getName();
        if (methodName.startsWith("get") ||
                methodName.startsWith("find") ||
                methodName.startsWith("list") ||
                methodName.startsWith("query") ||
                methodName.startsWith("select") ||
                methodName.startsWith("count") ||
                methodName.startsWith("search")) {
            return getSlaveDataSource(); // 读操作使用从库负载均衡
        }

        // 7. 默认使用主库
        return "master";
    }

    /**
     * 从库负载均衡选择
     * 简单的轮询策略
     */
    private String getSlaveDataSource() {
        // 简单的轮询策略，实际项目中可以使用更复杂的负载均衡算法
        long currentTime = System.currentTimeMillis();
        int slaveIndex = (int) (currentTime % 2) + 1; // 在slave1和slave2之间轮询
        return "slave" + slaveIndex;
    }
}