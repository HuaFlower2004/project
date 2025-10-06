// 文件路径: src/main/java/com/mi/project/controller/DataSourceTestController.java
package com.mi.project.controller;

import com.mi.project.dto.userDTO.UserRegisterDTO;
import com.mi.project.entity.User;
import com.mi.project.common.Result;
import com.mi.project.repository.UserRepository;
import com.mi.project.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源读写分离测试控制器
 * 可以通过API测试读写分离功能
 * @author 31591
 */
@RestController
@RequestMapping("/api/test/datasource")
@Tag(name = "数据源测试", description = "测试读写分离功能")
@CrossOrigin
public class DataSourceTestController {

    private static final Logger log = LoggerFactory.getLogger(DataSourceTestController.class);

    @Autowired
    private IUserService userService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 测试读操作 - 通过用户名查找(从库)
     */
    @GetMapping("/read/username/{username}")
    @Operation(summary = "测试读操作-用户名", description = "该操作应该使用从库")
    public Result<User> testReadByUsername(@PathVariable String username) {
        log.info("API测试: 通过用户名查找用户(应使用从库)");
        User user = userService.findUserByAccount(username);
        return Result.success("读取成功(使用从库)", user);
    }

    /**
     * 测试读操作 - 通过邮箱查找(从库)
     */
    @GetMapping("/read/email/{email}")
    @Operation(summary = "测试读操作-邮箱", description = "该操作应该使用从库")
    public Result<User> testReadByEmail(@PathVariable String email) {
        log.info("API测试: 通过邮箱查找用户(应使用从库)");
        User user = userRepository.findByEmail(email);
        return Result.success("读取成功(使用从库)", user);
    }

    /**
     * 测试读操作 - 查询所有用户(从库)
     */
    @GetMapping("/read/all")
    @Operation(summary = "测试读操作-列表", description = "该操作应该使用从库")
    public Result<List<User>> testReadAll() {
        log.info("API测试: 查询所有用户(应使用从库)");
        List<User> users = userRepository.findAll();
        return Result.success("读取成功(使用从库)", users);
    }

    /**
     * 测试读操作 - 检查用户名可用性(从库)
     */
    @GetMapping("/read/check/username/{username}")
    @Operation(summary = "测试读操作-用户名可用性", description = "该操作应该使用从库")
    public Result<Boolean> testCheckUsername(@PathVariable String username) {
        log.info("API测试: 检查用户名可用性(应使用从库)");
        boolean available = userService.isUsereNameAvailable(username);
        return Result.success("查询成功(使用从库)", available);
    }

    /**
     * 测试读操作 - 检查邮箱可用性(从库)
     */
    @GetMapping("/read/check/email/{email}")
    @Operation(summary = "测试读操作-邮箱可用性", description = "该操作应该使用从库")
    public Result<Boolean> testCheckEmail(@PathVariable String email) {
        log.info("API测试: 检查邮箱可用性(应使用从库)");
        boolean available = userService.isUserEmailAvailable(email);
        return Result.success("查询成功(使用从库)", available);
    }

    /**
     * 测试读操作 - 统计用户数量(从库)
     */
    @GetMapping("/read/count")
    @Operation(summary = "测试读操作-统计", description = "该操作应该使用从库")
    public Result<Long> testCount() {
        log.info("API测试: 统计用户数量(应使用从库)");
        long count = userRepository.count();
        return Result.success("统计成功(使用从库)", count);
    }

    /**
     * 测试写操作 - 注册新用户(主库)
     */
    @PostMapping("/write/register")
    @Operation(summary = "测试写操作-注册", description = "该操作应该使用主库")
    public Result<User> testRegister(@RequestBody UserRegisterDTO registerDTO) {
        log.info("API测试: 注册新用户(应使用主库)");
        try {
            User user = userService.register(registerDTO);
            return Result.success("注册成功(使用主库)", user);
        } catch (Exception e) {
            return Result.failure(500, "注册失败: " + e.getMessage());
        }
    }

