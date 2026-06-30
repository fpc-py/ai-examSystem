package com.ai.exam.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {
    public String endpoint;
    public String username;
    public String password;
    public String bucketName;
    public Long urlExpiry;
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)      // 设置MinIO服务器地址
                .credentials(username, password)  // 设置认证凭据
                .build();
    }
}
