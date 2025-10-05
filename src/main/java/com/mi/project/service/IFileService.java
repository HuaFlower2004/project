package com.mi.project.service;

import com.mi.project.dto.fileDTO.FileUploadDTO;
import com.mi.project.entity.File;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mi.project.entity.User;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author JackBlack
 * @since 2025-07-14
 */
public interface IFileService extends IService<File> {

    File uploadFile(FileUploadDTO upFileDTO, User user);

    void processFileAsync(Long fileId,String postParams,String absolutePath);

    List<File> getUserFiles(String username);

    File getFileById(Long fileID,String userName);

    void deleteFile(Long fileId,String userName);
}
