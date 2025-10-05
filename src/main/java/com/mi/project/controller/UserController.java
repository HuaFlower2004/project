package com.mi.project.controller;

import com.mi.project.common.Result;
import com.mi.project.dto.userDTO.UserLoginPasswordDTO;
import com.mi.project.dto.userDTO.UserRegisterDTO;
import com.mi.project.dto.userDTO.UserUpdateDTO;
import com.mi.project.entity.User;
import com.mi.project.exception.UserException;
import com.mi.project.repository.UserRepository;
import com.mi.project.service.serviceImpl.UserServiceImpl;
import com.mi.project.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author JackBlack
 * @since 2025-07-14
 */
@Slf4j
@Controller
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户相关接口")
@CrossOrigin(origins = { "http://192.168.93.182:5174",
        "http://192.168.93.182:5173" }, allowCredentials = "true", methods = { RequestMethod.GET, RequestMethod.POST,
                RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS })
public class UserController {

    private final UserServiceImpl userService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // 多级代理时，X-Forwarded-For 可能有多个IP，取第一个
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    private boolean isPasswordLoginAllowed(String accountOrEmail, String ip) {
        // 账号维度
        String keyAccount = "limit:login:fail:" + accountOrEmail;
        Long countAccount = redisTemplate.opsForValue().increment(keyAccount);
        if (countAccount == 1) {
            redisTemplate.expire(keyAccount, 2, TimeUnit.MINUTES);
        }
        // IP维度
        String keyIp = "limit:login:fail:ip:" + ip;
        Long countIp = redisTemplate.opsForValue().increment(keyIp);
        if (countIp == 1) {
            redisTemplate.expire(keyIp, 2, TimeUnit.MINUTES);
        }
        // 账号和IP都不能超限
        return countAccount <= 5 && countIp <= 10;
    }

    // 验证码登录防暴力破解
    private boolean isCodeLoginAllowed(String email, String ip) {
        String keyEmail = "limit:code-login:fail:" + email;
        Long countEmail = redisTemplate.opsForValue().increment(keyEmail);
        if (countEmail == 1) {
            redisTemplate.expire(keyEmail, 2, TimeUnit.MINUTES);
        }
        String keyIp = "limit:code-login:fail:ip:" + ip;
        Long countIp = redisTemplate.opsForValue().increment(keyIp);
        if (countIp == 1) {
            redisTemplate.expire(keyIp, 2, TimeUnit.MINUTES);
        }
        return countEmail <= 5 && countIp <= 10;
    }

    @PostMapping("/emailcode")
    @ResponseBody
    public Result<Void> sendEmailCode(@RequestParam(value = "email") String email) {
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        redisTemplate.opsForValue().set("login:code:" + email, code, 5, TimeUnit.MINUTES);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("3159140248@qq.com"); // 可用配置项替换
        message.setTo(email);
        message.setSubject("登录验证码");
        message.setText("您的验证码是：" + code + "，5分钟内有效。");
        mailSender.send(message);
        return Result.success("验证码已发送", null);
    }

    @PostMapping("/loginbycode")
    @ResponseBody
    public Result<Map<String, Object>> loginByEmailCode(@RequestParam String email, @RequestParam String code, HttpServletRequest request) {
        // 防暴力破解：验证码登录
        String ip = getClientIp(request);

        if (!isCodeLoginAllowed(email,ip)) {
            return Result.failure(429, "验证码错误次数过多，请2分钟后再试");
        }
        // 1. 校验验证码
        String redisKey = "login:code:" + email;
        String realCode = redisTemplate.opsForValue().get(redisKey);
        if (realCode == null || !realCode.equals(code)) {
            return Result.failure(401, "验证码错误或已过期");
        }
        // 2. 校验通过，删除验证码
        redisTemplate.delete(redisKey);

        // 3. 查询/注册用户
        User user = userRepository.findByEmail(email);
        redisTemplate.delete("limit:code-login:fail:" + email);
        redisTemplate.delete("limit:code-login:fail:ip:" + ip);
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setUserName(email.split("@")[0]);
            user.setActive(true);
            userRepository.save(user);
        }

        // 4. 生成Token（可用JWT或自定义）
        String token = jwtUtil.generateToken(user.getUserName(), user.getPassword() != null ? user.getPassword() : "");

