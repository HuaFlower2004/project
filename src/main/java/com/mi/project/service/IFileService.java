package com.mi.project.service;

import com.mi.project.config.datasource.Master;
import com.mi.project.config.datasource.ReadOnly;
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

    @Master
    File uploadFile(FileUploadDTO upFileDTO, User user);

    void processFileAsync(Long fileId,String postParams,String absolutePath);

    @ReadOnly
    List<File> getUserFiles(String username);

    @ReadOnly
    File getFileById(Long fileId,String userName);

    @Master
    void deleteFile(Long fileId,String userName);
}
