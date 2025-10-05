package com.mi.project.dto.userDTO;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户更新请求")
public class UserUpdateDTO {
    @Schema(description = "新邮箱", example = "john@example.com")
    @Email(message = "邮箱格式不正确")
    @Size(max = 32, message = "新邮箱长度不能超过32个字符")
    private String email;

    @Schema(description = "新手机号", example = "13999999999")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNumber;

    @Schema(description = "当前密码（修改密码时必填）")
    private String currentPassword;

    @Schema(description = "新密码")
    @Size(min = 6, max = 32, message = "新密码长度应在6-32个字符之间")
    private String newPassword;
}
