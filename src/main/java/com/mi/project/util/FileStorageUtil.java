package com.mi.project.util;

import com.mi.project.common.MyWebSocketHandler;
import com.mi.project.entity.File;
import com.mi.project.util.ZipLasExtractorUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class FileStorageUtil {
    @Resource
    CloudUploadUtil cloudUploadUtil;
    private static final String storagePath = new File().getPreFilePath();
    private final long maxFileSize = 1024*1024*1024;
    public List<String> storeFile(MultipartFile file, String userName) {
        List<String> result = new ArrayList<>();
        try {
            // 验证文件
            validateFile(file);
            // 创建存储目录
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String userPath = "user_" + userName;
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename).toLowerCase();
            String storedFileName = UUID.randomUUID() + extension;

            Path uploadDir = Paths.get(storagePath, userPath, datePath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            String relativePath = userPath + "/" + datePath + "/" + storedFileName;
            Path targetPath = uploadDir.resolve(storedFileName);
            String finalLasPath;
            String fileUrl = null;
            if (extension.equals(".zip")) {
                // 1. 保存zip到目标目录
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                // 2. 创建临时解压目录
                Path tempUnzipDir = Files.createTempDirectory("unzip_las_");
                // 3. 解压出第一个las文件
                Path lasPath = ZipLasExtractorUtil.extractFirstLasFileFromZip(targetPath, tempUnzipDir);
                if (lasPath == null) {
                    throw new IllegalArgumentException("zip包中未找到las文件");
                }
                finalLasPath = lasPath.toString();
                // 4. 可选：上传原始zip到云端
                fileUrl = cloudUploadUtil.cloudStorage(file, relativePath + storedFileName);
                // 6. 可选：将las文件复制到正式存储目录
                Path finalLasTarget = uploadDir.resolve(UUID.randomUUID() + ".las");
                Files.copy(lasPath, finalLasTarget, StandardCopyOption.REPLACE_EXISTING);
                // 7. 结果信息
                result.add(relativePath);
                result.add(storedFileName);
                result.add(extension);
                result.add(fileUrl);
                result.add(finalLasTarget.toString());
                // 8. 清理临时解压目录
                try { Files.deleteIfExists(lasPath); Files.deleteIfExists(tempUnzipDir); } catch (Exception ignore) {}
                return result;
            } else if (".las".equals(extension)) {
                // 直接保存las文件
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                finalLasPath = targetPath.toString();
                fileUrl = cloudUploadUtil.cloudStorage(file, relativePath + storedFileName);
                log.info("文件存储成功: {}", relativePath);
                result.add(relativePath);
                result.add(storedFileName);
                result.add(extension);
                result.add(fileUrl);
                result.add(finalLasPath);
                return result;
            } else {
                throw new IllegalArgumentException("不支持的文件类型: " + extension);
            }
        } catch (Exception e) {
            log.info("云端文件上传失败");
            throw new RuntimeException(e);
        }
    }
    private void validateFile(MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小超过限制: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(filename).toLowerCase();

        // 检查文件扩展名
        if (!extension.equals(".las") && !extension.equals(".zip")) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension + "，仅支持.las和.zip文件");
        }
    }
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }

        return filename.substring(lastDotIndex);
    }
    public void deleteFile(String relativePath) {
        try {
            Path filePath = Paths.get(storagePath, relativePath);
            Files.deleteIfExists(filePath);
            log.info("文件删除成功: {}", relativePath);
        } catch (IOException e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
        }
    }
}