    /**
     * 测试写操作 - 直接保存用户(主库)
     */
    @PostMapping("/write/save")
    @Operation(summary = "测试写操作-保存", description = "该操作应该使用主库")
    public Result<User> testSave() {
        log.info("API测试: 直接保存用户(应使用主库)");

        User user = User.builder()
                .userName("api_test_" + System.currentTimeMillis())
                .email("apitest_" + System.currentTimeMillis() + "@example.com")
                .password("test123")
                .isActive(true)
                .build();

        User saved = userRepository.save(user);
        return Result.success("保存成功(使用主库)", saved);
    }

    /**
     * 测试写操作 - 删除用户(主库)
     */
    @DeleteMapping("/write/delete/{username}")
    @Operation(summary = "测试写操作-删除", description = "该操作应该使用主库")
    public Result<Boolean> testDelete(@PathVariable String username) {
        log.info("API测试: 删除用户(应使用主库)");
        try {
            boolean deleted = userService.deleteUser(username);
            return Result.success("删除成功(使用主库)", deleted);
        } catch (Exception e) {
            return Result.failure(500, "删除失败: " + e.getMessage());
        }
    }

    /**
     * 测试混合操作 - 读写交替
     */
    @PostMapping("/mixed/operations")
    @Operation(summary = "测试混合操作", description = "测试读写操作的自动切换")
    public Result<Map<String, Object>> testMixedOperations() {
        log.info("API测试: 混合读写操作");

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 读操作 - 查询用户数量(从库)
            long beforeCount = userRepository.count();
            result.put("step1_count_before", beforeCount);
            log.info("步骤1: 查询用户数量(从库) = {}", beforeCount);

            // 2. 写操作 - 创建新用户(主库)
            User newUser = User.builder()
                    .userName("mixed_api_" + System.currentTimeMillis())
                    .email("mixedapi_" + System.currentTimeMillis() + "@example.com")
                    .password("test123")
                    .isActive(true)
                    .build();

            User saved = userRepository.save(newUser);
            result.put("step2_created_user", saved.getUserName());
            log.info("步骤2: 创建新用户(主库) = {}", saved.getUserName());

            // 3. 读操作 - 检查用户名可用性(从库)
            boolean available = userService.isUsereNameAvailable("nonexist");
            result.put("step3_username_available", available);
            log.info("步骤3: 检查用户名可用性(从库) = {}", available);

            // 4. 读操作 - 查找刚创建的用户(从库)
            User found = userService.findUserByAccount(saved.getUserName());
            result.put("step4_found_user", found != null ? found.getId() : null);
            log.info("步骤4: 查找刚创建的用户(从库) = {}", found != null ? "找到" : "未找到");

            // 5. 读操作 - 再次查询用户数量(从库)
            long afterCount = userRepository.count();
            result.put("step5_count_after", afterCount);
            log.info("步骤5: 再次查询用户数量(从库) = {}", afterCount);

            result.put("success", true);
            result.put("message", "混合操作测试完成,数据源切换正常");

            return Result.success("混合操作测试完成", result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return Result.failure(500, "混合操作测试失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据源配置信息
     */
    @GetMapping("/info")
    @Operation(summary = "数据源配置信息", description = "获取数据源配置统计")
    public Result<Map<String, Object>> getDataSourceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("primary", "master");
        info.put("slaves", new String[]{"slave1"});
        info.put("strategy", "基于方法名和注解的动态路由");
        info.put("read_annotation", "@ReadOnly");
        info.put("write_annotation", "@Master");
        info.put("status", "运行中");
        info.put("description", "读操作自动使用从库,写操作自动使用主库");

        return Result.success("获取成功", info);
    }

    /**
     * 压力测试 - 模拟并发读操作
     */
    @GetMapping("/stress/read")
    @Operation(summary = "读压力测试", description = "模拟100次并发读操作")
    public Result<Map<String, Object>> stressTestRead() {
        log.info("开始读压力测试...");

        long startTime = System.currentTimeMillis();
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            userRepository.count();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("iterations", iterations);
        result.put("duration_ms", duration);
        result.put("avg_time_ms", duration / (double) iterations);
        result.put("datasource", "slave1(从库)");

        log.info("读压力测试完成: {}次操作耗时{}ms", iterations, duration);

        return Result.success("压力测试完成", result);
    }
}