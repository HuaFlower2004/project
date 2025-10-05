package com.mi.project.service.serviceImpl;

import com.mi.project.common.FileStatus;
import com.mi.project.dto.fileDTO.FileUploadDTO;
import com.mi.project.entity.File;
import com.mi.project.entity.User;
import com.mi.project.mapper.FileMapper;
import com.mi.project.repository.FileRepository;
import com.mi.project.service.IFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mi.project.util.*;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author JackBlack
 * @since 2025-07-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements IFileService {

    @Resource
    CloudUploadUtil cloudUploadUtil;

    private final FileRepository fileRepository;

    private final FileStorageUtil fileStorageUtil;

    private final PythonScriptExecutorUtil pythonScriptExecutor;

    @Override
    @Transactional
    public File uploadFile(FileUploadDTO uploadDTO, User user) {
        try {

            MultipartFile multipartFile = uploadDTO.getFile();

            // 存储文件相对路径和文件名字都在里面。
            List<String> result = fileStorageUtil.storeFile(multipartFile, user.getUserName());

            // 创建文件记录

            LocalDateTime localDateTime = LocalDateTime.now();

            File file = File.builder()
                    .fileName(multipartFile.getOriginalFilename())
                    .storedFileName(result.get(1))
                    .processStartTime(localDateTime)
                    .relativeFilePath(result.get(0))
                    .userName(uploadDTO.getUserName())
                    .uploadTime(LocalDateTime.now())
                    .fileUrl(result.get(3))
                    .fileStatus(FileStatus.UPLOADED) // 已上传状态
                    .fileType(result.get(2))
                    .fileSize((int) multipartFile.getSize())
                    .user(user)
                    .build();

            // 保存文件记录
            File savedFile = fileRepository.save(file);

            // 异步处理文件
            processFileAsync(savedFile.getId(), uploadDTO.getPostParams(), result.get(4));

            String processResult =  cloudUploadUtil.uploadLocalFile("C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\json\\ransac_json\\powerline_curves-1_report.html", "powerline_curves-1_report.html");
            file.setProcessResult(processResult);
            log.info("文件上传成功: {} (ID: {})", multipartFile.getOriginalFilename(), savedFile.getId());
            file.setProcessEndTime(LocalDateTime.now());
            return savedFile;

        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    @Async
    @Transactional
    // String
    public void processFileAsync(Long fileId, String postParams, String absolutePath) {
        try {
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("文件不存在"));


            // 更新状态为处理中
            file.setFileStatus(FileStatus.PROCESSING);
            fileRepository.save(file);

            WebSocketSenderUtil.sendJsonToAll("{\"type\": \"start\"}");

            log.info("开始处理文件: {} (ID: {})", file.getFileName(), fileId);
            int index = absolutePath.indexOf("resources");
            String desiredPath = absolutePath.substring(0, index + "resources".length()) + "\\json\\" + file.getRelativeFilePath();
            String jsonPath = desiredPath.replaceAll("\\.las$", ".json");
            // 调用Python脚本处理

            String outputDir = "C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\tilefiles";
            String outputPrefix = "tile";

            List<String> tileFiles =  pythonScriptExecutor.runLastile(absolutePath, outputDir, outputPrefix);

            String processDir = "C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\process";
            List<String> processedLasFiles = pythonScriptExecutor.processTilesAndExtractPowerlines(tileFiles, processDir);

            String normJsonPath = "C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\tilefiles\\tile_threejs_params.json";

            // 5. ransac 和 point 脚本输出目录
            String ransacJsonDir = "C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\json\\ransac_json";
            String pointJsonDir = "C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\json\\point_json";

            // 6. 分别批量处理
            pythonScriptExecutor.runRansacFitToJsonOnProcessFolders(processDir, normJsonPath, ransacJsonDir);
            pythonScriptExecutor.runPointToJsonOnProcessFolders(processDir, normJsonPath, pointJsonDir);

            // 新增：将生成的json文件通过WebSocket发送给前端
            java.io.File ransacDir = new java.io.File(ransacJsonDir);
            if (ransacDir.exists() && ransacDir.isDirectory()) {
                java.io.File[] ransacJsonFiles = ransacDir.listFiles((dir, name) -> name.endsWith(".json"));
                if (ransacJsonFiles != null) {
                    for (java.io.File jsonFile : ransacJsonFiles) {
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            String fileContent = java.nio.file.Files
                                    .readString(java.nio.file.Paths.get(jsonFile.getAbsolutePath()));
                            com.fasterxml.jackson.databind.node.ObjectNode node = (com.fasterxml.jackson.databind.node.ObjectNode) mapper
                                    .readTree(fileContent);
                            node.put("type", "lines"); // 你可以根据需要改成其他类型
                            String mergedJson = node.toString();
                            WebSocketSenderUtil.sendJsonToAll(mergedJson);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            java.io.File pointDir = new java.io.File(pointJsonDir);
            if (pointDir.exists() && pointDir.isDirectory()) {
                java.io.File[] pointJsonFiles = pointDir.listFiles((dir, name) -> name.endsWith(".json"));
                if (pointJsonFiles != null) {
                    for (java.io.File jsonFile : pointJsonFiles) {
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            String fileContent = java.nio.file.Files
                                    .readString(java.nio.file.Paths.get(jsonFile.getAbsolutePath()));
                            com.fasterxml.jackson.databind.node.ObjectNode node = (com.fasterxml.jackson.databind.node.ObjectNode) mapper
                                    .readTree(fileContent);
                            node.put("type", "points"); // 你可以根据需要改成其他类型
                            String mergedJson = node.toString();
                            WebSocketSenderUtil.sendJsonToAll(mergedJson);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            // 更新处理结果
            file.setFileStatus(FileStatus.COMPLETED);
            fileRepository.save(file);

            log.info("文件处理完成: {} (ID: {})", file.getFileName(), fileId);

            // return 这个地方应该是接收python文件处理的文件路径集合

        } catch (Exception e) {
            log.error("文件处理失败: {}", e.getMessage(), e);

            // 更新为失败状态
            fileRepository.findById(fileId).ifPresent(file -> {
                file.setFileStatus(FileStatus.FAILED);
                file.setProcessResult("处理失败: " + e.getMessage());
                fileRepository.save(file);
            });
        }
    }

    @Override
    public List<File> getUserFiles(String userName) {
        return fileRepository.findByUserNameOrderByUploadTimeDesc(userName);
    }

    @Override
    public File getFileById(Long fileId, String userName) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        log.info("1 " + userName);
        log.info("2 " + file.getUser().getUserName());

        // 检查文件是否属于当前用户
        if (!file.getUserName().equals(userName)) {
            throw new RuntimeException("无权限访问此文件");
        }

        return file;
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId, String userName) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        // 检查文件是否属于当前用户
        if (!file.getUserName().equals(userName)) {
            throw new RuntimeException("无权限删除此文件");
        }
        log.info("1 " + userName + "2 " + file.getUserName());
        // 删除物理文件
        fileStorageUtil.deleteFile(file.getRelativeFilePath());

        // 删除数据库记录
        fileRepository.delete(file);

        log.info("文件删除成功: {} (ID: {})", file.getFileName(), fileId);
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
}
