package com.mi.project.controller;

import com.mi.project.common.Result;
import com.mi.project.dto.fileDTO.FileUploadDTO;
import com.mi.project.entity.File;
import com.mi.project.entity.User;
import com.mi.project.service.serviceImpl.FileServiceImpl;
import com.mi.project.util.CloudUploadUtil;
import com.mi.project.util.LASToJsonUtil;
import com.mi.project.util.WebSocketSenderUtil;
import io.swagger.v3.oas.annotations.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author JackBlack
 * @since 2025-07-14
 */
@Slf4j
@Controller
@RequestMapping("/api/file")
public class FileController {

    private final FileServiceImpl fileService;
    private final CloudUploadUtil cloudUploadUtil;
    public FileController(FileServiceImpl fileService, CloudUploadUtil cloudUploadUtil) {
        this.fileService = fileService;
        this.cloudUploadUtil = cloudUploadUtil;
    }
    @GetMapping("/test")
    @ResponseBody
    public String test() throws IOException {
        java.io.File file = new java.io.File("C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\json\\3_3_4_4_analysis.json");
        String fileContent = null;
        if (file.exists() && file.isFile()) {
            // 将文件内容读取为字符串
            fileContent = new String(Files.readAllBytes(Paths.get("C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\json\\3_3_4_4_analysis.json")));

            System.out.println("已发送文件内容: " + fileContent);
        } else {
            System.out.println("文件不存在: " + "C:\\Users\\31591\\Desktop\\project\\src\\main\\resources\\json\\3_3_4_4_analysis.json");
        }
        return fileContent;
    }

    @PostMapping("/upload")
    @Operation(summary = "上传雷达文件（支持.las文件和.zip压缩包）")
    @ResponseBody
    @CrossOrigin
    public Result<File> uploadFile(@RequestParam("file") MultipartFile multipartFile,
                                   @RequestParam(value = "description",required = false) String description,
                                   @RequestParam("userName") String userName,
                                   HttpServletRequest request) {



        log.info("文件上传接口被调用，文件名: {}", multipartFile.getOriginalFilename());

        FileUploadDTO uploadDTO = FileUploadDTO.builder()
                .file(multipartFile)
                .description(description)
                .userName(userName)
                .build();

        try {
            // 获取当前用户
            User currentUser = (User) request.getAttribute("currentUser");
            if (currentUser == null) {
                return Result.failure(401, "未登录");
            }

            // 上传文件
            File file = fileService.uploadFile(uploadDTO, currentUser);

            WebSocketSenderUtil.sendJsonToAll("{\n" +
                    "  \"type\": \"complete\",\n" +
                    "  \"status\": \"success\",\n" +
                    "  \"message\": \"操作已完成\",\n" +
                    "  \"data\": {\n" +
                    "    \"id\": 1,\n" +
                    "    \"timestamp\": \"2025-07-18T10:30:00Z\"\n" +
                    "  }\n" +
                    "}");
            return Result.success( "文件上传成功，正在处理中...",file);

        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return Result.failure(500,"文件上传失败: "+e.getMessage());
        }
    }

    @GetMapping("/list")
    @Operation(summary = "获取用户文件列表")
    @ResponseBody
    @CrossOrigin
    public Result<List<File>> getUserFiles(HttpServletRequest request) {
        try {
            // 获取当前用户
            User currentUser = (User) request.getAttribute("currentUser");
            if (currentUser == null) {
                return Result.failure(401, "未登录");
            }

            List<File> files = fileService.getUserFiles(currentUser.getUserName());

            return Result.success( "获取成功",files);

        } catch (Exception e) {
            log.error("获取文件列表失败: {}", e.getMessage(), e);
            return Result.failure(500,"获取文件列表失败: "+e.getMessage());
        }
    }

    @GetMapping("/{fileId:[0-9]+}")
    @Operation(summary = "获取文件详情")
    @ResponseBody
    @CrossOrigin
    public Result<File> getFileById(@PathVariable Long fileId,
                                    HttpServletRequest request) {
        try {
            // 获取当前用户
            User currentUser = (User) request.getAttribute("currentUser");
            if (currentUser == null) {
                return Result.failure(401, "未登录");
            }

            File file = fileService.getFileById(fileId, currentUser.getUserName());

            return Result.success( "获取成功",file);

        } catch (Exception e) {
            log.error("获取文件详情失败: {}", e.getMessage(), e);
            return Result.failure(500,"获取文件详情失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{fileId:[0-9]+}")
    @Operation(summary = "删除文件")
    @ResponseBody
    @CrossOrigin
    public Result<Void> deleteFile(@PathVariable Long fileId,
                                   HttpServletRequest request) {
        try {
            // 获取当前用户
            User currentUser = (User) request.getAttribute("currentUser");
            if (currentUser == null) {
                return Result.failure(401, "未登录");
            }

            fileService.deleteFile(fileId, currentUser.getUserName());

            return Result.success( "文件删除成功",null);

        } catch (Exception e) {
            log.error("删除文件失败: {}", e.getMessage(), e);
            return Result.failure(500,"删除文件失败: "+e.getMessage());
        }
    }
}
