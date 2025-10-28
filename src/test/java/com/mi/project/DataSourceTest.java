
// 文件路径: src/test/java/com/mi/project/DataSourceTest.java
package com.mi.project;

import com.mi.project.config.datasource.DataSourceContextHolder;
import com.mi.project.dto.userDTO.UserRegisterDTO;
import com.mi.project.dto.userDTO.UserUpdateDTO;
import com.mi.project.entity.User;
import com.mi.project.repository.UserRepository;
import com.mi.project.service.IUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 数据源读写分离测试
 * 使用项目实际的IUserService和JPA Repository进行测试
 */
@SpringBootTest
public class DataSourceTest {

    private static final Logger log = LoggerFactory.getLogger(DataSourceTest.class);

    @Autowired
    private IUserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataSource dataSource;

    /**
     * 测试读操作 - 使用JPA Repository查询
     * 应该使用从库(slave1)
     */
    @Test
    public void testReadFromSlaveByJPA() {
        log.info("========== 测试JPA读操作(应使用从库) ==========");

        // 1. 使用JPA的findAll查询所有用户
        List<User> allUsers = userRepository.findAll();
        log.info("查询到 {} 个用户", allUsers.size());

        // 2. 使用JPA的findById查询单个用户
        if (!allUsers.isEmpty()) {
            Long userId = allUsers.get(0).getId();
            Optional<User> userOpt = userRepository.findById(userId);
            userOpt.ifPresent(user ->
                    log.info("通过ID查询到用户: {}, 邮箱: {}", user.getUserName(), user.getEmail())
            );
        }

        // 3. 使用自定义查询方法
        User user = userRepository.findByEmail("test@example.com");
        if (user != null) {
            log.info("通过邮箱查询到用户: {}", user.getUserName());
        } else {
            log.info("未找到邮箱为 test@example.com 的用户");
        }
    }

    /**
     * 测试读操作 - 使用Service层方法
     * 应该使用从库(slave1)
     */
    @Test
    public void testReadFromSlaveByService() {
        log.info("========== 测试Service读操作(应使用从库) ==========");

        // 1. 测试用户名是否可用(读操作)
        boolean available = userService.isUserNameAvailable("testuser123");
        log.info("用户名 testuser123 是否可用: {}", available);

        // 2. 测试邮箱是否可用(读操作)
        boolean emailAvailable = userService.isUserEmailAvailable("test123@example.com");
        log.info("邮箱 test123@example.com 是否可用: {}", emailAvailable);

        // 3. 通过账号查找用户(读操作)
        List<User> users = userRepository.findAll();
        if (!users.isEmpty()) {
            String account = users.get(0).getUserName();
            User foundUser = userService.findUserByAccount(account);
            if (foundUser != null) {
                log.info("通过账号 {} 查找到用户: ID={}", account, foundUser.getId());
            }
        }
    }

    /**
     * 测试写操作 - 用户注册
     * 应该使用主库(master)
     */
    @Test
    public void testWriteToMasterRegister() {
        log.info("========== 测试用户注册(应使用主库) ==========");

        try {
            // 构造注册DTO
            UserRegisterDTO registerDTO = UserRegisterDTO.builder()
                    .userName("datasource_test_" + System.currentTimeMillis())
                    .email("test_" + System.currentTimeMillis() + "@example.com")
                    .phoneNumber("13800138" + (System.currentTimeMillis() % 1000))
                    .password("Test@123456")
                    .confirmPassword("Test@123456")
                    .build();

            // 执行注册 - 这是写操作,应该使用master
            User registeredUser = userService.register(registerDTO);
            log.info("用户注册成功: ID={}, 用户名={}, 邮箱={}",
                    registeredUser.getId(),
                    registeredUser.getUserName(),
                    registeredUser.getEmail());

            // 验证是否真的保存了
            Optional<User> savedUser = userRepository.findById(registeredUser.getId());
            log.info("验证保存结果: {}", savedUser.isPresent() ? "成功" : "失败");

        } catch (Exception e) {
            log.error("注册失败: {}", e.getMessage());
        }
    }

    /**
     * 测试写操作 - 使用JPA直接保存
     * 应该使用主库(master)
     */
    @Test
    public void testWriteToMasterByJPA() {
        log.info("========== 测试JPA写操作(应使用主库) ==========");

        // 直接使用JPA Repository保存用户
        User newUser = User.builder()
                .userName("jpa_test_" + System.currentTimeMillis())
                .email("jpatest_" + System.currentTimeMillis() + "@example.com")
                .password("encoded_password")
                .isActive(true)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("JPA保存用户成功: ID={}, 用户名={}", savedUser.getId(), savedUser.getUserName());
    }

