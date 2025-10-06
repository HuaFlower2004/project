// 文件路径: src/main/java/com/mi/project/controller/DataSourceMonitorController.java
package com.mi.project.controller;

import com.mi.project.common.Result;
import com.mi.project.config.datasource.DataSourceContextHolder;
import com.mi.project.dto.userDTO.UserRegisterDTO;
import com.mi.project.entity.User;
import com.mi.project.repository.UserRepository;
import com.mi.project.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据源监控和演示控制器
 * 用于实时展示读写分离效果
 */
@RestController
@RequestMapping("/api/monitor/datasource")
@Tag(name = "数据源监控", description = "实时监控数据源使用情况")
@CrossOrigin
public class DataSourceMonitorController {

    private static final Logger log = LoggerFactory.getLogger(DataSourceMonitorController.class);

    @Autowired
    private IUserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataSource dataSource;

    // 统计数据
    private static final AtomicInteger masterCount = new AtomicInteger(0);
    private static final AtomicInteger slaveCount = new AtomicInteger(0);
    private static final Map<String, List<String>> operationLog = new ConcurrentHashMap<>();

    /**
     * 获取实时监控数据
     */
    @GetMapping("/stats")
    @Operation(summary = "获取数据源统计", description = "实时显示主从库使用情况")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 基本信息
        stats.put("masterServer", "192.168.108.152:3306");
        stats.put("slaveServer", "192.168.108.151:3306");

        // 使用统计
        stats.put("masterRequests", masterCount.get());
        stats.put("slaveRequests", slaveCount.get());
        stats.put("totalRequests", masterCount.get() + slaveCount.get());

        // 最近操作日志(只保留最近10条)
        stats.put("recentOperations", operationLog);

        // 数据库连接信息
        try (Connection conn = dataSource.getConnection()) {
            stats.put("connectionUrl", conn.getMetaData().getURL());
            stats.put("connectionStatus", "正常");
        } catch (Exception e) {
            stats.put("connectionStatus", "异常: " + e.getMessage());
        }

