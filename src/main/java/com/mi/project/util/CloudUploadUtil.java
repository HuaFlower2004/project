package com.mi.project.util;


import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Component
@Slf4j
public class CloudUploadUtil {
    public String cloudStorage(MultipartFile file, String storageFileName) throws Exception {
        // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
        String endpoint = "https://oss-cn-beijing.aliyuncs.com";
        // 从环境变量中获取访问凭证。运行本代码示例之前，请确保已设置环境变量OSS_ACCESS_KEY_ID和OSS_ACCESS_KEY_SECRET。
        EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
        // 填写Bucket名称，例如examplebucket。
        String bucketName = "project-mi";
        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
        String objectName = storageFileName;
        // 填写Bucket所在地域。以华东1（杭州）为例，Region填写为cn-hangzhou。
        String region = "cn-beijing";

        // 创建OSSClient实例。
        // 当OSSClient实例不再使用时，调用shutdown方法以释放资源。
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        try {
            // 设置文件元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            // 可选：设置存储类型和访问权限
            // metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
            // metadata.setObjectAcl(CannedAccessControlList.Private);
            // 创建PutObjectRequest对象，使用MultipartFile的InputStream
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, file.getInputStream());
            putObjectRequest.setMetadata(metadata);
            // 上传文件。
            ossClient.putObject(putObjectRequest);
            String fileUrl = String.format("https://%s.oss-cn-beijing.aliyuncs.com/%s",
                    bucketName, storageFileName);

            log.info("文件上传成功: {}", storageFileName);

            log.info("文件URL: {}", fileUrl);

            System.out.println("文件上传成功: " + objectName);

            return fileUrl;

        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            ossClient.shutdown();
        }
        return "none url";
    }

    /**
     * 上传本地文件到OSS
     * @param localFilePath 本地文件路径
     * @param storageFileName OSS中存储的文件名
     * @return 文件的访问URL
     * @throws Exception
     */
    public String uploadLocalFile(String localFilePath, String storageFileName) throws Exception {
        File file = new File(localFilePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("文件不存在: " + localFilePath);
        }

        return uploadLocalFile(file, storageFileName);
    }

    /**
     * 上传本地文件到OSS
     * @param file 本地文件对象
     * @param storageFileName OSS中存储的文件名
     * @return 文件的访问URL
     * @throws Exception
     */
    public String uploadLocalFile(File file, String storageFileName) throws Exception {
        String endpoint = "https://oss-cn-beijing.aliyuncs.com";
        EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
        String bucketName = "project-mi";
        String objectName = storageFileName;
        String region = "cn-beijing";

        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        try {
            // 设置文件元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());

            // 根据文件扩展名设置Content-Type
            String contentType = getContentType(file.getName());
            metadata.setContentType(contentType);

            // 使用FileInputStream创建PutObjectRequest
            FileInputStream fileInputStream = new FileInputStream(file);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, fileInputStream);
            putObjectRequest.setMetadata(metadata);

            // 上传文件
            ossClient.putObject(putObjectRequest);

            // 构建文件URL
            String fileUrl = String.format("https://%s.oss-cn-beijing.aliyuncs.com/%s",
                    bucketName, storageFileName);

            log.info("本地文件上传成功: {}", file.getAbsolutePath());
            log.info("存储文件名: {}", storageFileName);
            log.info("文件URL: {}", fileUrl);

            // 关闭输入流
            fileInputStream.close();

            return fileUrl;

        } catch (OSSException oe) {
            log.error("OSS异常: {}", oe.getErrorMessage());
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
            throw oe;
        } catch (ClientException ce) {
            log.error("客户端异常: {}", ce.getMessage());
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
            throw ce;
        } catch (IOException e) {
            log.error("文件读取异常: {}", e.getMessage());
            throw e;
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 根据文件扩展名获取Content-Type
     */
    private String getContentType(String fileName) {
        try {
            Path path = Paths.get(fileName);
            String contentType = Files.probeContentType(path);
            return contentType != null ? contentType : "application/octet-stream";
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }
}
