package com.mi.project.service.serviceImpl;

import com.mi.project.config.datasource.Master;
import com.mi.project.config.datasource.ReadOnly;
import com.mi.project.dto.userDTO.UserRegisterDTO;
import com.mi.project.dto.userDTO.UserUpdateDTO;
import com.mi.project.entity.User;
import com.mi.project.exception.UserException;
import com.mi.project.mapper.UserMapper;
import com.mi.project.repository.UserRepository;
import com.mi.project.service.IUserService;
import com.mi.project.service.ICacheService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author JackBlack
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ICacheService cacheService;

    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    @Master
    public User register(UserRegisterDTO userRegisterDTO){

        if(userRepository.existsByUserName(userRegisterDTO.getUserName())){
            throw new UserException.UserNAmeExistsException();
        }

        if (StringUtils.hasText(userRegisterDTO.getEmail())&&userRepository.existsByEmail(userRegisterDTO.getEmail())){
            throw new UserException.EmailExistsException();
        }

        if(StringUtils.hasText(userRegisterDTO.getPhoneNumber())&&userRepository.existsByPhoneNumber(userRegisterDTO.getPhoneNumber())){
            throw new UserException.PhoneExistsException();
        }
        User user = User.builder()
                .userName(userRegisterDTO.getUserName())
                .password(passwordEncoder.encode(userRegisterDTO.getPassword() != null ? userRegisterDTO.getPassword() : "123456"))
                .email(userRegisterDTO.getEmail())
                .phoneNumber(userRegisterDTO.getPhoneNumber())
                .isActive(true)
                .createdTime(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    @ReadOnly
    @Override
    @com.mi.project.annotation.Cacheable(key = "'user:available:' + #userName", ttl = 300, dataType = "user")
    public boolean isUserNameAvailable(String userName) {
        return !userRepository.existsByUserName(userName);
    }

    @ReadOnly
    @Override
    @com.mi.project.annotation.Cacheable(key = "'email:available:' + #email", ttl = 300, dataType = "user")
    public boolean isUserEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @ReadOnly
    @Override
    @com.mi.project.annotation.Cacheable(key = "'user:account:' + #account", ttl = 1800, dataType = "user")
    public User findUserByAccount(String account) {
        // 支持用户名或邮箱查找
        if (account.contains("@")) {
            return userRepository.findByEmail(account);
        }
        else if(account.matches("^(?=.*[a-zA-Z])[a-zA-Z0-9_]+$")) {
            return userRepository.findByUserName(account);
        }
        else{
            return  userRepository.findByPhoneNumber(account);
        }
    }

    @Master
    @Override
    @Transactional
    public User updateUserInfo(String userName, UserUpdateDTO updateDTO) {
        User user = null;
        // 1. 查找用户
        if(userRepository.existsByUserName(userName)){
            user = userRepository.findByUserName(userName);
        }else{
            throw new RuntimeException("用户不存在");
        }
        String originalEmail = user.getEmail();
        String originalPhone = user.getPhoneNumber();
        // 2. 更新邮箱（如果提供了新邮箱）
        if (StringUtils.hasText(updateDTO.getEmail())) {
            // 检查邮箱是否被其他用户使用
            if(updateDTO.getEmail().equals(user.getEmail())){
                throw new RuntimeException("邮箱未改动");
            }
            else if (userRepository.existsByEmail(updateDTO.getEmail())) {
                throw new RuntimeException("邮箱已被其他用户使用");
            }
            user.setEmail(updateDTO.getEmail());
        }
        // 3. 更新手机号（如果提供了新手机号）
        if (StringUtils.hasText(updateDTO.getPhoneNumber())) {
            // 检查手机号是否被其他用户使用
            if(updateDTO.getPhoneNumber().equals(user.getPhoneNumber())){
                throw new RuntimeException("手机号未改动");
            }
            else if (userRepository.existsByPhoneNumber(updateDTO.getPhoneNumber())) {
                throw new RuntimeException("手机号已被其他用户使用");
            }
            user.setPhoneNumber(updateDTO.getPhoneNumber());
        }
        // 4. 更新密码（如果提供了新密码）
        if (StringUtils.hasText(updateDTO.getNewPassword())) {
            // 检查是否提供了当前密码
            if (!StringUtils.hasText(updateDTO.getCurrentPassword())) {
                throw new RuntimeException("修改密码需要提供当前密码");
            }
            // 验证当前密码
            if (!passwordEncoder.matches(updateDTO.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("当前密码错误");
            }
            // 更新密码
            user.setPassword(passwordEncoder.encode(updateDTO.getNewPassword()));
        }
        // 5. 保存更新
        User updatedUser = userRepository.save(user);

        // 6. 清除相关缓存，确保一致性
        clearUserCache(userName, originalEmail, originalPhone);
        // 如果更改了邮箱/手机号，也清理新键以避免短期并发脏读
        if (StringUtils.hasText(updateDTO.getEmail())) {
            cacheService.deleteCache("user:account:" + updateDTO.getEmail());
            cacheService.deleteCache("email:available:" + updateDTO.getEmail());
        }
        if (StringUtils.hasText(updateDTO.getPhoneNumber())) {
            cacheService.deleteCache("user:account:" + updateDTO.getPhoneNumber());
        }

        return updatedUser;
    }

    @Master
    @Override
    @Transactional
    public boolean deleteUser(String userName) {
        // 直接删除用户
        if (userRepository.existsByUserName(userName)) {
            userRepository.deleteByUserName(userName);
            return true;
        }
        return false;
    }

    /**
     * 清除用户相关缓存键
     */
    private void clearUserCache(String userName, String email, String phoneNumber) {
        // 用户信息缓存
        cacheService.deleteCache("user:account:" + userName);
        if (StringUtils.hasText(email)) {
            cacheService.deleteCache("user:account:" + email);
        }
        if (StringUtils.hasText(phoneNumber)) {
            cacheService.deleteCache("user:account:" + phoneNumber);
        }
        // 可用性缓存
        cacheService.deleteCache("user:available:" + userName);
        if (StringUtils.hasText(email)) {
            cacheService.deleteCache("email:available:" + email);
        }
    }
}
