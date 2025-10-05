package com.mi.project.dto.fileDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "上传文件请求")
@Builder
public class FileUploadDTO {
    @Schema(description = "上传的文件",requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile file;

    @Schema(description = "操作者名称",requiredMode = Schema.RequiredMode.REQUIRED)
    private String userName;

    @Schema(description = "文件描述/备注")
    private String description;

    @Schema(description = "处理参数")
//    private final String postParams = "--classification 16 0";
    private String postParams;
}
