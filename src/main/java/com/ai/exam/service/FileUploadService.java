package com.ai.exam.service;

import com.ai.exam.config.MinioConfig;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class FileUploadService {

    @Autowired(required = false)
    private MinioClient minioClient;
    @Autowired(required = false)
    private MinioConfig minioConfig;

    @Value("${file.upload.path:./uploads/}")  // 本地文件存储路径
    private String localUploadPath;

    @Value("${file.upload.url-prefix:http://localhost:8080/files/}")  // 文件访问URL前缀
    private String fileUrlPrefix;

/**
 * 上传文件（自动选择MinIO或本地存储）
 */
public Map<String,Object> uploadFile(MultipartFile file,String folder){

    try {
        String url;
        if (minioClient != null && minioConfig != null){
            url = uploadToMinio(file,folder);
        }else {
            // 否则使用本地文件存储  // 降级使用本地存储
            log.info("MinIO未配置，使用本地文件存储");
            url = uploadToLocal(file, folder);
        }
        Map<String,Object> map = new HashMap<>();
        map.put("url",url);
        map.put("fileName",file.getOriginalFilename());
        map.put("fileSize",file.getSize());
        map.put("contentType",file.getContentType());
        return map;
    } catch (Exception e) {
        log.error("MinIO上传失败，降级使用本地存储: {}", e.getMessage());

        try {
            String url = uploadToLocal(file, folder);

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            result.put("contentType", file.getContentType());
            return result;

        } catch (Exception localException) {
            log.error("本地文件上传也失败: {}", localException.getMessage());
            throw new RuntimeException("文件上传失败: " + localException.getMessage());
        }
    }
}

private String uploadToMinio(MultipartFile file,String folder) throws IOException {
    log.info("开始上传文件到MinIO: 文件名={}, 大小={}, 类型={}",
            file.getOriginalFilename(), file.getSize(), file.getContentType());

    ensureBucketExists();

   String fileName = generateFileName(file.getOriginalFilename());
   String objectName = folder + "/" + fileName;

    InputStream inputStream = file.getInputStream();

    try {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioConfig.bucketName)
                        .object(objectName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
    } catch (Exception e) {
        log.info("上传失败");

    }
    String url = generateFileUrl(objectName);
    return url;
}
    /**
     * 上传到本地文件系统
     */
    private String uploadToLocal(MultipartFile file, String folder) throws Exception {
        // 创建上传目录  // 确保目录存在
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        Path uploadDir = Paths.get(localUploadPath, folder, datePath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // 生成唯一文件名  // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;

        // 保存文件  // 写入文件
        Path filePath = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        // 生成访问URL  // 构建访问地址
        String relativePath = folder + "/" + datePath + "/" + fileName;
        String url = fileUrlPrefix + relativePath;

        log.info("文件上传到本地成功: {}", url);
        return url;
    }

    public String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String uuid = UUID.randomUUID().toString().replace("-", "");

        return datePath + "/" + uuid+ extension;
    }

    private void ensureBucketExists(){
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioConfig.bucketName)
                            .build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioConfig.bucketName)
                                .build()
                );}
        } catch (Exception e) {
            throw new RuntimeException("存储桶操作失败: " + e.getMessage());
        }

    }

    public String generateFileUrl(String objectName){
        try {
            // 生成预签名URL  // 生成临时访问地址
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.bucketName)
                            .object(objectName)
                            .expiry(Math.toIntExact(minioConfig.urlExpiry), TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成文件URL失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成文件URL失败: " + e.getMessage());
        }
    }

    public void deleteFile(String objectName) {
        try {
            if (minioClient != null && minioConfig != null) {
                // 从MinIO删除文件  // 删除文件
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioConfig.bucketName)
                                .object(objectName)
                                .build()
                );
                log.info("文件从MinIO删除成功: {}", objectName);
            } else {
                // 从本地删除文件  // 删除本地文件
                Path filePath = Paths.get(localUploadPath, objectName);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("本地文件删除成功: {}", objectName);
                }
            }
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件删除失败: " + e.getMessage());
        }
    }

}
