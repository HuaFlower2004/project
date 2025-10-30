package com.mi.project.rmi.server;

import com.mi.project.rmi.api.FileRmiService;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;

@Component
public class FileRmiServiceImpl implements FileRmiService {
    public FileRmiServiceImpl() throws RemoteException {}

    @Override
    public String uploadFile(byte[] fileData, String filename, String userName) throws RemoteException {
        File folder = new File("uploaded_files/" + userName);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File targetFile = new File(folder, filename);
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(fileData);
        } catch (IOException e) {
            throw new RemoteException("远程文件保存失败", e);
        }
        return targetFile.getAbsolutePath();
    }
}