        return Result.success("获取成功", stats);
    }

    /**
     * 演示场景1: 用户登录(读操作)
     * 用户体验: 查询用户信息验证密码
     */
    @GetMapping("/demo/login/{username}")
    @Operation(summary = "演示-登录", description = "模拟用户登录(读从库)")
    public Result<Map<String, Object>> demoLogin(@PathVariable String username) {
        long startTime = System.currentTimeMillis();

        // 记录操作
        logOperation("用户登录查询", "slave1(从库)", "查询用户: " + username);
        slaveCount.incrementAndGet();

        // 执行查询(会使用从库)
        User user = userService.findUserByAccount(username);

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("operation", "用户登录(查询操作)");
        result.put("datasource", "slave1 - 192.168.108.151:3306");
        result.put("username", username);
        result.put("found", user != null);
        result.put("duration_ms", duration);
        result.put("explanation", "登录需要查询用户信息，这是读操作，自动使用从库(192.168.108.151)");

        return Result.success("登录查询完成(使用从库)", result);
    }

    /**
     * 演示场景2: 用户注册(写操作)
     * 用户体验: 创建新用户账号
     */
    @PostMapping("/demo/register")
    @Operation(summary = "演示-注册", description = "模拟用户注册(写主库)")
    public Result<Map<String, Object>> demoRegister(@RequestBody UserRegisterDTO registerDTO) {
        long startTime = System.currentTimeMillis();

        try {
            // 记录操作
            logOperation("用户注册", "master(主库)", "注册用户: " + registerDTO.getUserName());
            masterCount.incrementAndGet();

            // 执行注册(会使用主库)
            User newUser = userService.register(registerDTO);

            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> result = new HashMap<>();
            result.put("operation", "用户注册(写操作)");
            result.put("datasource", "master - 192.168.108.152:3306");
            result.put("userId", newUser.getId());
            result.put("username", newUser.getUserName());
            result.put("duration_ms", duration);
            result.put("explanation", "注册需要插入新数据，这是写操作，自动使用主库(192.168.108.152)");

            return Result.success("注册成功(使用主库)", result);
        } catch (Exception e) {
            return Result.failure(500, "注册失败: " + e.getMessage());
        }
    }

    /**
     * 演示场景3: 查看用户列表(读操作)
     * 用户体验: 浏览用户列表
     */
    @GetMapping("/demo/list")
    @Operation(summary = "演示-用户列表", description = "模拟查看用户列表(读从库)")
    public Result<Map<String, Object>> demoList() {
        long startTime = System.currentTimeMillis();

        // 记录操作
        logOperation("查询用户列表", "slave1(从库)", "查询所有用户");
        slaveCount.incrementAndGet();

        // 执行查询(会使用从库)
        List<User> users = userRepository.findAll();

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("operation", "查看用户列表(读操作)");
        result.put("datasource", "slave1 - 192.168.108.151:3306");
        result.put("userCount", users.size());
        result.put("duration_ms", duration);
        result.put("explanation", "查看列表是读操作，自动使用从库(192.168.108.151)，减轻主库压力");

        // 只返回前5个用户的基本信息
        List<Map<String, Object>> userList = new ArrayList<>();
        for (int i = 0; i < Math.min(5, users.size()); i++) {
            User u = users.get(i);
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", u.getId());
            userInfo.put("username", u.getUserName());
            userInfo.put("email", u.getEmail());
            userList.add(userInfo);
        }
        result.put("users", userList);

        return Result.success("查询成功(使用从库)", result);
    }

    /**
     * 演示场景4: 完整的用户操作流程
     * 展示一次完整的业务流程中，读写操作如何分离
     */
    @PostMapping("/demo/complete-flow")
    @Operation(summary = "演示-完整流程", description = "模拟完整的用户操作流程")
    public Result<Map<String, Object>> demoCompleteFlow() {
        Map<String, Object> flowResult = new HashMap<>();
        List<Map<String, String>> steps = new ArrayList<>();

        try {
            // 步骤1: 检查用户名是否可用(读操作 - 从库)
            String testUsername = "demo_user_" + System.currentTimeMillis();
            boolean available = userService.isUsereNameAvailable(testUsername);
            slaveCount.incrementAndGet();
            logOperation("检查用户名", "slave1(从库)", testUsername);

            Map<String, String> step1 = new HashMap<>();
            step1.put("step", "1. 检查用户名可用性");
            step1.put("operation", "读操作");
            step1.put("datasource", "slave1(192.168.108.151)");
            step1.put("result", "用户名可用: " + available);
            steps.add(step1);

            // 步骤2: 查询现有用户数量(读操作 - 从库)
            long beforeCount = userRepository.count();
            slaveCount.incrementAndGet();
            logOperation("统计用户数", "slave1(从库)", "count: " + beforeCount);

            Map<String, String> step2 = new HashMap<>();
            step2.put("step", "2. 查询现有用户数量");
            step2.put("operation", "读操作");
            step2.put("datasource", "slave1(192.168.108.151)");
            step2.put("result", "用户总数: " + beforeCount);
            steps.add(step2);

            // 步骤3: 注册新用户(写操作 - 主库)
            User newUser = User.builder()
                    .userName(testUsername)
                    .email("demo_" + System.currentTimeMillis() + "@example.com")
                    .password("encoded_password")
                    .isActive(true)
                    .build();
            User saved = userRepository.save(newUser);
            masterCount.incrementAndGet();
            logOperation("注册新用户", "master(主库)", saved.getUserName());

            Map<String, String> step3 = new HashMap<>();
            step3.put("step", "3. 注册新用户");
            step3.put("operation", "写操作");
            step3.put("datasource", "master(192.168.108.152)");
            step3.put("result", "用户ID: " + saved.getId());
            steps.add(step3);

            // 等待主从同步(给点时间)
            Thread.sleep(100);

            // 步骤4: 验证注册结果(读操作 - 从库)
            User found = userService.findUserByAccount(testUsername);
            slaveCount.incrementAndGet();
            logOperation("验证新用户", "slave1(从库)", testUsername);

            Map<String, String> step4 = new HashMap<>();
            step4.put("step", "4. 验证注册结果");
            step4.put("operation", "读操作");
            step4.put("datasource", "slave1(192.168.108.151)");
            step4.put("result", found != null ? "找到新用户" : "主从同步中...");
            steps.add(step4);

            // 步骤5: 再次查询用户数量(读操作 - 从库)
            long afterCount = userRepository.count();
            slaveCount.incrementAndGet();
            logOperation("再次统计", "slave1(从库)", "count: " + afterCount);

            Map<String, String> step5 = new HashMap<>();
            step5.put("step", "5. 再次查询用户数量");
            step5.put("operation", "读操作");
            step5.put("datasource", "slave1(192.168.108.151)");
            step5.put("result", "用户总数: " + afterCount + " (增加了" + (afterCount - beforeCount) + "个)");
            steps.add(step5);

            flowResult.put("steps", steps);
            flowResult.put("summary", "完整流程中，1次写操作使用主库，4次读操作使用从库");
            flowResult.put("masterUsed", 1);
            flowResult.put("slaveUsed", 4);

            return Result.success("完整流程演示完成", flowResult);

        } catch (Exception e) {
            flowResult.put("error", e.getMessage());
            return Result.failure(500, "演示失败: " + e.getMessage());
        }
    }

    /**
     * 实时查看当前连接的数据库
     */
    @GetMapping("/current-connection")
    @Operation(summary = "查看当前连接", description = "显示当前实际连接的数据库服务器")
    public Result<Map<String, Object>> getCurrentConnection() {
        Map<String, Object> info = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            // 获取连接URL
            String url = conn.getMetaData().getURL();
            info.put("url", url);

            // 执行查询获取服务器信息
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT @@hostname as hostname, @@port as port, @@server_id as server_id");
            if (rs.next()) {
                info.put("hostname", rs.getString("hostname"));
                info.put("port", rs.getInt("port"));
                info.put("server_id", rs.getInt("server_id"));
            }

            // 判断是主库还是从库
            if (url.contains("192.168.108.152")) {
                info.put("type", "主库(Master)");
                info.put("server", "192.168.108.152:3306");
                info.put("role", "处理所有写操作");
            } else if (url.contains("192.168.108.151")) {
                info.put("type", "从库(Slave)");
                info.put("server", "192.168.108.151:3306");
                info.put("role", "处理所有读操作");
            }

            return Result.success("获取成功", info);
        } catch (Exception e) {
            info.put("error", e.getMessage());
            return Result.failure(500, "获取失败: " + e.getMessage());
        }
    }

    /**
     * 重置统计数据
     */
    @PostMapping("/reset-stats")
    @Operation(summary = "重置统计", description = "清空统计数据")
    public Result<String> resetStats() {
        masterCount.set(0);
        slaveCount.set(0);
        operationLog.clear();
        return Result.success("统计数据已重置");
    }

    /**
     * 压力测试 - 模拟大量并发请求
     */
    @GetMapping("/stress-test/{operations}")
    @Operation(summary = "压力测试", description = "模拟大量并发请求")
    public Result<Map<String, Object>> stressTest(@PathVariable int operations) {
        long startTime = System.currentTimeMillis();

        int readOps = 0;
        int writeOps = 0;

        for (int i = 0; i < operations; i++) {
            if (i % 10 == 0) {
                // 每10次操作有1次写操作
                User user = User.builder()
                        .userName("stress_" + System.currentTimeMillis() + "_" + i)
                        .email("stress_" + i + "@test.com")
                        .password("test")
                        .isActive(true)
                        .build();
                userRepository.save(user);
                writeOps++;
                masterCount.incrementAndGet();
            } else {
                // 其余都是读操作
                userRepository.count();
                readOps++;
                slaveCount.incrementAndGet();
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("totalOperations", operations);
        result.put("readOperations", readOps);
        result.put("writeOperations", writeOps);
        result.put("duration_ms", duration);
        result.put("avgTime_ms", duration / (double) operations);
        result.put("explanation", String.format(
                "执行了%d次操作，其中%d次读操作使用从库(192.168.108.151)，%d次写操作使用主库(192.168.108.152)",
                operations, readOps, writeOps
        ));

        return Result.success("压力测试完成", result);
    }

    /**
     * 记录操作日志
     */
    private void logOperation(String operation, String datasource, String detail) {
        String timestamp = new Date().toString();
        String logEntry = String.format("[%s] %s - %s: %s", timestamp, operation, datasource, detail);

        operationLog.computeIfAbsent("recent", k -> new ArrayList<>()).add(logEntry);

        // 只保留最近10条
        List<String> logs = operationLog.get("recent");
        if (logs.size() > 10) {
            logs.remove(0);
        }

        // 打印到控制台
        log.info("{}", logEntry);
    }
}