package com.mi.project.rmi.client;

import com.mi.project.rmi.api.FileRmiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.rmi.Naming;

@RestController
@RequestMapping("/rmi/file")
public class FileRmiController {
    @Value("${rmi.file.service.url:rmi://192.168.181.152:1099/FileRmiService}")
    private String fileRmiUrl;

    @PostMapping("/upload")
    public String uploadByRmi(@RequestParam("file") MultipartFile file,
                              @RequestParam("userName") String userName) throws Exception {
        FileRmiService service = (FileRmiService) Naming.lookup(fileRmiUrl);
        byte[] data = file.getBytes();
        String fileName = file.getOriginalFilename();
        return service.uploadFile(data, fileName, userName);
    }
}