        // 5. 返回登录信息
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", token);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 86400L);
        response.put("userInfo", user);
        return Result.success("登录成功", response);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户账号")
    @ResponseBody
    public Result<User> register(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        try {
            if (!userRegisterDTO.isPasswordMatching()) {
                throw new UserException.PasswordNotMatchException();
            }
            User user = userService.register(userRegisterDTO);
            return Result.success(user);
        } catch (Exception e) {
            return Result.failure(500, "服务器错误" + e.getMessage());
        }
    }

    @GetMapping("/check_username")
    @Operation(summary = "检查用户名", description = "检查用户名是否可行用")
    @ResponseBody
    public Result<Boolean> checkUserName(@RequestParam String userName) {
        boolean isAvailable = userService.isUsereNameAvailable(userName);
        return Result.success(isAvailable ? "用户名可用" : "用户名不可用", isAvailable);
    }

    @GetMapping("/check_email")
    @Operation(summary = "检查邮箱", description = "检查邮箱是否可用")
    @ResponseBody
    public Result<Boolean> checkEmail(@RequestBody String email) {
        boolean isAvailable = userService.isUserEmailAvailable(email);
        return Result.success(isAvailable ? "邮箱可用" : "邮箱不可用", isAvailable);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    @ResponseBody
    public Result<Map<String, Object>> login(@Valid @RequestBody UserLoginPasswordDTO loginDTO , HttpServletRequest request) {
        try {
            String ip = getClientIp(request);
            if (!isPasswordLoginAllowed(loginDTO.getAccount(),ip)) {
                return Result.failure(429, "密码错误次数过多，请2分钟后再试");
            }
            User user = userService.findUserByAccount(loginDTO.getAccount());
            // 1. 根据账号查找用户

            if (user == null) {
                return Result.failure(401, "用户不存在");
            }

            // 2. 检查账号状态
            if (!user.isActive()) {
                return Result.failure(401, "账号已被禁用");
            }

            // 3. 验证密码
            if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
                return Result.failure(401, "密码错误");
            }

            // 4. 生成JWT token
            String token = jwtUtil.generateToken(user.getUserName(), user.getPassword());

            // 5. 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);// 保存
            redisTemplate.delete("limit:login:fail:" + loginDTO.getAccount());
            redisTemplate.delete("limit:login:fail:ip:" + ip);
            // 6. 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", token);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", 86400L);
            response.put("userInfo", user); // User对象，password被@JsonIgnore自动隐藏

            return Result.success("登录成功", response);

        } catch (Exception e) {
            log.error("登录异常: {}", e.getMessage());
            return Result.failure(500, "登录失败");
        }
    }

    @GetMapping("/current")
    @Operation(summary = "获取当前用户")
    @ResponseBody
    public Result<User> getCurrentUser(HttpServletRequest request) {
        // 从拦截器中获取当前用户
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser != null) {
            return Result.success("获取成功", currentUser);
        }
        return Result.failure(401, "未登录");
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    @ResponseBody
    public Result<Void> logout() {
        // JWT是无状态的，客户端删除token即可
        return Result.success("登出成功", null);
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户信息", description = "可以更新邮箱、手机号、密码，只传需要更新的字段")
    @ResponseBody
    public Result<User> updateUserInfo(@Valid @RequestBody UserUpdateDTO updateDTO,
            HttpServletRequest request) {
        try {
            // 获取当前用户
            User currentUser = (User) request.getAttribute("currentUser");
            if (currentUser == null) {
                return Result.failure(401, "未登录");
            }

            // 更新用户信息
            User updatedUser = userService.updateUserInfo(currentUser.getUserName(), updateDTO);

            return Result.success("更新成功", updatedUser);

        } catch (Exception e) {
            log.error("更新用户信息失败: {}", e.getMessage());
            return Result.failure(500, "更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除账号")
    @ResponseBody
    public Result<Void> deleteAccount(HttpServletRequest request) {
        try {
            // 获取当前用户
            User currentUser = (User) request.getAttribute("currentUser");
            if (currentUser == null) {
                return Result.failure(401, "未登录");
            }

            // 直接删除用户

            return userService.deleteUser(currentUser.getUserName()) ? Result.success("账号删除成功", null)
                    : Result.failure(500, "账号删除失败");

        } catch (Exception e) {
            log.error("删除账号失败: {}", e.getMessage());
            return Result.failure(500, "删除账号失败: " + e.getMessage());
        }
    }
}
