package com.mi.project.dto.fileDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.mi.project.entity.File;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileProceessResultDTO {
    private Long fileId;
    private String fileName;
    private String processResult;
    private String errorMessage;
    private LocalDateTime uploadTime;
    private LocalDateTime processStartTime;
    private LocalDateTime processEndTime;
    private Long processingDuration;  // 处理耗时（毫秒）
}
