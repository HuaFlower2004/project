package com.mi.project.dto.userDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "用户登录请求")
public class UserLoginPasswordDTO {

    @Schema(description = "账户",requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账户不能为空")
    @Length(min = 6, max = 25, message = "账户长度必须在6-20个字符之间")
    private String account;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "Password123!")
    @NotBlank(message = "密码不能为空")
    @Length(min = 8, max = 32, message = "密码长度必须在8-32个字符之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,32}$",
            message = "密码必须包含大小写字母、数字和特殊字符")
    private String password;

    @Schema(description = "记住我", example = "true")
    private boolean rememberMe = false;
}
