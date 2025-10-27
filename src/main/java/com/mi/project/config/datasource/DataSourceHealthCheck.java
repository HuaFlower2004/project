package com.mi.project.config.datasource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源健康检查
 * 监控主从数据库的连接状态
 */
@Slf4j
@Component
public class DataSourceHealthCheck implements HealthIndicator {

    @Autowired
    private com.baomidou.dynamic.datasource.DynamicRoutingDataSource dynamicDataSource;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean allHealthy = true;

        try {
            // 检查主库
            DataSource masterDataSource = dynamicDataSource.getDataSource("master");
            if (masterDataSource != null) {
                boolean masterHealthy = checkDataSource(masterDataSource, "master");
                details.put("master", masterHealthy ? "UP" : "DOWN");
                if (!masterHealthy) allHealthy = false;
            }

            // 检查从库1
            DataSource slave1DataSource = dynamicDataSource.getDataSource("slave1");
            if (slave1DataSource != null) {
                boolean slave1Healthy = checkDataSource(slave1DataSource, "slave1");
                details.put("slave1", slave1Healthy ? "UP" : "DOWN");
                if (!slave1Healthy) allHealthy = false;
            }

            // 检查从库2
            DataSource slave2DataSource = dynamicDataSource.getDataSource("slave2");
            if (slave2DataSource != null) {
                boolean slave2Healthy = checkDataSource(slave2DataSource, "slave2");
                details.put("slave2", slave2Healthy ? "UP" : "DOWN");
                if (!slave2Healthy) allHealthy = false;
            }

        } catch (Exception e) {
            log.error("数据源健康检查失败", e);
            allHealthy = false;
            details.put("error", e.getMessage());
        }

        return allHealthy ? Health.up().withDetails(details).build() 
                          : Health.down().withDetails(details).build();
    }

    /**
     * 检查单个数据源的健康状态
     */
    private boolean checkDataSource(DataSource dataSource, String dataSourceName) {
        try (Connection connection = dataSource.getConnection()) {
            // 执行简单的查询测试连接
            connection.createStatement().execute("SELECT 1");
            log.debug("数据源 {} 健康检查通过", dataSourceName);
            return true;
        } catch (SQLException e) {
            log.error("数据源 {} 健康检查失败: {}", dataSourceName, e.getMessage());
            return false;
        }
    }
}


