package com.mi.project.service;

import com.mi.project.dto.userDTO.UserRegisterDTO;
import com.mi.project.dto.userDTO.UserUpdateDTO;
import com.mi.project.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author JackBlack
 * @since 2025-07-14
 */
public interface IUserService extends IService<User> {

    User register(UserRegisterDTO userRegisterDTO);

    boolean isUsereNameAvailable(String userName);

    boolean isUserEmailAvailable(String email);

    User findUserByAccount(String account);

    User updateUserInfo(String userName, UserUpdateDTO updateDTO);

    boolean deleteUser(String userName);
}
