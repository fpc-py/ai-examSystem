package com.ai.exam.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/files")
@CrossOrigin
public class FileController {
    @Value("${file.upload.path:./uploads/}")  // 本地文件存储路径
    private String localUploadPath;

    @GetMapping("/**")
    public void getFile(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            String requestURI = request.getRequestURI();
            String filePath = requestURI.substring("/files/".length());

            Path fullPath = Paths.get(localUploadPath + filePath);
            File file = fullPath.toFile();

            if (!file.exists() || !file.isFile()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String canonicalPath = file.getCanonicalPath();
            String canonicalUploadPath = new File(localUploadPath).getCanonicalPath();
            if (!canonicalPath.equals(canonicalUploadPath)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            String contentType = Files.probeContentType(fullPath);
            if (contentType == null) {
                contentType = "application/octet-stream";  // 默认类型
            }
            response.setContentType(contentType);
            response.setContentLength((int) file.length());


            response.setHeader("Cache-Control", "public, max-age=86400");  // 缓存1天

            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            log.debug("文件访问成功: {}", filePath);
        } catch (IOException e) {
            log.error("文件访问失败: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }

}