    /**
     * 测试更新操作
     * 应该使用主库(master)
     */
    @Test
    public void testUpdateUser() {
        log.info("========== 测试用户更新(应使用主库) ==========");

        try {
            // 先查询一个用户
            List<User> users = userRepository.findAll();
            if (users.isEmpty()) {
                log.info("没有用户可供测试更新");
                return;
            }

            User user = users.get(0);
            String originalUserName = user.getUserName();

            // 构造更新DTO
            UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                    .email("updated_" + System.currentTimeMillis() + "@example.com")
                    .phoneNumber("13900139" + (System.currentTimeMillis() % 1000))
                    .build();

            // 执行更新
            User updatedUser = userService.updateUserInfo(originalUserName, updateDTO);
            log.info("用户更新成功: 新邮箱={}, 新手机={}",
                    updatedUser.getEmail(),
                    updatedUser.getPhoneNumber());

        } catch (Exception e) {
            log.error("更新失败: {}", e.getMessage());
        }
    }

    /**
     * 测试删除操作
     * 应该使用主库(master)
     */
    @Test
    public void testDeleteUser() {
        log.info("========== 测试用户删除(应使用主库) ==========");

        try {
            // 先创建一个测试用户
            User testUser = User.builder()
                    .userName("delete_test_" + System.currentTimeMillis())
                    .email("deletetest@example.com")
                    .password("password")
                    .isActive(true)
                    .build();

            User saved = userRepository.save(testUser);
            log.info("创建测试用户: ID={}", saved.getId());

            // 删除用户
            boolean deleted = userService.deleteUser(saved.getUserName());
            log.info("删除用户结果: {}", deleted ? "成功" : "失败");

            // 验证是否真的删除了
            Optional<User> checkUser = userRepository.findById(saved.getId());
            log.info("验证删除结果: {}", checkUser.isPresent() ? "用户仍存在(删除失败)" : "用户已删除");

        } catch (Exception e) {
            log.error("删除失败: {}", e.getMessage());
        }
    }

    /**
     * 测试混合操作 - 读写交替
     * 验证数据源自动切换
     */
    @Test
    public void testMixedOperations() {
        log.info("========== 测试混合读写操作 ==========");

        try {
            // 1. 读操作 - 查询所有用户(从库)
            log.info("步骤1: 查询所有用户(应使用从库)");
            List<User> beforeUsers = userRepository.findAll();
            log.info("当前用户总数: {}", beforeUsers.size());

            // 2. 写操作 - 注册新用户(主库)
            log.info("步骤2: 注册新用户(应使用主库)");
            UserRegisterDTO registerDTO = UserRegisterDTO.builder()
                    .userName("mixed_test_" + System.currentTimeMillis())
                    .email("mixed_" + System.currentTimeMillis() + "@example.com")
                    .password("Test@123456")
                    .confirmPassword("Test@123456")
                    .build();

            User newUser = userService.register(registerDTO);
            log.info("新用户注册成功: ID={}", newUser.getId());

            // 3. 读操作 - 检查用户名是否可用(从库)
            log.info("步骤3: 检查用户名可用性(应使用从库)");
            boolean available = userService.isUserNameAvailable("testuser999");
            log.info("用户名 testuser999 可用性: {}", available);

            // 4. 读操作 - 通过账号查找刚注册的用户(从库)
            log.info("步骤4: 查找新注册的用户(应使用从库)");
            User foundUser = userService.findUserByAccount(newUser.getUserName());
            log.info("找到用户: {}", foundUser != null ? foundUser.getUserName() : "null");

            // 5. 写操作 - 更新用户信息(主库)
            log.info("步骤5: 更新用户信息(应使用主库)");
            UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                    .email("updated_mixed@example.com")
                    .build();

            User updated = userService.updateUserInfo(newUser.getUserName(), updateDTO);
            log.info("用户更新成功, 新邮箱: {}", updated.getEmail());

            // 6. 读操作 - 再次查询所有用户(从库)
            log.info("步骤6: 再次查询所有用户(应使用从库)");
            List<User> afterUsers = userRepository.findAll();
            log.info("当前用户总数: {}", afterUsers.size());

            log.info("========== 混合操作测试完成 ==========");

        } catch (Exception e) {
            log.error("混合操作测试失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 测试数据源连接
     */
    @Test
    public void testDataSourceConnection() {
        log.info("========== 测试数据源连接 ==========");

        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            String databaseName = conn.getMetaData().getDatabaseProductName();
            String databaseVersion = conn.getMetaData().getDatabaseProductVersion();

            log.info("数据库连接URL: {}", url);
            log.info("数据库类型: {}", databaseName);
            log.info("数据库版本: {}", databaseVersion);
            log.info("数据库连接成功!");
        } catch (SQLException e) {
            log.error("数据库连接失败", e);
        }
    }

    /**
     * 测试JPA的exists方法(读操作)
     * 应该使用从库
     */
    @Test
    public void testJPAExistsMethods() {
        log.info("========== 测试JPA exists方法(应使用从库) ==========");

        // 测试existsByUserName
        boolean exists = userRepository.existsByUserName("admin");
        log.info("用户名 admin 是否存在: {}", exists);

        // 测试existsByEmail
        boolean emailExists = userRepository.existsByEmail("admin@example.com");
        log.info("邮箱 admin@example.com 是否存在: {}", emailExists);

        // 测试count方法
        long userCount = userRepository.count();
        log.info("用户总数: {}", userCount);
    }
}