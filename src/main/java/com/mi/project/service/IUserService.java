package com.mi.project.service;
import com.mi.project.config.datasource.Master;
import com.mi.project.config.datasource.ReadOnly;
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

    @Master
    User register(UserRegisterDTO userRegisterDTO);

    @ReadOnly
    boolean isUserNameAvailable(String userName);

    @ReadOnly
    boolean isUserEmailAvailable(String email);

    @ReadOnly
    User findUserByAccount(String account);

    @Master
    User updateUserInfo(String userName, UserUpdateDTO updateDTO);

    @Master
    boolean deleteUser(String userName);
}
