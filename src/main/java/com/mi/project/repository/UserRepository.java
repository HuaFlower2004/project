package com.mi.project.repository;

import com.mi.project.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * @author 31591
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUserName(String userName);

    boolean existsByEmail(@Email(message = "邮箱格式不正确") @Size(max = 100, message = "邮箱长度不能超过100个字符") String email);

    boolean existsByPhoneNumber(@Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phoneNumber);

    User findByUserName(@NotBlank(message = "用户名不能为空") @Length(min = 6, max = 20, message = "用户名长度必须在6-20个字符之间") @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线") String userName);

    User findByEmail(String account);

    User findByPhoneNumber(String phoneNumber);

    void deleteByUserName(String userName);
}
